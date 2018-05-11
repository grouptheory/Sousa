#ifndef DATALAYER_H
#define DATALAYER_H

// a datalayer loaded by idol

#include <string>
#include <iostream>
#include <sstream>
#include <osg/MatrixTransform>
#include <osg/Group>
#include <osg/Geode>
#include <osgText/Text>
#include "Utils.h"

using std::string;
using std::vector;
using std::map;
using std::cerr;
using std::cout;
using std::endl;

class DataLayer {
public:
    DataLayer(const string& name) :
	name_(name),
	layerRoot_(new osg::Group)
	{ }
    
    ~DataLayer()
	{
	    for (map<string, osg::MatrixTransform*>::iterator i = children_.begin(); i != children_.end(); i++)
		i->second->unref();
	}
    	
    void addChild(const vector<string>& tokens)
	// create and add a child
	// modify <layername> addchild <name>
	//        [ {static <lat> <lon> <elev>} |
	//	    {dynamic <lat> <lon> <elev>} |
	//	    {fluxed <time> <lat> <lon> <elev> <vlat> <vlon> <velev>} ]
	//        [ {text <text> <R> <G> <B> <A> <scale>} |
	//	    {model <filename> <scale>} ]
	{
	    const string& name = tokens[4];

	    if (children_.find(name) == children_.end()) {
		double lat, lon, elev, scale;
		string text;
		osg::Vec4 color(1.0f, 1.0f, 1.0f, 1.0f);
		
#if defined(DATALAYER_DEBUG)
		cout << "DataLayer::addChild: Found child " << name << endl;
#endif

		extractLatLonElev(tokens[5], &lat, &lon, &elev);

#if defined(DATALAYER_DEBUG)
		cout << "DataLayer::addChild: " << tokens[5] 
			 << " contains lat " << lat
			 << " lon " << lon
			 << " elev " << elev << endl;
#endif
		extractScaleTextColor(tokens[6], &scale, &text, &color);

#if defined(DATALAYER_DEBUG)
		cout << "DataLayer::addChild: " << tokens[6] 
			 << " contains scale " << scale
			 << " text " << text
			 << " color <" 
			 << color.x() << ", " 
			 << color.y() << ", "
			 << color.z() << ", "
			 << color.w() << ">" << endl;
#endif
		osg::MatrixTransform* node = createChild(lat, lon, elev, scale, name + "::" + text, color);
		node->ref();
		children_[name] = node;
		layerRoot_->addChild(node);
	    } else cerr << "DataLayer::addChild: No child " << name << endl;
	}
	
    void moveChild(const vector<string>& tokens)
	// move a child
	// update <layername> set <name> {fluxed <time> <lat> <lon> <elev> <vlat> <vlon> <velev>}
	// change <layername> set <name> {dynamic <lat> <lon> <elev>}
	{
	    map<string, osg::MatrixTransform*>::iterator i = children_.find(tokens[4]);
	    if (i != children_.end()) {
		double lat, lon, elev;
		extractLatLonElev(tokens[5], &lat, &lon, &elev);
		setTransform(i->second, lat, lon, elev);
	    }
	}
	
    void removeChild(const vector<string>& tokens)
	// remove a child
	// change <layername> removechild <name>
	{
	    map<string, osg::MatrixTransform*>::iterator i = children_.find(tokens[4]);
	    if (i != children_.end()) {
		layerRoot_->removeChild(i->second);
		i->second->unref();
		children_.erase(i);
	    }
	}
	
    const string& name() const 
	// name of this layer
	{ return name_; }
	
    osg::Group* layerRoot() const
	// root of this layer's scene subgraph 
	{ return const_cast<osg::Group*>(layerRoot_.get()); }  

protected:
    string name_;
	// layer name

    osg::ref_ptr<osg::Group> layerRoot_;
	// group node this layer's children are added under
	
    map<string, osg::MatrixTransform*> children_;
	// collection of current children
    
    osg::MatrixTransform* createChild(float lat, float lon, float elev, float scale, const string& name, const osg::Vec4& color)
	// create a subgraph representing the data
	{
	    // create a text geode
	    osgText::Text* text = new osgText::Text;
	    text->setFont();
	    text->setCharacterSize(6.0f*scale);
	    text->setColor(color);
	    text->setAlignment(osgText::Text::CENTER_CENTER);
	    text->setAutoRotateToScreen(true);
	    text->setText(name);
	    osg::Geode* geode = new osg::Geode;
	    geode->addDrawable(text);
	    
	    // put a transform on top of the geode
	    osg::MatrixTransform* result = new osg::MatrixTransform; 
	    result->addChild(geode);
	    setTransform(result, lat, lon, elev);
	    
	    return result;    
	}

    void setTransform(osg::MatrixTransform* transform, double lat, double lon, double elev) const
	// set transform to be a translate to lat,lon,elev
	{ transform->setMatrix(osg::Matrixd::translate(Utils::geo2xyz(lat, lon, elev))); }

    void extractLatLonElev(const string& s, double* lat, double* lon, double* elev) const
	// extract data from s in one of these formats:
	//    static <lat> <lon> <elev>
	//    dynamic <lat> <lon> <elev>
	//    fluxed <time> <lat> <lon> <elev> <vlat> <vlon> <velev>
	{
	    assert(lat && lon && elev);
	    
	    vector<string> subtokens;
	    Utils::lexQuotedTokens(s, &subtokens);
	    assert(subtokens.size() == 4 || subtokens.size() == 8);
	    int fluxed = subtokens[0] == "fluxed";
	    
	    *lat = Utils::asDouble(subtokens[1 + fluxed]);
	    *lon = Utils::asDouble(subtokens[2 + fluxed]) - 90.0;
	    *elev = 1.0 + Utils::asDouble(subtokens[3 + fluxed])/Utils::planetRadius;
	}

    void extractScaleTextColor(const string& s, double* scale, string* text, osg::Vec4* color) const
	// extract data from s in one of these formats:
	//    text <text> <R> <G> <B> <A> <scale>
	//    model <filename> <scale>
	{
	    assert(scale && text && color);

	    vector<string> subtokens;
	    Utils::lexQuotedTokens(s, &subtokens);
	    assert(subtokens.size() == 3 || subtokens.size() == 7);
	    
	    *scale = Utils::asDouble(subtokens[subtokens[0] == "text" ? 6 : 2]);
	    *text = subtokens[1];
	    if (subtokens[0] == "text")
		color->set(Utils::asDouble(subtokens[2])/255.0, Utils::asDouble(subtokens[3])/255.0, Utils::asDouble(subtokens[4])/255.0, Utils::asDouble(subtokens[5])/255.0);
	}
};

#endif
