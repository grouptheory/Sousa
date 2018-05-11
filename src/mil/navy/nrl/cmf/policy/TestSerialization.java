package mil.navy.nrl.cmf.policy;

import com.skaringa.javaxml.DeserializerException;
import com.skaringa.javaxml.NoImplementationException;
import com.skaringa.javaxml.ObjectTransformer;
import com.skaringa.javaxml.ObjectTransformerFactory;
import com.skaringa.javaxml.SerializerException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle;

public class TestSerialization {

	/*
	  In this test, we create a PolicyElement for three users and
	  seven QueryResultHandles.
	 */
	public static void main(String args[]) {
		List identities = new LinkedList();
		List results = new LinkedList();

		for (int i=1; i < 4; i++) {
			identities.add(new Identity("user_"+i, "192.168.0."+i));
		}

		for (int i=0; i < 7; i++) {

			Map fields = new HashMap();
			fields.clear();
			fields.put("mapname", "this/that/the-other-thing/file_"+i+".nui");
			fields.put("north", new Double(38.825996 + 0.000001 * i));
			fields.put("south", new Double(38.825245 + 0.000001 * i));
			fields.put("west", new Double(-77.060217 + 0.000001 * i));
			fields.put("east", new Double(-77.058551 + 0.000001 * i));
			fields.put("policy", new String("POLICY_"+i));
			results.add(new QueryResultHandle(i, fields));
		}

		PolicyElement p = new PolicyElement(identities, results);
		PolicyList policyElements = new PolicyList();
		policyElements.add(p);

		// This code is from the Skaringa tutorial
		try {
            // Get an ObjectTransformer.
            // The ObjectTransformer interface offers all needed methods.
            ObjectTransformer trans =
                ObjectTransformerFactory.getInstance().getImplementation();

            // The transformer should create extra line feeds in the output.
            trans.setProperty(javax.xml.transform.OutputKeys.INDENT, "yes");

            // Set the amount of indenting if Xalan is used as XSL transformer.
            trans.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");

			 // Serialize the Person object into a file.
            FileOutputStream out = new FileOutputStream("PolicyList.xml");
            trans.serialize(policyElements, new StreamResult(out));
            out.close();

			// Create an XML schema file that describes the PolicyList class.
            out = new FileOutputStream("PolicyList.xsd");
            trans.writeXMLSchema(PolicyList.class, new StreamResult(out));
            out.close();

            out = new FileOutputStream("PolicyElement.xsd");
            trans.writeXMLSchema(PolicyElement.class, new StreamResult(out));
            out.close();

            out = new FileOutputStream("Identity.xsd");
            trans.writeXMLSchema(Identity.class, new StreamResult(out));
            out.close();
		} catch (NoImplementationException ex) {
			System.out.println(ex);
		} catch (SerializerException ex) {
			System.out.println(ex);
		} catch (FileNotFoundException ex) {
			System.out.println(ex);
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}
}