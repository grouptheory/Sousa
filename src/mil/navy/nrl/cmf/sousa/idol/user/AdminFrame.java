package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.table.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.idol.util.ImageParams;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.*;

/**
   AdminFrame
 */
public final class AdminFrame
{
	private static final Logger _LOG = Logger.getLogger(AdminFrame.class);

	/**
	   _adminp
	 */
	/*@ non_null */ private final AdminPanel _adminp;

	/**
	   _peer
	 */
	/*@ non_null */ private final Peer _peer;

	/**
	   _frame
	 */
	/*@ non_null */ private final JFrame _frame;

	/**
	   _inprocess
	 */
	private JLabel _inprocess;

	/**
	   _table
	 */
	private JTable _table;

	/**
	   _model
	 */
        private AdminFrameTableModel _model;

	/**
	   _command
	 */
	private JTextField _command;

	/**
	   _closed
	 */
	private boolean _closed = false;

	/**
	   _rasters
	 */
	private final java.util.Set _rasters = new TreeSet(); // RasterEntry

// Content for _inprocess
    private final static String WAIT_TEXT = "Processing : wait";
    private final static String COMPLETED_TEXT = "Processing : completed";


static class RasterEntry
implements Comparable
{
	private final ImageParams _params;
	private final String _name;

RasterEntry(ImageParams params)
{
	this._params = params;
	this._name = null;
}

RasterEntry(String name)
{
	this._params = null;
	this._name = name;
}

String
name()
{
	return (null == _name) ? _params.name() : _name;
}

public boolean
equals(Object o)
{
	if ((null == o) || (!(o instanceof RasterEntry))) {
		return false;
	}

	return name().equals(((RasterEntry)o).name());
}

public int
hashCode()
{
	return name().hashCode();
}

public int
compareTo(Object o)
{
	return name().compareTo(((RasterEntry)o).name());
}
}; // RasterEntry

// Constructors

/**
   AdminFrame(AdminPanel, Peer)
   @methodtype ctor
   @param adminp .
   @param peer .
 */
AdminFrame(/*@ non_null */ AdminPanel adminp, /*@ non_null */ Peer peer)
{
	this._adminp = adminp;
	this._peer = peer;
	this._frame = new JFrame(peer.getContactAddress().toString());

	construct();

	_frame.addWindowListener(new WindowAdapter()
	{
		public final void
		windowClosing(WindowEvent event)
		{
			shutdown();
			_closed = true;
		}
	});
}

// mil.navy.nrl.cmf.idol.user.AdminFrame

/**
   start()
   @methodtype command
 */
final void
start()
{
	_frame.pack();
	//	_frame.show();
}

/**
   show()
   @methodtype command
*/
final void
show() 
{
	_closed = false;
	_frame.show();
}

/**
   shutdown()
   @methodtype command
 */
final void
shutdown()
{
	// hide() removes the JFrame from the display but does not destroy
	// it.
	_frame.hide();
}

/**
   updateStatus(Iterator)
   @methodtype set
   @param i .
 */
final void
updateStatus(/*@ non_null */ Iterator i)
{
	_inprocess.setText(WAIT_TEXT);

	_rasters.clear();
	// i is an Iterator of QueryResultHandles
	while (i.hasNext()) {
		QueryResultHandle h = (QueryResultHandle)i.next();

		_LOG.debug(new Strings(new Object[] {
			"-updateStatus(): adding ", h
		}));
							   
		// Assume that there will always be mapname, north, east, minelev, and maxelev
		String name = (String)h.getFieldValue("mapname");
		Double north = (Double)h.getFieldValue("north");
		Double east = (Double)h.getFieldValue("east");
		Double minelev = (Double)h.getFieldValue("minelev");
		Double maxelev = (Double)h.getFieldValue("maxelev");

		Double south = (Double)h.getFieldValue("south");
		if (null == south) south = north;
		
		Double west = (Double)h.getFieldValue("west");
		if (null == west) west = east;
		
		// DAVID: The parts of ImageParams that depend on size aren't
		// used.  Any value for size is OK as long as neither the
		// width nor the height is zero.  Query Dardo to see if size
		// can go away.
		Dimension size = new Dimension(1, 1);
		
		ImageParams ip = new ImageParams(name, 
										 north.doubleValue(), 
										 south.doubleValue(), 
										 west.doubleValue(), 
										 east.doubleValue(), 
										 size, 
										 minelev.doubleValue(), 
										 maxelev.doubleValue());
		
		_rasters.add(new RasterEntry(ip));
	}

 	_model.fireTableDataChanged();

	_inprocess.setText(COMPLETED_TEXT);
}

/**
   peer()
   @methodtype get
 */
final Peer
peer()
{
	return _peer;
}

/**
   isOpen()
   @methodtype get
 */
final boolean
isOpen()
{
	return !_closed;
}

// Utility

private void
construct()
{
	JPanel cp = (JPanel)_frame.getContentPane();

	_table = new JTable(_model = new AdminFrameTableModel(_peer.getDescription()));
	_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	_table.addMouseListener(new MouseAdapter()
	{
		// java.awt.event.MouseAdapter

		/**
		   @see MouseAdapter#mouseClicked(MouseEvent)
		 */
		public final void
		mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2) {
				int row = _table.rowAtPoint(e.getPoint());

				if (row >= 0) {
					RasterEntry entry = getRasterEntry(row);

					if (null != entry._params) {
						double lat = entry._params.south() + ((entry._params.north() - entry._params.south()) / 2.0);
						double lon = entry._params.west() + ((entry._params.east() - entry._params.west()) / 2.0);

						//						_adminp.gotolatlon(lat, lon);
					}
				}
			}
		}
	});

	cp.add(_inprocess = new JLabel(WAIT_TEXT), BorderLayout.NORTH);

	cp.add(new JScrollPane(_table), BorderLayout.CENTER);

	cp.add(_command = new JTextField(), BorderLayout.SOUTH);

	_command.addActionListener(new ActionListener()
	{
		public final void
		actionPerformed(ActionEvent event)
		{
			//			_adminp.sendCommand(_peer, event.getActionCommand());

			_command.setText("");
		}
	});
}

private RasterEntry
getRasterEntry(int index)
{
	RasterEntry result = null;
	Iterator i = _rasters.iterator();
	while (index >= 0) {
		result = (RasterEntry)i.next();
		index--;
	}
	return result;
}

// ****************************************************************

/**
   AdminFrameTableModel
 */
final class AdminFrameTableModel
extends AbstractTableModel
{
private final String _column0Text;

public AdminFrameTableModel(String column0Text) 
{
	super();
	
	_column0Text = column0Text;
}

// javax.swing.table.AbstractTableModel

/**
   @see AbstractTableModel#getColumnCount()
 */
public final int
getColumnCount()
{
	return 1;
}

/**
   @see AbstractTableModel#getRowCount()
 */
public final int
getRowCount()
{
	return _rasters.size();
}

/**
   @see AbstractTableModel#getColumnName(int)
 */
public final String
getColumnName(int col)
{
	switch (col) {
		case 0:
			return _column0Text;
		default:
			return null;
	}
}

/**
   @see AbstractTableModel#getValueAt(int, int)
 */
public final Object
getValueAt(int row, int col)
{
	switch (col) {
		case 0:
			return getRasterEntry(row).name();
		default:
			return null;
	}
}

/**
   @see AbstractTableModel#getColumnClass(int)
 */
public final Class
getColumnClass(int c)
{
	return getValueAt(0, c).getClass();
}

/**
   @see AbstractTableModel#isCellEditable(int, int)
 */
public final boolean
isCellEditable(int row, int col)
{
	return false;
}
}; // AdminFrameTableModel
}; // AdminFrame
