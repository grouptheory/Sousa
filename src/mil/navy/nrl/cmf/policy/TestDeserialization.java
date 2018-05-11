package mil.navy.nrl.cmf.policy;

import com.skaringa.javaxml.DeserializerException;
import com.skaringa.javaxml.NoImplementationException;
import com.skaringa.javaxml.ObjectTransformer;
import com.skaringa.javaxml.ObjectTransformerFactory;
import com.skaringa.javaxml.SerializerException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle;

public class TestDeserialization {

	/*
	  In this test, we create a PolicyElement for three users and
	  seven QueryResultHandles.
	 */
	public static void main(String args[]) {
		List identities = new LinkedList();
		List results = new LinkedList();

		try {
            // Get an ObjectTransformer.
            // The ObjectTransformer interface offers all needed methods.
            ObjectTransformer trans =
                ObjectTransformerFactory.getInstance().getImplementation();

			/*
            // The transformer should create extra line feeds in the output.
            trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");

            // Set the amount of indenting if Xalan is used as XSL transformer.
            trans.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
			*/

            FileInputStream in = new FileInputStream(args[0]);
            PolicyList p = (PolicyList)trans.deserialize(new StreamSource(in));
            in.close();

			for (Iterator i=p.iterator(); i.hasNext(); ) {
				Iterator j;
				PolicyElement e = (PolicyElement)i.next();
				System.out.print("Identities: ");
				for (j=e.identities().iterator(); j.hasNext(); ) {
					System.out.print(j.next() + ", ");
				}
				System.out.println();

				for (j=e.results().iterator(); j.hasNext(); ) {
					System.out.println("Result: " + j.next());
				}

				System.out.println("+++++++++++++++++++++++++");
			}
		} catch (NoImplementationException ex) {
			System.out.println(ex);
		} catch (DeserializerException ex) {
			System.out.println(ex);
		} catch (FileNotFoundException ex) {
			System.out.println(ex);
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}