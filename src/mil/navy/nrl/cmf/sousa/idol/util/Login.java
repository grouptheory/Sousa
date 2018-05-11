package mil.navy.nrl.cmf.sousa.idol.util;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
   Login
 */
public final class Login
{
// Constructors

/**
   Login()
   @methodtype ctor
 */
private
Login()
{
}

// sousa.util.Login

/**
   login(String, PrivilegedExceptionAction)
   @methodtype command
   @param context .
   @param action .
   @return Object
   @throws LoginException .
   @throws PrivilegedActionException .
 */
public static final Object
login(/*@ non_null */ String context, /*@ non_null */ PrivilegedExceptionAction action)
throws LoginException, PrivilegedActionException
{
	LoginContext lc = new LoginContext(context);

	lc.login();
	try {
		Subject subject = lc.getSubject();
/*
		System.out.println("Principals");
		System.out.println("==========");
		Set principals = subject.getPrincipals();
		for (Iterator it = principals.iterator(); it.hasNext(); ) {
			Object principal = it.next();
			System.out.println(principal.getClass().getName());
			System.out.println(principal);
			System.out.println("---------------------------------------");
		}
		System.out.println();
		System.out.println("Private Credentials");
		System.out.println("===================");
		Set privcreds = subject.getPrivateCredentials();
		for (Iterator it = privcreds.iterator(); it.hasNext(); ) {
			Object cred = it.next();
			System.out.println(cred.getClass().getName());
			System.out.println(cred);
			System.out.println("---------------------------------------");
		}
		System.out.println();
		System.out.println("Public Credentials");
		System.out.println("==================");
		Set pubcreds = subject.getPublicCredentials();
		for (Iterator it = pubcreds.iterator(); it.hasNext(); ) {
			Object cred = it.next();
			System.out.println(cred.getClass().getName());
			System.out.println(cred);
			System.out.println("---------------------------------------");
		}
		System.out.println();
*/
		return Subject.doAs(subject, action);
	} finally {
		lc.logout();
	}
}
}; // Login
