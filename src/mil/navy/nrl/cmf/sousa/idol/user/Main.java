// File: Main.java
package mil.navy.nrl.cmf.sousa.idol.user;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.sousa.idol.util.Login;

import org.apache.log4j.Logger;

/**
   Main is a generic launcher of IDOL user Entities.  It uses
   Kerberos authentication.
   <P>
   Usage (under UNIX): 
   <PRE>
	env LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APP_HOME/ext/@sys \
		 LD_LIBRARYN32_PATH=$LD_LIBRARY_PATH:$APP_HOME/ext/@sys \
	$JAVA_HOME/bin/java $JVMARGS -Xms1000M -Xmx1000M \
	-cp $CLASSPATH -Dapp.home=$APP_HOME \
	-Djava.library.path=$APP_HOME/ext/@sys \
	\
	-Dsun.security.krb5.principal=$KRB5PRINCIPAL \
	-Dsun.security.krb5.realm=$KRB5REALM \
	-Dsun.security.krb5.kdc=$KRB5KDC \
	-Dkrb5.ccname=`echo $KRB5CCNAME | sed 's/FILE://g'` \
	\
	-Djava.security.auth.login.config=$APP_HOME/etc/user.jaas.conf \
	\
	-Djdbc.drivers=$JDBC_DRIVER \
	\
	-Didol.conf=$CONF
	mil.navy.nrl.cmf.sousa.idol.service.Main 
	</PRE>
	<P>
	These are the definitions of the variables used above:
	<UL>
	<LI><code>APP_HOME</code> the location of the SOUSA framework
	<LI><code>CLASSPATH</code> the Java classpath
	<LI><code>CONF</code> the properties file for the service
	<LI><code>JAVA_HOME</code> the directory where the Java Runtime Environment resides
	<LI><code>JDBC_DRIVER</code> the fully qualified name of the JDBC driver (for example, org.postgresql.Driver)
	<LI><code>JVMARGS</code> extra arguments for the JVM
	<LI><code>KRB5KDC</code> the name of the Kerberos V5 KDC (for example, guardian.cmf.nrl.navy.mil)
	<LI><code>KRB5PRINCIPAL</code> the name of the Kerberos V5 principal
	<LI><code>KRB5REALM</code> the Kerberos V5 realm (for example, CMF.NRL.NAVY.MIL)
	<LI><code>KRB5CCNAME</code> the name of the Kerberos V5 credentials cache file
	<LI><code>LD_LIBRARY_PATH</code> the paths to all of the extra shared object libraries
	</UL>
*/
public final class Main
{
	protected static final Logger _LOG = 
	Logger.getLogger(Main.class);

    // Constructors

    /**
	   Private constructor
    */
    private
		Main()
    {
    }

	private static final Object realMain() 
		throws PrivilegedActionException {
		try {
			// Wait on _forever. Exit when it is notified.
			// For now, _forever will not be notified.
			Object _forever = new Object();
			Properties p = new Properties();
			String source = System.getProperty("idol.conf",
											   System.getProperty("user.home") + 
											   File.separator + 
											   ".idol.properties");
			if (source.startsWith("http")) {
				p.load(new URL(source).openStream());
			} else {
				p.load(new FileInputStream(source));
			}

			String initializerClassString = 
				p.getProperty("idol.entity.initializer");
			Class initializerClass = 
				Class.forName(initializerClassString);

			// Formal arguments of the Constructor: a single
			// Properties Object.
			Constructor initializeCtor = 
				initializerClass.getConstructor(new Class []{Properties.class});

			// Actual arguments of the Constructor: p
			EntityInitializer initializer = 
				(EntityInitializer)initializeCtor.newInstance(new Object[]{p});
					
			Entity e = new Entity(initializer.getContactPort(),
								  initializer.getMcastAddress(),
								  initializer.getMcastBasePort(),
								  initializer.getState(),
								  initializer.getControlLogic(),
								  initializer.getQoSClasses(),
								  initializer.getCommandLogics(),
								  initializer.getContentTypes());

			initializer.scheduleInitialFetches(e);

			Main._LOG.warn("Client initialized.");

			synchronized(_forever) {
				_forever.wait();
			}

			return null;
		} catch (Exception ex) {
			throw new PrivilegedActionException(ex);
		}
	}

    // sousa.idol.user.Main

	/**
	   Authenticates the user <EM>WITH THE WHAT</EM>, constructs and
	   then launches the Entity.

	   <P>

	   The value of the property <code>idol.conf</code> is the name of
	   the properties file that defines the runtime properties of the
	   service.  Some of the properties are defined in {@link
	   mil.navy.nrl.cmf.sousa.idol.service.ServerInitializer} and
	   {@link mil.navy.nrl.cmf.sousa.idol.IdolInitializer}.  Services
	   may define their own properties requirements.  The default
	   value of <code>idol.conf</code> is
	   <code>/etc/idol/idol.properties</code>.

	   <P>
	   The properties file must contain the property
	   <code>idol.entity.initializer</code>.  The value is the fully
	   qualified name of the class that initializes the user
	   Entity.  This class must be a descendant of
	   mil.navy.nrl.cmf.sousa.idol.IdolInitializer.

	   <P>
	   BUGS: The only way to stop a user is with Ctrl-C or an
	   equivalent signal.

	   @param args ignored by this implementation.  All runtime
	   arguments come from properties.
    */
    public static final void
	main(String[] args)
    {
		try {
			String subvertLogin = System.getProperty("subvertLogin");
			if (null != subvertLogin) {
				realMain();
			} else {
				Login.login(Main.class.getName(), new PrivilegedExceptionAction()
					{
						// java.security.PrivilegedExceptionAction

						/**
						   @see PrivilegedExceptionAction#run()
						*/
						//@ also_ensures (\result == null);
						public final Object
							run()
							throws PrivilegedActionException
						{
							return realMain();
						}
					});

				System.exit(0);
			}
		} catch (Exception ex) {
			_LOG.fatal(new Strings(new Object[]
				{StackTrace.formatStackTrace(ex)}));
			System.exit(-1);
		}
	}
}; // Main
