/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : MetadataFormatException.java
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
package uk.ac.jorum.packager;

import org.dspace.content.crosswalk.MetadataValidationException;
import org.jdom.Element;

/**
 * @author gwaller
 *
 */
public class MetadataFormatException extends MetadataValidationException {

	public MetadataFormatException(Element elem){
		super("Unsupported metadata format found - cannot find match for namespace " + elem.getNamespace().toString());
	}
	
	public MetadataFormatException(){
		super("Unable to determine metadata format. Perhaps metadata elements cannot be found.");
	}
	
}
