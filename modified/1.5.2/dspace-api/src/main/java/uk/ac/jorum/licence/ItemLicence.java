/**
 * 
 */
package uk.ac.jorum.licence;

import java.util.Properties;

import org.dspace.eperson.Group;

import uk.ac.jorum.utils.Sequencer;

/**
 * @author gwaller
 *
 */
public class ItemLicence {

	/**
	 * Property keys which map to suffixes of keys in the dspace.cfg file.
	 * NOTE: If another key is added here, remember to parse and set the property in 
	 * @see uk.ac.jorum.licence.BaseLicenceManager#parseConfigProperty
	 */
	public final static String NAME_KEY = ".displayName";
	public final static String ICON_KEY = ".iconPath";
	public final static String URL_KEY = ".url";
	public final static String ALLOW_SUBMISSION_KEY = ".allowWebUIDeposit";
	public final static String RDF_STYLESHEET_KEY = ".rdfStyleSheet";
	public final static String RDF_URL_KEY = ".rdfUrl";
	public final static String AUTH_GROUPS_DEPOSIT = ".authorisedGroupsForDepositing";
	public final static String AUTH_GROUPS_VIEW = ".authorisedGroupsForViewing";
    public final static String POSITION_KEY = ".displayPositionHint";

	
	private Properties props;
	private int id;
	
	public ItemLicence(){
		props = new Properties();
		
		// Set a new unique id
		id = Sequencer.getInstance().next();
	}

	public Properties getProps() {
		return props;
	}

	public int getId() {
		return id;
	}
	
	/**
	 * Private utility method to pull out the authorised group array from the properties hash.
	 * Called by @see uk.ac.jorum.licence.ItemLicence#authorisedGroupsForViewing and 
	 * @see uk.ac.jorum.licence.ItemLicence#authorisedGroupsForDepositing
	 * @param key
	 * @return
	 */
	private Group[] authorisedGroups(String key){
		Group[] result = null;
		// NOTE: use of get not getProperty - use the hashtable directly as an array should have been stored - not a string!
		Object v = props.get(key);
		// Should be an array of groups
		if (v instanceof Group[]){
			result = (Group[])v;
		} else {
			// return a blank array
			result = new Group[0];
		}
		
		return result;
	}
	
	/**
	 * Return an array of Groups who are authorised to view bitstreams on the item assigned with this licence. 
	 * @return the array of DSpace groups who are authorised to view bitstreams. If an array with zero elements is returned, this means that everyone 
	 * is authorised to view. If at least one Group is returned then only members of those groups listed are authorised.
	 */
	public Group[] authorisedGroupsForViewing(){
		return authorisedGroups(AUTH_GROUPS_VIEW);
	}
	
	/**
	 * Return an array of Groups who are authorised to download bitstreams on the item assigned with this licence. 
	 * @return the array of DSpace groups who are authorised to download bitstreams. If an array with zero elements is returned, this means that everyone 
	 * is authorised to download. If at least one Group is returned then only members of those groups listed are authorised.
	 */
	public Group[] authorisedGroupsForDepositing(){
		return authorisedGroups(AUTH_GROUPS_DEPOSIT);
	}
	
	public boolean allowWebUIDeposit(){
		boolean result = false;
		String v = props.getProperty(ALLOW_SUBMISSION_KEY);
		if (v != null){
			result = Boolean.parseBoolean(v);
		}
		
		return result;
	}
	
	
}
