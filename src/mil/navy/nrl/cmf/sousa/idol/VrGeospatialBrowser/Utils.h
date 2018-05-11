#ifndef UTILS_H
#define UTILS_H

// misc utility functions: date, time, string, math

#include <stdlib.h>
#include <string>
#include <vector>
#include <math.h>
#include <sys/time.h>

namespace Utils {
    
    void julianDayToGregorianDate(int julianDayNumber, int* year, int* month, int* day, const char** dayOfWeek)
        // convert julian day number to gregorian date 
	{
	    /* 
	     * Fliegel-Van Flandern algorithm
	     * http://mathforum.org/library/drmath/view/51907.html
	     * http://home.capecod.net/~pbaum/date/back.htm#JDN
	     */
	    
	    static const char* const names[] = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	    
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
	    if (dayOfWeek)
		*dayOfWeek = names[julianDayNumber % 7];
	}

    void secondsToGregorianDateTime(double t, int* year, int* month, int* day, int* hours, int* minutes, int* seconds, int* ms, const char** dayOfWeek)
	// convert [t seconds from unix epoch] to [gregorian date and time]
	{
	    const int epoch = 2440588; // julian day number of unix epoch January 1st 1970 AD
	    int s((int)t);
	    
	    if (ms)
		*ms = (int)fmod(t*1000.0, 1000.0);
	    if (seconds)
		*seconds = s % 60;
	    if (minutes)
		*minutes = (s/60) % 60;
	    if (hours)
		*hours = (s/3600) % 24;
	    julianDayToGregorianDate(s/(3600*24) + epoch, year, month, day, dayOfWeek);
	}
    
    double wallTime()
	// seconds since unix epoch midnight (00:00) UTC, January 1st 1970 AD  
	{
	    struct timeval tv;
	    gettimeofday(&tv, NULL);
	    return static_cast<double>(tv.tv_sec) + static_cast<double>(tv.tv_usec)/1000000.0;
	}
	
    void lexQuotedTokens(const std::string& str, std::vector<std::string>* tokens)
	// lex str into whitespace delimited tokens.  curly braces are used to quote
	// tokens containing whitespace. braces can be nested but must be balanced.
	{
	    assert(tokens != NULL);
	    
	    const char* const whitespace = " \t";
	    const char openQuote('{'), closeQuote('}');
	    
	    tokens->clear();
	    int start = 0;
	    int end;
	    while (start < str.length() && start >= 0) {
		if (str[start] == openQuote) {
		    int openBraceCount = 1;
		    if (start+1 < str.length()) {
			start++;
			if (str[start] != closeQuote) {
			    end = start+1;
			    while (end < str.length() && openBraceCount > 0) {
				if (str[end] == openQuote)
				    openBraceCount++;
				else if (str[end] == closeQuote)
				    openBraceCount--;
				end++;
			    }
			} else {
			    openBraceCount = 0;
			    start++;
			    end = start+1;
			}
		    }
		    if (openBraceCount == 0) {
			tokens->push_back(str.substr(start, end-1-start));
		    } else {
			std::cerr << "Utils::lexQuotedTokens(): unbalanced braces: " << str << std::endl;
			end = str.length();
		    }
		    
		} else if (str[start] == closeQuote) {
		    std::cerr << "Utils::lexQuotedTokens(): unbalanced braces: " << str << std::endl;
		    end = str.length();
		    
		} else {
		    end = str.find_first_of(whitespace, start);
		    tokens->push_back(str.substr(start, end-start));
		}
		
		start = str.find_first_not_of(whitespace, end);
	    }
	}

	double asDouble(const std::string& s)
	    // value of s as a double, or 0 if not a double
	    {
		char* end;
		const char* start = s.c_str();
		double x = strtod(start, &end);
		return (start != end) ? x : 0.0;
	    }

	double deg2rad(double degrees)
	    // convert degrees to radians
	    { return M_PI/180.0*degrees; }

	double rad2deg(double radians)
	    // convert radians to degrees
	    { return 180.0/M_PI*radians; }

	osg::Vec3d geo2xyz(double lat, double lon, double elev)
	    // convert lat,lon,elev to x,y,z
	    {
		double theta = deg2rad(lon);
		double phi = deg2rad(lat);
		return osg::Vec3d(elev*cos(phi)*cos(theta), elev*cos(phi)*sin(theta), elev*sin(phi));
	    }
	    
	void xyz2geo(const osg::Vec3d& v, float* lat, float* lon, float* elev)
	    // convert x,y,z, to lat,lon,elev 
	    {
		assert(lat && lon && elev);
		
		*elev = (float)v.length();
		*lat  = 90.0f - (float)rad2deg(acos(v.z()/(*elev)));
		*lon  = (float)rad2deg(atan2(v.y(), v.x()));
	    }
	
	std::ostream& operator<<(std::ostream& s, const osg::Vec3& v)
	    // stream out an osg::Vec3
	    { return (s << v[0] << ' ' << v[1] << ' ' << v[2]); } 

	const double planetRadius = 6378137.0; 
	    // equatorial radius of earth in meters
}

// XXX is there a nicer way to do this than a using directive in the header?
using Utils::operator<<;

#endif
