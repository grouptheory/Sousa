// this program is derived from osgviewer

#include <Producer/RenderSurface>
#include <osgProducer/Viewer>
#include <osgDB/ReadFile>
#include <osg/Geometry>
#include <osg/Group>

#include "Utils.h"
#include "DataLayer.h"
#include "IdolConnection.h"

using namespace std;


/*
  HUD code from
  http://www.nps.navy.mil/cs/sullivan/osgtutorials/osgText.htm

  Overview:

  The Text class is derived from the Drawable class. This means that
  text instances can be added to Geode class instances and rendered as
  other geometry. A complete list of the methods associated with the
  Text class is listed *here*
  (http://openscenegraph.sourceforge.net/documentation/OpenSceneGraph/doc/doc++/osgText/HIER.html).
  The project 'osgExample Text' demonstrates many of these
  methods. This tutorial provides a sample of limited functionality of
  the Text class. Drawing a HUD involves two main concepts:

  Creating a subtree that has as its root the appropriate projection
  and model view matrices and...

  Assigning the geometry in the HUD subtree to the appropriate
  RenderBin so that the HUD geometry is drawn after the rest of the
  scene with the correct state settings.

  The subtree to render a HUD involves a a projection matrix and model
  view matrix. For the projection matrix we'll use an orthographic
  projection with a horizontal and vertical extent equivalent to the
  screen dimensions. Following this scheme, coordinates will equate to
  pixel coordinates. To keep things straightforward, the use the
  identity matrix for the model view matrix.

  To render the hud, we'll assign the geometry in it to a specific
  RenderBin. RenderBins allow users to specify the order in which
  geometry is drawn. This is helpful since the hud geometry needs to
  be drawn last.
 */
osgText::Text* initHud(osg::ref_ptr<osg::Group> root)
{
    // A geometry node for our hud:
    osg::Geode* hudGeode = new osg::Geode();
    // Text instance that wil show up in the hud:
    osgText::Text* result = new osgText::Text();
    // Projection node for defining view frustrum for hud:
    osg::Projection* hudProjectionMatrix = new osg::Projection;

    // Next, set up the scene to display the hud components. Add a
    // subtree that has as its root a projection and model view
    // matrix.

    // Initialize the projection matrix for viewing everything we
    // will add as descendants of this node. Use screen coordinates
    // to define the horizontal and vertical extent of the projection
    // matrix. Positions described under this node will equate to
    // pixel coordinates.
    hudProjectionMatrix->setMatrix(osg::Matrix::ortho2D(0,1024,0,768));

    // For the hud model view matrix use an identity matrix:
    osg::MatrixTransform* hudModelViewMatrix = new osg::MatrixTransform;
    hudModelViewMatrix->setMatrix(osg::Matrix::identity());

    // Make sure the model view matrix is not affected by any transforms
    // above it in the scene graph:
    hudModelViewMatrix->setReferenceFrame(osg::Transform::ABSOLUTE_RF);

    // Add the hud projection matrix as a child of the root node
    // and the hud model view matrix as a child of the projection matrix
    // Anything under this node will be viewed using this projection matrix
    // and positioned with this model view matrix.
    root->addChild(hudProjectionMatrix);
    hudProjectionMatrix->addChild(hudModelViewMatrix);

    // Now for setting up the geometry. Here we build a quad that is
    // aligned with screen coordinates and set up its color and
    // texture parameters.

    // Add the Geometry node to contain hud geometry as a child of the
    // hud model view matrix.
    hudModelViewMatrix->addChild(hudGeode);

    // Set up geometry for the hud and add it to the hud
    osg::Vec3Array* hudBackgroundVertices = new osg::Vec3Array;
    hudBackgroundVertices->push_back(osg::Vec3(   0, 0,-1));
    hudBackgroundVertices->push_back(osg::Vec3(1024, 0,-1));
    hudBackgroundVertices->push_back(osg::Vec3(1024,20,-1));
    hudBackgroundVertices->push_back(osg::Vec3(   0,20,-1));

    osg::DrawElementsUInt* hudBackgroundIndices = new osg::DrawElementsUInt(osg::PrimitiveSet::POLYGON, 0);
    hudBackgroundIndices->push_back(0);
    hudBackgroundIndices->push_back(1);
    hudBackgroundIndices->push_back(2);
    hudBackgroundIndices->push_back(3);

    osg::Vec4Array* hudcolors = new osg::Vec4Array;
    hudcolors->push_back(osg::Vec4(1.0f, 1.0f, 1.0f, 1.0f)*0.4f);

    osg::Vec2Array* texcoords = new osg::Vec2Array(4);
    (*texcoords)[0].set(0.0f,0.0f);
    (*texcoords)[1].set(1.0f,0.0f);
    (*texcoords)[2].set(1.0f,1.0f);
    (*texcoords)[3].set(0.0f,1.0f);

    osg::Geometry* hudBackgroundGeometry = new osg::Geometry();
    hudBackgroundGeometry->setTexCoordArray(0,texcoords);
    osg::Texture2D* hudTexture = new osg::Texture2D;
    hudTexture->setDataVariance(osg::Object::DYNAMIC);
    osg::Vec3Array* hudnormals = new osg::Vec3Array;
    hudnormals->push_back(osg::Vec3(0.0f,0.0f,1.0f));
    hudBackgroundGeometry->setNormalArray(hudnormals);
    hudBackgroundGeometry->setNormalBinding(osg::Geometry::BIND_OVERALL);
    hudBackgroundGeometry->addPrimitiveSet(hudBackgroundIndices);
    hudBackgroundGeometry->setVertexArray(hudBackgroundVertices);
    hudBackgroundGeometry->setColorArray(hudcolors);
    hudBackgroundGeometry->setColorBinding(osg::Geometry::BIND_OVERALL);

    hudGeode->addDrawable(hudBackgroundGeometry);

    // To render the hud correctly we need create an osg::StateSet
    // with depth testing disabled (always draw) and alpha blending
    // enabled (for a transparent hud.) We also need to make sure the
    // hud geometry is drawn last. Render order can be controlled by
    // specifying a numbered render bin to load the geometry into
    // duing the cull traversal. The last line shows how:

    // Create and set up a state set using the texture from above:
    osg::StateSet* hudStateSet = new osg::StateSet();
    hudGeode->setStateSet(hudStateSet);
    hudStateSet->setTextureAttributeAndModes(0,hudTexture,osg::StateAttribute::ON);

    // For this state set, turn blending on (so alpha texture looks right)
    hudStateSet->setMode(GL_BLEND,osg::StateAttribute::ON);

    // Disable depth testing so geometry is draw regardless of depth values
    // of geometry already draw.
    hudStateSet->setMode(GL_DEPTH_TEST,osg::StateAttribute::OFF);
    hudStateSet->setRenderingHint(osg::StateSet::TRANSPARENT_BIN);

    // Need to make sure this geometry is draw last. RenderBins are handled
    // in numerical order so set bin number to 11
    hudStateSet->setRenderBinDetails(11, "RenderBin");

    // Finally for working with text. Since the osg::Text class is
    // derived from osg::Drawable, osg::Text instances can be added as
    // a children of osg::Geode class instances.

    // Add the text (Text class is derived from drawable) to the geode:
    hudGeode->addDrawable(result);

    result->setCharacterSize(10);
    result->setFont();
    result->setText("-");
    result->setAxisAlignment(osgText::Text::SCREEN);
    result->setAlignment(osgText::Text::CENTER_CENTER);
    result->setPosition(osg::Vec3(512,10,-1.5));
    result->setColor(osg::Vec4(0, 0, 0, 1));

    return result;
}

void gregorianDateTime(double time, int* year, int* month, int* day, int* hours, int* minutes, int* seconds, const char** dayOfWeek)
{
    int st = (int)trunc(time);
    int julianDayNumber = st/(3600*24) + 2440588;  // 2440588 = unix epoch's julian day number 
    
    if (seconds)
	*seconds = st % 60;
    if (minutes)
	*minutes = (st/60) % 60;
    if (hours)
	*hours = (st/3600) % 24;

    /* 
     * convert julian day number to gregorian date.
     * Fliegel-Van Flandern algorithm
     * http://mathforum.org/library/drmath/view/51907.html
     * http://home.capecod.net/~pbaum/date/back.htm#JDN
     */
    long long p = julianDayNumber + 68569;
    long long q = 4*p/146097;
    long long r = p - (146097*q + 3)/4;
    long long s = 4000*(r + 1)/1461001;
    long long t = r - 1461*s/4 + 31;
    long long u = 80*t/2447;
    long long v = u/11;
    if (year)
	*year = 100*(q - 49) + s + v;
    if (month)
	*month = u + 2 - 12*v;
    if (day)
	*day = t - 2447*u/80;

    static const char* names[] = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    if (dayOfWeek)
	*dayOfWeek = names[julianDayNumber % 7];
}

void computeCenterOfInterest(const osgProducer::Viewer& viewer, float* lat, float* lon, float* elev, double* time, double *lastTime)
{
    osg::Vec3 eye, center, up;
    viewer.getViewMatrix().getLookAt(eye, center, up);
    
    Utils::xyz2geo(eye, lat, lon, elev);
    *elev = (*elev - 1.0)*Utils::planetRadius;
    
    if (1) {
		// clock advances in realtime from current
		// notion of wall time
		double now = Utils::wallTime();
		
		*time += (now - *lastTime);
		*lastTime = now;

	} else {
		*time = Utils::wallTime();   // clock advances in realtime
		*lastTime = *time;
	}
}

void updateHudText(osgText::Text* hudText, float lat, float lon, float elev, double time)
{
    int year, month, day, hours, minutes, seconds;
    const char* dayOfWeek;
    gregorianDateTime(time, &year, &month, &day, &hours, &minutes, &seconds, &dayOfWeek);

    char buffer[1024];
    buffer[sizeof(buffer)-1] = '\0';
    snprintf(buffer, sizeof(buffer)-1, "lat: %f    lon: %f    elev: %8.2f m     time: %02d/%02d/%d %02d:%02d:%02d UTC", lat, lon, elev, month, day, year, hours, minutes, seconds); 
    
    hudText->setText(buffer);
}

int main(int argc, char** argv)
{
    float lat(0.0), lon(0.0), elev(0.0);
    double time(Utils::wallTime()); // initial time = current time
	double lastTime(time);

    // create basic scene, set up viewer
    osg::ref_ptr<osg::Group> scene = new osg::Group;
    osgText::Text* hudText = initHud(scene);
    osgProducer::Viewer viewer;
    viewer.setUpViewer(osgProducer::Viewer::STANDARD_SETTINGS);
    viewer.setSceneData(scene.get());
    viewer.realize();
    
	// connect to idol, give it the scene graph
    IdolConnection idol(&viewer, scene.get());

    while (!viewer.done()) {
		// What does IDOL have to say?  He might change my position or
		// my time.  
		bool idolChangedSharedState = 
			idol.processIdolCommands(&lat, &lon, &elev, &time);

        // Communicate with idol federation
        if (!idolChangedSharedState) {
			computeCenterOfInterest(viewer, &lat, &lon, &elev, 
									&time, &lastTime);
			idol.updateCenterOfInterest(lat, lon, elev, time);
		} else {
			// Always keep track of the last time that the
			// authoritative time was updated so that we know how much
			// time has passed since the last update.
			lastTime = Utils::wallTime();
		}

		updateHudText(hudText, lat, lon, elev, time);
	
        // Handle user input, draw a frame
        viewer.sync();
        viewer.update();
        viewer.frame();
    }
    
    // shutdown all threads and exit
    viewer.sync();
    viewer.cleanup_frame();
    viewer.sync();
    return 0;
}

    
