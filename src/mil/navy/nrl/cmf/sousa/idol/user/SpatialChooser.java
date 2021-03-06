package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.*;

/**
   SpatialChooser

   Permits selecting latitude and longitude by clicking on a map.
 */
public final class SpatialChooser
	extends JPanel
	implements SetPosition
{
private static final Logger _LOG = Logger.getLogger(SpatialChooser.class);

	/**
	   _gui
	 */
	/*@ non_null */ private final GUI _gui;

	/**
	   _position
	 */
	private final Vector3d _position = new Vector3d();

	/**
	   _width
	*/
	private final Vector3d _width = new Vector3d();

	/**
	   _hpr  Heading, Pitch, Roll
	*/
	private final Vector3d _hpr = new Vector3d();

	/**
	   _latlon
	 */
	private JPanel _latlon;

	/**
	   _updating
	 */
	private boolean _updating = false;

// Constructors

/**
   SpatialChooser(GUI)
   @methodtype ctor
   @param gui .
 */
public SpatialChooser(/*@ non_null */ GUI gui)
{
	super();
	this._gui = gui;
	construct();
}

// mil.navy.nrl.cmf.idol.user.SpatialChooser

// mil.navy.nrl.cmf.idol.user.SetPosition
/**
   update(Vector3d)
   @methodtype set
   @param position .
 */
public void setPosition(/*@ non_null */ Vector3d position, 
						Vector3d width,
						Vector3d hpr)
{
	synchronized (_position) {
		_position.set(position);
		if (null != width)
			_width.set(width);
		if (null != hpr)
			_hpr.set(hpr);
	}

	_updating = true;
	try {
		_latlon.repaint();
	} finally {
		_updating = false;
	}
}

// Utility

/**
   construct()
   @methodtype command
 */
private void
construct()
{
	final BufferedImage image = new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB);
	_latlon = new JPanel()
	{
		protected final void
		paintComponent(Graphics g)
		{
			super.paintComponent(g);

			g.drawImage(image, 0, 0, null);

			g.setColor(Color.white);

			int y;
			int x;
			synchronized (_position) {
				y = 256 - (int)((256.0 / 180.0) * (_position.y + 90.0));
				x = (int)((512.0 / 360.0) * (_position.x + 180.0));
			}

			g.drawLine(x - 5, y - 5, x + 5, y + 5);
			g.drawLine(x + 5, y - 5, x - 5, y + 5);
		}
	};
	image.createGraphics().drawImage(loadImage("images/earth.jpg", this), null, null);

	_latlon.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
	_latlon.addMouseListener(new MouseAdapter()
	{
		public final void
		mouseClicked(MouseEvent event)
		{
			double lat = -(((180.0 / 256.0) * event.getY()) - 90.0);
			double lon = ((360.0 / 512.0) * event.getX()) - 180.0;

			// Create a LocalCommand updatePosition
			synchronized (_position) {
				_position.x = lon;
				_position.y = lat;

				SpatialChooser._LOG.debug(new Strings(new Object[] 
					{"Map setting _position to ", _position}));

				// DAVID: All I want is to call _latlon.repaint().
				setPosition(_position, _width, _hpr);

				if (! _updating) {
				    _gui.addCommand(new SetPositionCommand(_position, _width, _hpr));
				}
			}
		}
	});

	add(_latlon, BorderLayout.CENTER);
}

/**
   loadImage(String, Component)
   @methodtype factory
   @param filename .
   @param comp .
   @return Image
 */
private static Image
loadImage(/*@ non_null */ String filename, /*@ non_null */ Component comp)
{
	java.net.URL imgURL = SpatialChooser.class.getResource(filename);
	if (null != imgURL) {
		Image image = Toolkit.getDefaultToolkit().getImage(imgURL);
		MediaTracker mt = new MediaTracker(comp);
		mt.addImage(image, 0);
		try { 
			mt.waitForID(0);
		} catch (InterruptedException e) {
			_LOG.error(new Strings(new Object[] {
				"Caught exception ", e, ":",
				StackTrace.formatStackTrace(e)}));
			return null;
		}

		if (mt.isErrorID(0)) {
			return null;
		}
		return image; 
	} else {
		_LOG.error(new Strings(new Object[] 
			{"Couldn't find file: ", filename}));
		return null;
	}
}
}; // SpatialChooser
