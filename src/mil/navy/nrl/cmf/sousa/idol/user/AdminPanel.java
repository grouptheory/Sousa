package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.util.*;
import mil.navy.nrl.cmf.stk.XYZd;
import org.apache.log4j.*;

/**
   Adminpanel -- should be called DirectoryPanel.  It displays the
   Entity directory as returned by a Directory Service.
 */
public final class AdminPanel
	extends JPanel
	implements Renderable
{
	private static final Logger _LOG = Logger.getLogger(AdminPanel.class);

	public static final String CONTENT_TYPE = "x-idol/x-directory";

	/**
	   _gui
	 */
	/*@ non_null */ private final GUI _gui;

	/**
	   _table
	 */
	private JTable _table;

	/**
	   _model
	 */
        private AdminPanelTableModel _model;

	/**
	   _peers is a Set of Peer.  It is sorted.
	   
	 */
	private final SortedSet _peers = new TreeSet(); // Peer

	/**
	   _admins
	 */
	//	private final Map _admins = new TreeMap(); // Peer -> AdminFrame

// Constructors

/**
   AdminPanel(GUI)
   @methodtype ctor
   @param gui .
 */
public AdminPanel(/*@ non_null */ GUI gui)
{
	super();
	this._gui = gui;
	construct();
}

// mil.navy.nrl.cmf.idol.user.AdminPanel

/**
   addPeer(Peer)
   @methodtype set
   @param peer .

   New Peer!  Create an AdminFrame for it if there isn't one already.
 */
final void
addPeer(/*@ non_null */ Peer peer)
{
	// Since _peers is a Set, we must remove peer before reinserting
	// it.  We reinsert because it is possible for a Peer to change
	// its type.  Only the Address is significant when comparing two
	// Peers.
	//
	boolean peerExists = _peers.remove(peer);
	_peers.add(peer);

	_LOG.debug(new Strings(new Object[] {"-addPeer(", peer, ")"}));

	if (! peerExists) {
		// DAVID: You could make a copy of the ServerContact.  If it
		// had any mutable data members, making a copy of it would
		// permit mutation without changing anything in AdminPanel and
		// Peer.
		_gui.addCommand(new FetchCommand(peer.getContactAddress(), -1));
	}

	_model.fireTableDataChanged();
}

/**
   removePeer(Peer)
   @methodtype set
   @param peer .
 */
final void
removePeer(/*@ non_null */ Peer peer)
{
	_peers.remove(peer);

// 	AdminFrame admin = (AdminFrame)_admins.remove(peer);
// 	if (null != admin) {
// 		admin.shutdown();
// 	}

	_model.fireTableDataChanged();
}

final void updatePeer(/*@ non_null */ Peer peer)
{
	// See comments in addPeer()
	boolean peerExists = _peers.remove(peer);
	_peers.add(peer);

// 	if (peerExists) { // See comments in addPeer()
// 		AdminFrame f = (AdminFrame) _admins.remove(peer);
// 		_admins.put(peer, f);
// 	}

	_model.fireTableDataChanged();
}


// /**
//    adminClosed(Peer)
//    @methodtype handler
//    @param peer .
//  */
// final void
// adminClosed(/*@ non_null */ Peer peer)
// {
// 	_admins.remove(peer);
// }

// Utility

/**
   construct()
   @methodtype command
 */
private void
construct()
{
	_table = new JTable(_model = new AdminPanelTableModel());

	_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

// 	_table.addMouseListener(new MouseAdapter()
// 	{
// 		// java.awt.event.MouseAdapter

// 		/**
// 		   @see MouseAdapter#mouseClicked(MouseEvent)
// 		 */
// 		public final void
// 		mouseClicked(MouseEvent e)
// 		{
// 			if (e.getClickCount() == 2) {
// 				int row = _table.rowAtPoint(e.getPoint());

// 				if (row >= 0) {
// 					Peer peer = (Peer)(_peers.toArray()[row]);
// 					AdminFrame admin = (AdminFrame)_admins.get(peer);
// 					if (null != admin) {
// 						admin.show();
// 					} else {
// 						_LOG.error(new Strings(new Object[] {
// 							"**CAN'T HAPPEN** No AdminFrame for ",
// 							peer}));
// 						// _gui.requestStatusUpdatePoll(peer);
// 					}
// 				}
// 			}
// 		}
// 	});

	add(new JScrollPane(_table), BorderLayout.CENTER);
}

// /**
//  * Build and display an AdminFrame for a Peer.
//  */
// private AdminFrame newAdminFrame(Peer peer) 
// {
// 	_LOG.debug(new Strings(new Object[] {
// 		"-newAdminFrame() for ", peer }));

// 	AdminFrame frame = new AdminFrame(AdminPanel.this, peer);
// 	_admins.put(peer, frame);
// 	frame.start();

// 	return frame;
// }

// /**
//    getAdminFrame(Peer)
//    @methodtype get
//    @param peer the Peer whose AdminFRame is desired

//    @return the AdminFrame that corresponds to the Peer.

//    Return the AdminFrame that corresponds to the Peer.  If there is no
//    AdminFrame for the Peer, create one.
//  */
// public AdminFrame getAdminFrame(Peer peer) 
// {
// 	AdminFrame answer = (AdminFrame) _admins.get(peer);

// 	if (null == answer) {
// 		answer = newAdminFrame(peer);
// 		_LOG.debug(new Strings(new Object[] {
// 			"-getAdminFrame() returning new AdminFrame for ", peer }));
// 	} else {
// 		_LOG.debug(new Strings(new Object[] {
// 			"-getAdminFrame() returning existing AdminFrame for ", peer }));
// 	}

// 	return answer;
// }


// ****************************************************************		

// mil.navy.nrl.cmf.sousa.Renderable
	public void loaddynamicmodel(String layername, String name, String filename, 
								 double scale, XYZd position)
		throws IOException {}

	public void loadfluxedmodel(String layername, String name, String filename, 
								double scale, double time, XYZd position, 
								XYZd velocity)
		throws IOException {}

	public void loadstatictext(String layername, String name, String text, 
							   Color color, double scale, XYZd position)
		throws IOException {}

	public void updatedynamicobject(String layername, String name, 
									XYZd position)
		throws IOException {}

	public void updatefluxedobject(String layername, String name, double time, 
								   XYZd position, XYZd velocity)
		throws IOException {}

	public void loadpatch(String layername, String name, String filename, 
						  int displacement)
		throws IOException {}

	public void unloadSceneGraphObject(String layername, String name)
		throws IOException {}

	public void loadObject(String layername, Object obj)
		throws IOException {
		try {
			Map.Entry e = (Map.Entry)obj;
			Peer p = new Peer((ServerContact)e.getKey(),
							  (String)e.getValue());
			addPeer(p);
		} catch (ClassCastException ex) {
			throw new IOException(ex.toString());
		}
	}

	public void unloadObject(String layername, Object obj)
		throws IOException {

		try {
			Map.Entry e = (Map.Entry)obj;
			Peer p = new Peer((ServerContact)e.getKey(),
							  (String)e.getValue());
			removePeer(p);
		} catch (ClassCastException ex) {
			throw new IOException(ex.toString());
		}
	}

	public void deleteLayer(String layername)
		throws IOException {}

	public Set getContentTypes() {
		Set answer = new HashSet();

		answer.add(CONTENT_TYPE);

		return answer;
	}

// ****************************************************************

/**
   AdminPanelTableModel
 */
final class AdminPanelTableModel
extends AbstractTableModel
{
// javax.swing.table.AbstractTableModel

/**
   @see AbstractTableModel#getColumnCount()
 */
public final int
getColumnCount()
{
	return 2;
}

/**
   @see AbstractTableModel#getRowCount()
 */
public final int
getRowCount()
{
	return _peers.size();
}

/**
   @see AbstractTableModel#getColumnName(int)
 */
public final String
getColumnName(int col)
{
	String answer = null;
	
	switch (col) {
	case 0:
		answer = "Host";
		break;
	case 1:
		answer = "Description";
		break;
	default:
		break;
	}

	return answer;
}

/**
   @see AbstractTableModel#getValueAt(int, int)
 */
public final Object
getValueAt(int row, int col)
{
	Object answer = null;
	
	Peer peer = (Peer)(_peers.toArray()[row]);

	switch (col) {
		case 0:
			answer = peer.getContactAddress();
			break;
			
	case 1:
		answer = peer.getDescription();
		break;
		
	default:
		break;
	}

	return answer;
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
}; // AdminPanelTableModel
}; // AdminPanel
