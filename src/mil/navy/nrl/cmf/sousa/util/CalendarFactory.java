package mil.navy.nrl.cmf.sousa.util;

import java.util.Calendar;
import java.util.Properties;

public final class CalendarFactory implements ObjectFactory
{
	public CalendarFactory() {};

	// Constructs Calendar.  The default value of any parameter is the
	// value contained in the corresponding Calendar created by
	// Calendar.getInstance().
	//
	// These are the suffixes that may be attached to prefix in p:
	//
	// Suffix	Meaning			Range
	// ------	-------			-----
	// .year	year			0-maxint
	// .month   month of year	0-11; Use 0 for January, 11 for December
	// .day		day of month	0-{28, 29, 30, 31}, depending on the month
	// .hour	hour of day		0-23
	// .minute	minute			0-59
	// .second	second			0-59
	//
	// BUGS: Ignores TimeZone and Locale.
	//
	public Object create(String prefix, Properties p)
		throws IllegalArgumentException 
	{
		// DAVID: Consider getting the timezone from p.
		Calendar now = Calendar.getInstance();

		int year = intVal(prefix, ".year", String.valueOf(now.get(Calendar.YEAR)), p, 0, Integer.MAX_VALUE);
		int month = intVal(prefix, ".month", String.valueOf(now.get(Calendar.MONTH)), p, 0, 11);
		int day = intVal(prefix, ".day", String.valueOf(now.get(Calendar.DAY_OF_MONTH)), p, 0, 31);
		int hour = intVal(prefix, ".hour", String.valueOf(now.get(Calendar.HOUR_OF_DAY)), p, 0, 23);
		int minute = intVal(prefix, ".minute", String.valueOf(now.get(Calendar.MINUTE)), p, 0, 59);
		int second = intVal(prefix, ".second", String.valueOf(now.get(Calendar.SECOND)), p, 0, 59);

		now.clear();
		now.set(year, month, day, hour, minute, second);

		return now;
	}

		private int intVal(String prefix, String fieldName, String defaultVal,
						   Properties p, int minVal, int maxVal) 
		throws IllegalArgumentException
	{
		int answer = 0;
		String val = p.getProperty(prefix + fieldName, defaultVal);

		try {
			answer = Integer.parseInt(val);
			if ((answer < minVal) || (answer > maxVal))
				throw new IllegalArgumentException("Expecting " + minVal + "-" + maxVal + " for " + 
												   prefix + fieldName + " but found " + 
												   val + " instead.");
		} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Expecting " + minVal + "-" + maxVal + " for " + 
												   prefix + fieldName + " but found " + 
												   val + " instead.");
		}

		return answer;
	}
}
