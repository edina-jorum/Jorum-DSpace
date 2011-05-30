/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : SCORMIngester.java
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

import java.io.IOException;
import java.io.InputStream;

import org.dspace.content.crosswalk.MetadataValidationException;

/**
 * @author gwaller
 *
 */
public class SCORMIngester extends IMSIngester {

	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.IMSIngester#createManifest(java.io.InputStream, boolean)
	 */
	@Override
	protected XMLManifest createManifest(InputStream is, boolean validate) throws IOException,
			MetadataValidationException {
		return SCORMManifest.create(is, validate);
	}

	
	
}
