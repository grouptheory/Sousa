package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.reflect.Proxy;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import mil.navy.nrl.cmf.annotation.ServerSI;
import mil.navy.nrl.cmf.sousa.Receptor;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

/**
   AnnotationCreator
*/
public final class AnnotationCreator
    extends JPanel
{
    /**
       _gui
    */
    /*@ non_null */ private final GUI _gui;

    /**
       Text input area
     */
    private final JTextArea _area;
	private final JList _servers;
	private final DefaultListModel _listModel = new DefaultListModel();

    /**
       The client's current location.
    */
    private final Vector3d _position = new Vector3d(0.0, 0.0, 0.0);
    private final Vector3d _width = new Vector3d(0.0, 0.0, 0.0);
    private final Vector3d _hpr = new Vector3d(0.0, 0.0, 0.0);

    // Constructors

    /**
       AnnotationCreator(GUI)
       @methodtype ctor
       @param gui .
    */
    public AnnotationCreator(/*@ non_null */ GUI gui)
    {
		super();
		this._gui = gui;

		// DAVID: Note magic numbers: 1 row, 50 columns.  This could
		// be a Property.  It's also a limit hard set in the
		// annotation server schema.
		_area = new JTextArea(1, 50);
		_servers = new JList(_listModel);

		JButton clear = new JButton("Clear");
		JButton submit = new JButton("Submit");

		// TODO: Consider adding a radius control <degrees of lat,
		// degrees of lon, meters of elev> to mark the sphere to which
		// the annotation pertains.
		clear.addActionListener(new ActionListener()
			{
				public final void
					actionPerformed(ActionEvent event)
				{
					_servers.clearSelection();
					_area.setText(null);
				}
			});

		submit.addActionListener(new ActionListener()
			{
				public final void
					actionPerformed(ActionEvent event)
				{
					try {
						Object selected[] = _servers.getSelectedValues();

						// TODO: Choose the right value for lower and
						// upper time bounds.
						Calendar now = Calendar.getInstance();

						// TODO: Choose the right value for the width.

						// Send the annotation to as many servers as
						// are selected.
						for (int i=0; i < selected.length; i++ ) {

							// DAVID: Note bogus time bounds
							_gui.addCommand(new AnnotationRemoteCommand((ServerContact)selected[i], 
																		_area.getText(), 
																		_position, _width,
																		now, now));
						}
					} catch (NullPointerException ex) {
						// No text in _area.

						// DAVID: What's the right thing
						// to do?  Send an empty string to the server or
						// ignore the event?
					}
				}
			});

		add(new JScrollPane(_area), BorderLayout.CENTER);
		add(new JScrollPane(_servers), BorderLayout.NORTH);
		add(clear,  BorderLayout.SOUTH);
		add(submit,  BorderLayout.SOUTH);

    }

	
    // mil.navy.nrl.cmf.idol.user.AnnotationCreator

    /**
       setPosition(Vector3d, Vector3d, Vector3d)
       @methodtype set
       @param position .
    */
    void setPosition(/*@ non_null */ Vector3d position, 
					 /*@ non_null */ Vector3d width,
					 /*@ non_null */ Vector3d hpr)
    {
		synchronized (_position) {
			_position.set(position);
			if (null != width)
				_width.set(width);
			if (null != hpr)
				_hpr.set(hpr);
		}
    }

	void addServer(Object server) {
		_listModel.addElement(server);
	}

	void removeServer(Object server) {
		_listModel.removeElement(server);
	}

    // ****************************************************************

    public static final class AnnotationRemoteCommand implements RemoteCommand {
		private final ServerContact _server;
		private final String _text;
		private final Vector3d _position;
		private final Vector3d _width;
		private final Calendar _timeLower;
		private final Calendar _timeUpper;

		public AnnotationRemoteCommand(ServerContact s, String text,
									   Vector3d position, Vector3d width,
									   Calendar timeLowerBound,
									   Calendar timeUpperBound) {

			// DAVID: Do I have to make a copy of s?
			_server = s;

			_text = new String(text);
			_position = new Vector3d(position);
			_width = new Vector3d(width);
			_timeLower = (Calendar)timeLowerBound.clone();
			_timeUpper = (Calendar)timeUpperBound.clone();
		}

		public ServerContact getServerContact() {
			return _server;
		}

		public void execute(RemoteCommandObject c) {
			Receptor r = c.getReceptor(_server);
			if (null != r) {
				
				ServerSI annotationServer = (ServerSI)
					Proxy.newProxyInstance(mil.navy.nrl.cmf.annotation.ServerSI.class.getClassLoader(),
										   new Class[] {mil.navy.nrl.cmf.annotation.ServerSI.class},
										   r);
 				if (null != annotationServer) {
 					annotationServer.annotate(_text, _position, 
 											  _timeLower, _timeUpper);
				}
			}
		}
	}
}; // AnnotationCreator
