/**
 * 
 */
package uk.ac.jorum.licence;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;


/**
 * @author gwaller
 * 
 *         This should not be instantiated directly but instantiated via the
 *         DSpace plugin manager to create a singleton instance.
 *         Did not create code to ensure this is a singleton directly (e.g. a
 *         getInstance method) as to follow the current DSpace plugin
 *         architecture and allow an alternative implementation of a
 *         LicenceManager e.g. config could be read from db etc
 * 
 */
public class JorumLicenceManager extends BaseLicenceManager {

	private static Logger logger = Logger.getLogger(JorumLicenceManager.class);

	protected final static String CONFIG_SECTION_KEY = "licence." + JorumLicenceManager.class.getCanonicalName() + ".section";
	protected final static String CONFIG_PREFIX = "licence.uk.ac.jorum.Jorum"; 
	
	public JorumLicenceManager(){
		super(CONFIG_PREFIX);
	}
	
	
	public String getSectionName() {
		String name = ConfigurationManager.getProperty(CONFIG_SECTION_KEY);
		if (name == null){
			name = "Jorum Licences";
		}
		
		return name;
	}


	public ItemLicence[] getInstalledLicences() {
		return super.getInstalledLicences();
	}

	
	
	
	



	
}
