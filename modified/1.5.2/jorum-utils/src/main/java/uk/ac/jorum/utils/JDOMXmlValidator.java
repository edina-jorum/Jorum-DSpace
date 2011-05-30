/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : JDOMXmlValidator.java
 *  Author              : gwaller
 *  Approver            : Gareth Waller 
 * 
 *  Notes               :
 *
 *
 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * HISTORY
 * -------
 *
 * $LastChangedRevision$
 * $LastChangedDate$
 * $LastChangedBy$ 
 */
package uk.ac.jorum.utils;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author gwaller
 *
 */
public class JDOMXmlValidator {

	public static void usage(){
		System.out.println("Usage: " + JDOMXmlValidator.class.getCanonicalName() + " <xml file>");
		System.exit(1);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1){
			usage();
		}
		
		SAXBuilder builder = new SAXBuilder(true); // Note: Also will expand entity references so no need to check in value strings

        // Set validation feature
        builder.setFeature("http://apache.org/xml/features/validation/schema", true);
        		
        // Parse the manifest file
        Document doc;
        try
        {
            doc = builder.build(args[0]);
           
            XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
            System.out.println("Validated XML DOCUMENT:");
            System.out.println(outputPretty.outputString(doc));
              
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

	}

}
