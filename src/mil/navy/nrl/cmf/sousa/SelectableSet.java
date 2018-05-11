// File: SelectableSet.java

package mil.navy.nrl.cmf.sousa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
   <CODE>SelectableSet</CODE> is a set of <CODE>Selectable</CODE>
   waiting to perform I/O operations together with the means to
   determine which of them is ready for I/O.
 */
public class SelectableSet
{
	/**
	   Semaphore for concurent I/O
	*/
    private Object  _blocked = new Object();
    
    /**
	   Tracks each {@link Selectable} and its {@link
	   Selectable.Handler} by {@link SignalType}.  The keys are
	   <CODE>SignalType</CODE>.  The values are <CODE>Maps</CODE>
	   where the keys are <CODE>Selectable</CODE> and the values are
	   <CODE>Selectable.Handler</CODE>.
	*/
    private HashMap _selectables = new HashMap(); 


    /**
	  Tracks each {@link Selectable} and its desire for I/O state by
	  {@link SignalType}.  The keys are <CODE>SignalType</CODE> and
	  the values are <CODE>HashMap</CODE> with <CODE>Selectable</CODE>
	  as keys and <CODE>Boolean</CODE> as values.  <CODE>true</CODE>
	  indicates that the <CODE>Selectable</CODE> desires to perform the I/O
	  indicated by the <CODE>SignalType</CODE>.
	*/
    private HashMap _marks = new HashMap(); 

	/**
	   Tracks each {@link Selectable}that is ready for I/O by {@link
	   SignalType}.  The keys are <CODE>SignalType</CODE> and the
	   values are <CODE>List</CODE> of <CODE>Selectable</CODE>.
	*/
    private HashMap _notifications = new HashMap(); 

	/**
	   The <CODE>Thread</CODE> under which {@link #select(long)} is running.
	 */
    private Thread  _thread = null;


	/**
	   Determines if {@link #_thread} is blocked/waiting.
	*/
    private boolean _inWait = false;

	/**
	   Class constructor
	*/
    public SelectableSet() { }

    /**
	   Wait up to <CODE>time</CODE> milliseconds for a {@link
	   Selectable} to become ready for I/O.

	   @param time the number of milliseconds to wait

	   @return the number of <CODE>Selectables</CODE> that are ready
	   for I/O; -1 indicates an interruption.
	 */
    int select(long time) 
    {
		int size = 0;
	
		synchronized(_selectables) {
			// clear notifications
			_clearNotifications();
			// attach the selectables, giving them a chance to bcast notifications
			_attachSelectables();
			// see if we already have notifications
			int prewait_notifications = _processNotifications(false);
			if (prewait_notifications == 0) {
				synchronized (_blocked) {
					_thread = Thread.currentThread();
		    
					try {
						_inWait = true;
						_blocked.wait(time);
						_inWait = false;
					}
					catch (InterruptedException ex) {
						_inWait = false;
						_thread = null;
						_detachSelectables();
						return -1;
					}
		    
					_thread = null;
				}
			}

			int postwait_notifications = _processNotifications(true);
			size = postwait_notifications;
			_detachSelectables();
		}
	
		return size;
    }
    
	/**
	   Adds <CODE>s</CODE> to this <CODE>SelectableSet</CODE>.
	   <CODE>s</CODE> is interested in I/O <CODE>st</CODE> using
	   <CODE>h</CODE>.

	   @param s the <CODE>Selectable</CODE> to add
	   @param st the type of I/O
	   @param h the handler of the I/O
	 */
	public void addSelectable(Selectable s, SignalType st, Selectable.Handler h)
    {
		synchronized(_selectables) {
			HashMap map = _getSelectablesMap(st);
			synchronized(map) {
				if (map.get(s) == null) {
					map.put(s, h);
				}
			}
			setMarked(s, st, true);
		}
    }

	/**
	   Removes <CODE>s</CODE> from this <CODE>SelectableSet</CODE> for
	   I/O type <CODE>st</CODE>.

	   @param s the <CODE>Selectable</CODE> to remove
	   @param st the I/O type
	 */
    void removeSelectable(Selectable s, SignalType st)
    {
		synchronized(_selectables) {
			HashMap map = _getSelectablesMap(st);
			map.remove(s);
			setMarked(s, st, false);
		}
    }

	/**
	   Returns the handler previously registered using {@link
	   #addSelectable(Selectable, SignalType, Selectable.Handler)} for
	   <CODE>s</CODE> and <CODE>st</CODE>

	   @param s the <CODE>Selectable</CODE>
	   @param st the I/O type
	   @return the handler for <CODE>s</CODE> and I/O type <CODE>st</CODE>.
	 */
    Selectable.Handler getHandler(Selectable s, SignalType st) {
		synchronized(_selectables) {
			HashMap map = _getSelectablesMap(st);
			Selectable.Handler h = (Selectable.Handler)map.get(s);
			return h;
		}
    }

	/**
	   Remove each {@link Selectable} for <CODE>st</CODE> from this
	   <CODE>SelectableSet</CODE>.

	   @param st the type of I/O
	 */
    void clearAllSelectables(SignalType st)
    {
		synchronized(_selectables) {
			HashMap map = _getSelectablesMap(st);
			synchronized(map) {
				while (map.size() > 0) {
					Iterator it = map.keySet().iterator();
					Selectable s = (Selectable)it.next();
					removeSelectable(s, st);
				}
				map.clear();
			}
		}
    }

    /**
	   Returns the number of {@link Selectable} interested in I/O type
	   <CODE>st</CODE>.

	   @param st the type of I/O
	   @return the number of <CODE>Selectables</CODE> interested in
	   <CODE>st</CODE>
	 */
    int size(SignalType st) 
    {
		int size = 0;
		synchronized(_marks) {
			HashMap map = _getMarkMap(st);
			synchronized(map) {
				for (Iterator it = map.keySet().iterator(); it.hasNext();) {
					Selectable s = (Selectable)it.next();
					if (getMarked(s, st)) {
						size++;
					}
				}
			}
		}
		return size;
    }

    /**
	   Returns an Iterator over the <CODE>Selectables</CODE>
	   interested in I/O type <CODE>st</CODE>.

	   @param st the type of I/O
	   @return an Iterator over <CODE>Selectable</CODE>
	 */
    Iterator iterator(SignalType st)
    {
		synchronized(_marks) {
			HashMap map = _getMarkMap(st);
			synchronized(map) {
				LinkedList markedlist = new LinkedList();
				for (Iterator it = map.keySet().iterator(); it.hasNext();) {
					Selectable s = (Selectable)it.next();
					if (getMarked(s, st)) {
						markedlist.addLast(s);
					}
				}
				return markedlist.iterator();
			}
		}
    }
    
    public String toString()
    {
		synchronized(_marks) {
			String answer = "";
			answer += this.toString(SignalType.READ)+"\n";
			answer += this.toString(SignalType.WRITE);
			return answer;
		}
    }

    /**
	   Returns a <CODE>String</CODE> of the <CODE>Selectables</CODE>
	   interested in <CODE>st</CODE>.

	   @param st the type of I/O
	   @return the String
	 */
    public String toString(SignalType st)
    {
		synchronized(_marks) {
			String answer = ""+st.toString()+":";
			for (Iterator it=iterator(st); it.hasNext();) {
				Selectable s = (Selectable)it.next();
				answer += s.toString();
			}
			return answer;
		}
    }

	/**
	   Mark <CODE>s</CODE> as interested or disinterested in
	   <CODE>st</CODE> depending on <CODE>value</CODE>

	   @param s the Selectable
	   @param st the type of I/O
	   @param value <CODE>true</CODE> for interested;
	   <CODE>false</CODE>for disinterested
	 */
    void setMarked(Selectable s, SignalType st, boolean value) 
    {
		synchronized(_marks) {
	    
			HashMap map = _getMarkMap(st);
			synchronized (map) {
				if (map.get(s) != null) {
					map.remove(s);
				}
				map.put(s, new Boolean(value));
			}
		}
    }
    
	/**
	   Returns <CODE>s</CODE>'s (dis)interest in <CODE>st</CODE>.

	   @param s the <CODE>Selectable</CODE> potentially interested in I/O
	   @param st the type of I/O in which <CODE>s</CODE> may be interested
	   @return <CODE>true</CODE> if <CODE>s</CODE> is interested;
	   <CODE>false</CODE> otherwise
	 */
    boolean getMarked(Selectable s, SignalType st)
    {
		synchronized(_marks) {
			HashMap map = _getMarkMap(st);
			synchronized (map) {
				Boolean val = (Boolean)map.get(s);
				if (val == null) {
					return false;
				}
				return val.booleanValue();
			}
		}
    }

    /**
	   Adds <CODE>s</CODE> to this <CODE>SelectableSet</CODE> for
	   <CODE>st</CODE>, optionally waking up any blocked Thread.

	   @param s the Selectable
	   @param st the type of I/O
	   @param notify <CODE>true</CODE> to notify the blocked Thread;
	   <CODE>false</CODE> otherwise
	 */
    void recvNotification(Selectable s, SignalType st, boolean notify)
    {
		synchronized(_notifications) {
	    
			LinkedList list = _getNotificationsList(st);
			synchronized (list) {
				if ( ! list.contains(s)) {
					list.addLast(s);
				}
			}
		}
	
		if (notify) {
			// awaken any pending select
			synchronized(_blocked) {
				_blocked.notify();
			}
		}
    }
    
	/**
	   Interrupt {@link #select(long)}.
	 */
    public void interrupt()
    {
		// awaken any pending select
		synchronized(_blocked) {
			if (_inWait) {
				_thread.interrupt();
			}
		}
    }

	/**
	   I HAVE NO IDEA.
	 */
    private void _attachSelectables() 
    {
		synchronized(_selectables) {
			for (Iterator it = _selectables.keySet().iterator(); it.hasNext();) {
				SignalType st = (SignalType)it.next();
				HashMap map = _getSelectablesMap(st);
				synchronized(map) {
					for (Iterator it2 = map.keySet().iterator(); it2.hasNext();) {
						Selectable s = (Selectable)it2.next();
						if (getMarked(s, st)) {
							s.addSelectableSet(this, st);
						}
					}
				}
			}
		}
    }
    
	/**
	   I HAVE NO IDEA.
	 */
    private void _detachSelectables() 
    {
		synchronized(_selectables) {
			for (Iterator it = _selectables.keySet().iterator(); it.hasNext();) {
				SignalType st = (SignalType)it.next();
				HashMap map = _getSelectablesMap(st);
				synchronized(map) {
					for (Iterator it2 = map.keySet().iterator(); it2.hasNext();) {
						Selectable s = (Selectable)it2.next();
						s.removeSelectableSet(this, st);
					}
				}
			}
		}
    }

    /**
	   Returns the <CODE>HashMap</CODE> that indicates the
	   <CODE>Selectables</CODE> willing to perform I/O
	   <CODE>st</CODE>.

	   @param st the type of I/O
	   @return a <CODE>HashMap</CODE> from <CODE>Selectable</CODE> to <CODE>Boolean</CODE>
	 */
    private HashMap _getMarkMap(SignalType st) {
		HashMap map = null;
		synchronized(_marks) {
			map = (HashMap)_marks.get(st);
			if (map == null) {
				map = new HashMap();
				_marks.put(st, map);
			}
		}
		return map;
    }

    /**
	   Discovers which Selectables are ready for I/O either before or after the wait() in {@link #select(long)}.

	   @param postwait <CODE>false</CODE> for pre-wait, <CODE>true</CODE> for post-wait
	   @return the number of ready <CODE>Selectables</CODE>
	 */
    private int _processNotifications(boolean postwait)
    {
		int size = 0;
		synchronized(_selectables) {
			for (Iterator it = _selectables.keySet().iterator(); it.hasNext();) {
				SignalType st = (SignalType)it.next();
				HashMap map = _getSelectablesMap(st);
				synchronized(_notifications) {
					LinkedList notifications = _getNotificationsList(st);
		    
					synchronized(notifications) {
						synchronized(map) {
							for (Iterator it2 = map.keySet().iterator(); it2.hasNext();) {
								Selectable s = (Selectable)it2.next();
				
								if (getMarked(s, st)) {
									if (notifications.contains(s)) {
										size++;
									}
								}
				
								if (postwait) {
									if (notifications.contains(s)) {
										setMarked(s, st, true);
									}
									else {
										setMarked(s, st, false);
									}
								}
							}
						}
					}
				}
			}
		}
	
		return size;
    }

    /**
	   Returns the HashMap from {@link Selectable} to {@link
	   Selectable.Handler} for the I/O type <CODE>st</CODE>.

	   @param st the type of I/O
	   @return the HashMap corresponding to <CODE>st</CODE>
	 */
    private HashMap _getSelectablesMap(SignalType st) {
		HashMap map = null;
		synchronized(_selectables) {
			map = (HashMap)_selectables.get(st);
			if (map == null) {
				map = new HashMap();
				_selectables.put(st, map);
			}
		}
		return map;
    }
    
    private LinkedList _getNotificationsList(SignalType st) {
		LinkedList list = null;
		synchronized(_notifications) {
			list = (LinkedList)_notifications.get(st);
			if (list == null) {
				list = new LinkedList();
				synchronized(list) {
					_notifications.put(st, list);
				}
			}
		}
		return list;
    }

    private void _clearNotifications() 
    {
		synchronized(_selectables) {
			for (Iterator it = _selectables.keySet().iterator(); it.hasNext();) {
				SignalType st = (SignalType)it.next();
				_clearNotificationsList(st);
			}
		}
    }
    
    private void _clearNotificationsList(SignalType st) {
		synchronized(_notifications) {
			LinkedList list = _getNotificationsList(st);
			synchronized(list) {
				list.clear();
			}
		}
    }
    
};
