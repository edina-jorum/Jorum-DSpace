/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : METSPackageDetector.java
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
package uk.ac.jorum.packager.detector;

import org.dspace.content.Bitstream;
import org.dspace.content.packager.AbstractMETSIngester;
import org.dspace.content.packager.DSpaceMETSIngester;
import org.dspace.content.packager.PackageIngester;
import org.jdom.Document;


/**
 * @author gwaller
 *
 */
public class METSPackageDetector extends BasePackageDetector{

	public METSPackageDetector(Bitstream b){
		this.setBitstream(b);
	}
	
	public METSPackageDetector(){
		super();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#isValidPackage()
	 */
	@Override
	public boolean isValidPackage() {
		
		Document manifest = this.containsManifest(AbstractMETSIngester.MANIFEST_FILE);
		
		/*
		 * NOTE:
		 * This isn't the most efficient way of using the manifest - it is parsed on the above step
		 * and then in the actual ingester it is parsed again. Need to refactor and pass in this already
		 * parsed instance.
		 */
		
		return (manifest != null);
	}

	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#ingesterClass()
	 */
	@Override
	public Class<? extends PackageIngester> ingesterClass() {
		return DSpaceMETSIngester.class;
	}

	
	
}
