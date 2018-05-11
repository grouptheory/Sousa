package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.stk.XYZd;

/**
   ContentPanel
 */
public final class ContentPanel
	extends JPanel
	implements Renderable
{
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
	private ContentPanelTableModel _model;

	/**
	   _patches
	 */
	private final Map _patches = new HashMap(); // String -> PatchEntry

	/**
	   _patchEntries
	 */
	private final List _patchEntries = new LinkedList(); // PatchEntry

// Constructors

/**
   ContentPanel(GUI)
   @methodtype ctor
   @param gui .
 */
public ContentPanel(/*@ non_null */ GUI gui)
{
	super();
	this._gui = gui;
	construct();
}

// mil.navy.nrl.cmf.idol.user.ContentPanel

/**
   addPatch(String)
   @methodtype set
   @param patch .
 */
final void
addPatch(/*@ non_null */ String patch)
{
	PatchEntry entry = new PatchEntry(patch);

	_patches.put(patch, entry);
	_patchEntries.add(entry);

	_model.fireTableDataChanged();
}

/**
   removePatch(String)
   @methodtype set
   @param patch .
 */
final void
removePatch(/*@ non_null */ String patch)
{
	PatchEntry entry = (PatchEntry)_patches.remove(patch);
	_patchEntries.remove(entry);

	_model.fireTableDataChanged();
}

// Utility

/**
   construct()
   @methodtype command
 */
private void
construct()
{
	_table = new JTable(_model = new ContentPanelTableModel());

	_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	TableColumn column = null;
	for (int i = 0; i < 3; i++) {
		column = _table.getColumnModel().getColumn(i);
		if (i == 0) {
			column.setPreferredWidth(250);
		} else {
			column.setPreferredWidth(50);
		}
	}
	add(new JScrollPane(_table), BorderLayout.CENTER);
}

// ****************************************************************		

// mil.navy.nrl.cmf.sousa.Renderable
	public void loadstaticmodel(String layername, String name, String filename, 
								double scale, XYZd position)
		throws IOException {}

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

	public void loaddynamictext(String layername, String name, String text, 
								Color color, double scale, XYZd position)
		throws IOException {}

	public void loadfluxedtext(String layername, String name, String text, 
							   Color color, double scale, double time, 
							   XYZd position, XYZd velocity)
		throws IOException {}

	public void updatedynamicobject(String layername, String name, 
									XYZd position)
		throws IOException {}

	public void updatefluxedobject(String layername, String name, double time, 
								   XYZd position, XYZd velocity)
		throws IOException {}

	public void loadpatch(String layername, String name, String filename, 
						  int displacement)
		throws IOException {
		addPatch(name);
	}

	public void unloadSceneGraphObject(String layername, String name)
		throws IOException {}

	public void loadObject(String layername, Object obj)
		throws IOException {
	}

	public void unloadObject(String layername, Object obj)
		throws IOException {
		removePatch(obj.toString());
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
   PatchEntry
 */
static final class PatchEntry
{
	/**
	   _patch
	 */
	/*@ non_null */ private final String _patch;

	/**
	   _showpatch
	 */
	private boolean _showpatch = true;

	/**
	   _showasd;
	 */
	private boolean _showasd = true;

// Constructors

/**
   PatchEntry(String)
   @methodtype ctor
   @param patch .
 */
PatchEntry(/*@ non_null */ String patch)
{
	this._patch = patch;
}
}; // PatchEntry

// ****************************************************************

/**
   ContentPanelTableModel
 */
final class ContentPanelTableModel
extends AbstractTableModel
{
// javax.swing.table.AbstractTableModel

/**
   @see AbstractTableModel#getColumnCount()
 */
public final int
getColumnCount()
{
	return 3;
}

/**
   @see AbstractTableModel#getRowCount()
 */
public final int
getRowCount()
{
	return _patchEntries.size();
}

/**
   @see AbstractTableModel#getColumnName(int)
 */
public final String
getColumnName(int col)
{
	switch (col) {
		case 0:
			return "Name";
		case 1:
			return "Show Patch";
		case 2:
			return "Show ASD";
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
	PatchEntry entry = (PatchEntry)_patchEntries.get(row);

	switch (col) {
		case 0:
			return entry._patch;
		case 1:
			return entry._showpatch ? Boolean.TRUE : Boolean.FALSE;
		case 2:
			return entry._showasd ? Boolean.TRUE : Boolean.FALSE;
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
	return (col > 0);
}

/**
   @see AbstractTableModel#setValueAt(Object, int, int)
 */
public final void
setValueAt(Object value, int row, int col)
{
	PatchEntry entry = (PatchEntry)_patchEntries.get(row);

	switch (col) {
		case 1:
			entry._showpatch = ((Boolean)value).booleanValue();
			break;
		case 2:
			entry._showasd = ((Boolean)value).booleanValue();
			break;
	}

	int mode = 0;
	if (entry._showpatch && entry._showasd) {
		mode = 3;
	} else if (entry._showpatch) {
		mode = 1;
	} else if (entry._showasd) {
		mode = 2;
	}

	//	_gui.switchPatch(entry._patch, String.valueOf(mode));

	fireTableCellUpdated(row, col);
}
}; // ContentPanelTableModel
}; // ContentPanel
