/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : IMSPackageDetector.java
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

import java.util.Iterator;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.packager.PackageIngester;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;

import uk.ac.jorum.packager.IMSIngester;
import uk.ac.jorum.packager.SCORMIngester;


/**
 * @author gwaller
 *
 */
public class SCORMPackageDetector extends BasePackageDetector{

	public static Namespace SCORM_1_2_NS = Namespace
    .getNamespace("adlcp1p2", "http://www.adlnet.org/xsd/adlcp_rootv1p2");
	
	public static Namespace SCORM_1_3_NS = Namespace
    .getNamespace("adlcp1p3", "http://www.adlnet.org/xsd/adlcp_v1p3");
	
	public static Namespace[] SCORM_NAMESPACES = {SCORM_1_2_NS, SCORM_1_3_NS}; 
	
	
	public SCORMPackageDetector(Bitstream b){
		this.setBitstream(b);
	}
	
	public SCORMPackageDetector(){
		super();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#isValidPackage()
	 */
	@Override
	public boolean isValidPackage() {
		boolean result = false;
		
		// Try and find an IMS manifest
		Document manifest = this.containsManifest(IMSIngester.MANIFEST_FILE);
		
		/*
		 * NOTE:
		 * This isn't the most efficient way of using the manifest - it is parsed on the above step
		 * and then in the actual ingester it is parsed again. Need to refactor and pass in this already
		 * parsed instance.
		 */
		
		if (manifest != null){
			// Now check to see if the some of the elements belong to the SCORM namespace
			Iterator iter = manifest.getDescendants(new ElementFilter());
			
			// Now walk the elements
			while (iter.hasNext() && !result){
				Object e = iter.next();
				// Should be an element
				if (e instanceof Element){
					// check the namespace of the element first
					for (Namespace n:SCORM_NAMESPACES){
						if (((Element)e).getNamespace().equals(n)){
							// Got a match !
							result = true;
							break; // exit the for loop over namespaces
						} else {
							// Check the attributes now for a matching scorm namespace
							List attrs = ((Element)e).getAttributes();
							for (Object a:attrs){
								if (a instanceof Attribute){
									// Check namespace
									if (((Attribute)a).getNamespace().equals(n)){
										// Got a match
										result = true;
										break; // exit the for loop over attrs
									}
								}
								
								if (result){
									// already found a match - break the for loop for namespaces
									break;
								}
								
							}
						} // namespace for loop 
					}
				}
			}
		
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#ingesterClass()
	 */
	@Override
	public Class<? extends PackageIngester> ingesterClass() {
		return SCORMIngester.class;
	}


	

	

}
