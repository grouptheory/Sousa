package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.stk.XYZd;

/**
   AnnotationViewer
 */
public final class AnnotationViewer
	extends JPanel
	implements Renderable
{
	// DAVID:  This is temporary
    private static final TimeZone _TZ = TimeZone.getTimeZone("UTC");

	public static final String CONTENT_TYPE = "x-idol/x-annotation";

	/**
	   _gui
	*/
	/*@ non_null */ private final GUI _gui;

	/**
	   _table
	*/
	private final JTable _table;

	private final JScrollPane _scrollPane;
	/**
	   _model
	*/
	private final AnnotationViewerTableModel _model;

	/**
	   The client's current location.
	*/
	private	final Vector3d _position = new Vector3d(0.0, 0.0, 0.0);

	/**
	   _patches
	*/
	private final Map _patches = new HashMap(); // String -> Annotation

	/**
	   _patchEntries
	*/
	private final List _patchEntries = new LinkedList(); // Annotation

	// Constructors

	/**
	   AnnotationViewer(GUI)
	   @methodtype ctor
	   @param gui .
	*/
	public AnnotationViewer(/*@ non_null */ GUI gui)
	{
		super();
		this._gui = gui;

		_table = new JTable(_model = new AnnotationViewerTableModel());

		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		_scrollPane= new JScrollPane(_table);
		add(_scrollPane, BorderLayout.CENTER);
	}

	// mil.navy.nrl.cmf.idol.user.AnnotationViewer

	/**
	   add(Vector3d, String, Calendar, Calendar)

	   Add an annotation about a location.  The annotation applices to
	   a range of time.

	   @methodtype set
	   @param position the location to which the annotation pertains
	   @param text the annotation
	   @param timeLowerBound the beginning of the time range
	   @param timeUpperBound the end of the time range
	*/
	final void
		add(/*@ non_null */ Vector3d position,
			/*@ non_null */ String text,
			/*@ non_null */ Calendar timeLowerBound,
			/*@ non_null */ Calendar timeUpperBound)
	{
		Annotation entry = new Annotation(position, text, 
										  timeLowerBound, timeUpperBound);

		// TODO: Refactor this into a method that generates the key.
		String key = position + "," + timeLowerBound.getTime().toString() +
			"," + timeUpperBound.getTime().toString();

		_patches.put(key, entry);

		_patchEntries.add(entry);

		Collections.sort(_patchEntries, new GreatCircleComparator(_position));

		// This might not work as expected.  There shouldn't be
		// duplicate Annotations but if there are duplicates, then
		// there might be a problem identifying the new row because
		// indexOf() returns the first matching Annotation.  That
		// might not be the right instance for the inserted row of the
		// table.
		int newRow = _patchEntries.indexOf(entry);

		// There will never be new columns, so it's OK to fire a row
		// update.
		_model.fireTableRowsInserted(newRow, newRow);
	}

	/**
	   removePatch(String)
	   @methodtype set
	   @param patch .
	*/
	final void
		remove(/*@ non_null */ Vector3d position, 
			   /*@ non_null */ Calendar timeLowerBound,
			   /*@ non_null */ Calendar timeUpperBound)
	{
		// TODO: Refactor this into a method that generates the key.
		String key = position + "," + timeLowerBound.getTime().toString() +
			"," + timeUpperBound.getTime().toString();

		Annotation entry = (Annotation)_patches.remove(key);

		// This might not work as expected.  There shouldn't be
		// duplicate Annotations but if there are duplicates, then
		// there might be a problem identifying the removed row
		// because indexOf() returns the first matching Annotation.
		// That might not be the right instance for the removed row of
		// the table.
		int removedRow = _patchEntries.indexOf(entry);
		_patchEntries.remove(entry);

		// There will never be new columns, so it's OK to fire a row
		// update.
		_model.fireTableRowsUpdated(removedRow, removedRow);
	}

	/**
	   update(Vector3d)
	   @methodtype set
	   @param position .
	*/
	void
		update(/*@ non_null */ Vector3d position)
	{
		synchronized (_position) {
			_position.set(position);
		}

		Collections.sort(_patchEntries, new GreatCircleComparator(_position));
		_model.fireTableRowsUpdated(0, _patchEntries.size() - 1);
	}


	// Utility

	public void setBounds(int x, int y, int width, int height) {

		Dimension d = new Dimension(width, height);

		_scrollPane.setPreferredSize(d);
		super.setBounds(x, y, width, height);
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
		throws IOException {}

	public void unloadSceneGraphObject(String layername, String name)
		throws IOException {}

	public void loadObject(String layername, Object obj)
		throws IOException {
		try {
			QueryResultHandle h = (QueryResultHandle)obj;

			final String annotation =
				(String)h.getFieldValue("text");
			final Double latitude = 
				(Double)h.getFieldValue("lat");
			final Double longitude = 
				(Double)h.getFieldValue("lon");
			final Double elevation = 
				(Double)h.getFieldValue("elev");
			final Double minTime =
				(Double)h.getFieldValue("mint");
			final Double maxTime =
				(Double)h.getFieldValue("maxt");

			Calendar timeLowerBound = Calendar.getInstance(_TZ);
			timeLowerBound.setTimeInMillis(minTime.longValue());

			Calendar timeUpperBound = Calendar.getInstance(_TZ);
			timeUpperBound.setTimeInMillis(maxTime.longValue());
			add(new Vector3d(longitude.doubleValue(), 
							 latitude.doubleValue(), 
							 elevation.doubleValue()), 
				annotation, 
				timeLowerBound,
				timeUpperBound);
		} catch (ClassCastException ex) {
			throw new IOException(ex.toString());
		}
	}

	public void unloadObject(String layername, Object obj)
		throws IOException {

		try {
			QueryResultHandle h = (QueryResultHandle)obj;
			final Double latitude = 
				(Double)h.getFieldValue("lat");
			final Double longitude = 
				(Double)h.getFieldValue("lon");
			final Double elevation = 
				(Double)h.getFieldValue("elev");
			final Double minTime =
				(Double)h.getFieldValue("mint");
			final Double maxTime =
				(Double)h.getFieldValue("maxt");

			Calendar timeLowerBound = Calendar.getInstance(_TZ);
			timeLowerBound.setTimeInMillis(minTime.longValue());

			Calendar timeUpperBound = Calendar.getInstance(_TZ);
			timeUpperBound.setTimeInMillis(maxTime.longValue());

			remove(new Vector3d(longitude.doubleValue(), 
								latitude.doubleValue(), 
								elevation.doubleValue()), 
				   timeLowerBound, 
				   timeUpperBound);
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
	   Annotation
	*/
	static final class Annotation
	{

		private final String _text;
		private final Vector3d _position;
		private final Calendar _timeLowerBound;
		private final Calendar _timeUpperBound;

		// Constructors

		/**
		   Annotation(Vector3d, String, Calendar, Calendar)
		   @methodtype ctor
		   @param position the location of the annotation
		   @param text the text of the annotation
		   @param timeLowerBound  the beginning of the annotatino's time range
		   @param timeUpperBound the end of the annotation's time range

		*/
		Annotation(Vector3d position,
				   String text,
				   Calendar timeLowerBound,
				   Calendar timeUpperBound)
		{
			this._position = position;
			this._text = text;
			this._timeLowerBound = timeLowerBound;
			this._timeUpperBound = timeUpperBound;

		}
	}; // Annotation

	// ****************************************************************

	// Sorts by the great circle distance from a reference point.  Sorts
	// by elevation when the objects are the same great circle distance
	// from the reference point.
	static final class GreatCircleComparator implements Comparator {
		private final Vector3d _referencePoint;

		GreatCircleComparator(Vector3d reference) {
			_referencePoint = reference;
		}

		public int compare(Object o1, Object o2) {
			int answer = 0;
			double d1 = greatCircleDistance(_referencePoint, 
											((Annotation)o1)._position);
			double d2 = greatCircleDistance(_referencePoint, 
											((Annotation)o2)._position);

			if (d1 > d2) answer = 1;
			else if (d1 < d2) answer = -1;
			else { // o1 and o2 are the same distance from _reference.
				// Sort by elevation without regard to _reference.
				if (((Annotation)o1)._position.z > ((Annotation)o2)._position.z)
					answer = 1;
				else if (((Annotation)o1)._position.z < ((Annotation)o2)._position.z)
					answer = -1;

				// Same distance and elevation, so sort by lower time bound
				else if (((Annotation)o1)._timeLowerBound.after(((Annotation)o2)._timeLowerBound))
					answer = 1;
				else if (((Annotation)o1)._timeLowerBound.before(((Annotation)o2)._timeLowerBound))
					answer = -1;
				// If everything else is equal, sort by the text of the annotation
				else answer =  (((Annotation)o1)._text.compareTo(((Annotation)o2)._text));

				// XXX If they're still equal, sort by the hash code?
				// XXX What if they're the same Object?
			}

			return answer;
		}

		// Two Comparators are equal if they are the same Comparator or if
		// they contain equal reference points.  Two reference points are
		// equal if their x values are equal and their y values are equal.
		// The z values are ignored.
		public boolean equals(Object obj) {
			boolean answer = (this == obj);
			if (! answer) {
				answer = ((_referencePoint.x == ((GreatCircleComparator)obj)._referencePoint.x) &&
						  (_referencePoint.y == ((GreatCircleComparator)obj)._referencePoint.y));
			}

			return answer;
		}

		// Calculate the great-circle distance between p1 and p2 using the
		// formula from Wikipedia:
		// http://en.wikipedia.org/wiki/Great-circle_distance
		//
		double greatCircleDistance(Vector3d p1, Vector3d p2) {
			double phi1 = Math.toRadians(p1.x);
			double lambda1 = Math.toRadians(p1.y);
			double phi2 =  Math.toRadians(p2.x);
			double lambda2 = Math.toRadians(p2.y);
			double deltaLambda = lambda1 - lambda2;

			double numerator = 
				Math.sqrt(Math.cos(phi2) * Math.sin(deltaLambda) + 
						  Math.pow((Math.cos(phi1) * Math.sin(phi2) - 
									Math.sin(phi1) * Math.cos(deltaLambda)), 2.0));
			double denominator = 
				Math.sin(phi1) * Math.sin(phi2) + 
				Math.cos(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

			double deltaSigma = Math.atan2(numerator, denominator);
			return deltaSigma;
		}
	}

	// ****************************************************************

	/**
	   AnnotationViewerTableModel
	*/
	final class AnnotationViewerTableModel
		extends AbstractTableModel
	{
		// javax.swing.table.AbstractTableModel

		public final int
			getColumnCount()
		{
			return 4;
		}

		public final int
			getRowCount()
		{
			return _patchEntries.size();
		}

		public final String
			getColumnName(int col)
		{
			switch (col) {
			case 0:
				return "Annotation";
			case 1:
				return "Position";
			case 2:
				return "Lower Date";
			case 3:
				return "Upper Date";
			default:
				return null;
			}
		}

		public final Object
			getValueAt(int row, int col)
		{
			Object answer = null;

			if (row < getRowCount()) {
				Annotation entry = (Annotation)_patchEntries.get(row);

				switch (col) {
				case 0:
					answer= entry._text;
					break;
				case 1:
					answer = entry._position;
					break;
				case 2:
					answer = entry._timeLowerBound.getTime().toString();
					break;
				case 3:
					answer = entry._timeUpperBound.getTime().toString();
					break;
				default:
					break;
				}
			}

			return answer;
		}

		public final Class
			getColumnClass(int c)
		{
			return getValueAt(0, c).getClass();
		}

		public final boolean
			isCellEditable(int row, int col)
		{
			return false;
		}

	}; // AnnotationViewerTableModel
}; // AnnotationViewer
