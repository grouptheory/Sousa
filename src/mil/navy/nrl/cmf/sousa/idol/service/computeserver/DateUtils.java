// File: DateUtils.java

package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.util.Calendar;
import mil.navy.nrl.cmf.stk.*;

/**
   DateUtils
 */
class DateUtils
extends MathSupport
implements SxPxConstants
{
static double julian_date(/*@ non_null */ Calendar cal)
{
	int year = cal.get(Calendar.YEAR);
	int month = cal.get(Calendar.MONTH) + 1;
	int day = cal.get(Calendar.DAY_OF_MONTH);
	int hour = cal.get(Calendar.HOUR_OF_DAY);
	int minute = cal.get(Calendar.MINUTE);
	double second = ((double)cal.get(Calendar.SECOND))
		+ (((double)cal.get(Calendar.MILLISECOND)) / 1000.0);

	return julian_date(year, month, day, hour, minute, second);
}

static double julian_date(int year, int month, int day_of_month, int hour, int minute, double second)
{
	int M1 = (month - 14) / 12;
	int Y1 = year + 4800;
	int i = 1461 * (Y1 + M1) / 4 + 367 * (month - 2 - 12 * M1) / 12 - 
		(3 * ((Y1 + M1 + 100) / 100)) / 4 + day_of_month - 32075;
	double f = ((double)hour + ((double)minute + (second / 60.0)) / 60.0) / 24.0;
	return ((double)i) - 0.5 + f;
}

static double gmst(/*@ non_null */ Calendar cal)
{
	int year = cal.get(Calendar.YEAR);
	int month = cal.get(Calendar.MONTH) + 1;
	int day = cal.get(Calendar.DAY_OF_MONTH);
	int hour = cal.get(Calendar.HOUR_OF_DAY);
	int minute = cal.get(Calendar.MINUTE);
	double second = ((double)cal.get(Calendar.SECOND))
		+ (((double)cal.get(Calendar.MILLISECOND)) / 1000.0);

	return gmst(year, month, day, hour, minute, second);
}

static double gmst(int year, int month, int day_of_month, int hour, int minute, double second)
{
	return gmst(julian_date(year, month, day_of_month, hour, minute, second));
}

static double gmst(double jd)
{
	double t = jd + 0.5;
	double ut = t - floor(t);
	jd -= ut;
	double tu = (jd - 2451545.0) / 36525;
	double result = 24110.54841 + tu
		* (8640184.812866 + tu * (0.093104 - tu * 6.2E-6));
	result = mod(result + SECDAY * 1.00273790934 * ut, SECDAY);
	return TWOPI * result / SECDAY;
}
}; // DateUtils
