/**
 * 
 */
package uk.ac.jorum.licence;

/**
 * @author gwaller
 *
 */
public interface LicenceManager {
	
	public String getSectionName();
	
	public ItemLicence[] getInstalledLicences();
	
	public ItemLicence[] getInstalledLicencesInDisplayOrder();
		
}
