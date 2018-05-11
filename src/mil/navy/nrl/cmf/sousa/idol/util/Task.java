package mil.navy.nrl.cmf.sousa.idol.util;

import java.util.Calendar;

/**
   Task
 */
public abstract class Task
{
	/**
	   ASAP
	 */
	private static final Long ASAP = new Long(0L);

	/**
	   _when
	 */
	/*@ non_null */ private final Long _when;

// Constructors

/**
   Task()
   @methodtype ctor
 */
protected
Task()
{
	this._when = ASAP;
}

/**
   Task(long)
   @methodtype ctor
   @param delay .
 */
protected
Task(long delay)
{
	this._when = new Long(System.currentTimeMillis() + delay);
}

/**
   Task(Calendar)
   @methodtype ctor
   @param when .
 */
protected
Task(/*@ non_null */ Calendar when)
{
	this._when = new Long(when.getTimeInMillis());
}

// mil.navsy.nrl.cmf.idol.util.Task (abstract)

/**
   run()
   @methodtype command
 */
public abstract void
run();

// mil.navsy.nrl.cmf.idol.util.Task (package private)

/**
   when()
   @methodtype get
   @return Long
 */
final Long
when()
{
	return _when;
}
}; // Task
