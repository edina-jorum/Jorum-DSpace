/**
 * 
 */
package uk.ac.jorum.licence;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * @author gwaller
 *
 */
public abstract class BaseLicenceManager implements LicenceManager{

	private static Logger logger = Logger.getLogger(BaseLicenceManager.class);
	
	protected Hashtable<String, ItemLicence> licenceHash;
	protected LinkedList<ItemLicence> displayOrderedLicenceList;
	private String configPrefix;
	
	public BaseLicenceManager(String configPrefix) {
		this.configPrefix = configPrefix;
		init();
	}

	private String extractAliasFromPropKey(String key, String matchedSuffix){
		return key.substring(configPrefix.length(), key.length() - matchedSuffix.length());
	}
	
	private void parseConfigProperty(String property, String value){
		Object propValue = null;
		String alias = null;
		String matchedKey = null;
		ArrayList<Group> authorisedGroups = new ArrayList<Group>();
		
		if (value != null){
			value = value.trim();
		} else {
			return;
		}
		
		if (property.endsWith(ItemLicence.NAME_KEY)){
			// Got the licence name
			alias = extractAliasFromPropKey(property, ItemLicence.NAME_KEY);
			matchedKey = ItemLicence.NAME_KEY;
		} else if (property.endsWith(ItemLicence.ICON_KEY)){
			// Got the licence icon path
			alias = extractAliasFromPropKey(property, ItemLicence.ICON_KEY);
			matchedKey = ItemLicence.ICON_KEY;
		} else if (property.endsWith(ItemLicence.POSITION_KEY)){
			// Got the licence display position
			alias = extractAliasFromPropKey(property, ItemLicence.POSITION_KEY);
			matchedKey = ItemLicence.POSITION_KEY;
		} else if (property.endsWith(ItemLicence.URL_KEY)){
			// Got the licence url
			alias = extractAliasFromPropKey(property, ItemLicence.URL_KEY);
			matchedKey = ItemLicence.URL_KEY;
		} else if (property.endsWith(ItemLicence.ALLOW_SUBMISSION_KEY)){
			// Got the licence allow submission boolean
			alias = extractAliasFromPropKey(property, ItemLicence.ALLOW_SUBMISSION_KEY);
			matchedKey = ItemLicence.ALLOW_SUBMISSION_KEY;
		} else if (property.endsWith(ItemLicence.RDF_STYLESHEET_KEY)){
				// Got the licence RDF Stylesheet name
				alias = extractAliasFromPropKey(property, ItemLicence.RDF_STYLESHEET_KEY);
				matchedKey = ItemLicence.RDF_STYLESHEET_KEY;
		} else if (property.endsWith(ItemLicence.RDF_URL_KEY)){
			// Got the licence RDF URL
			alias = extractAliasFromPropKey(property, ItemLicence.RDF_URL_KEY);
			matchedKey = ItemLicence.RDF_URL_KEY;
		} else if (property.endsWith(ItemLicence.AUTH_GROUPS_DEPOSIT)){
			// Got a comma separated list of authorised groups for deposit
			alias = extractAliasFromPropKey(property, ItemLicence.AUTH_GROUPS_DEPOSIT);
			matchedKey = ItemLicence.AUTH_GROUPS_DEPOSIT;
		}  else if (property.endsWith(ItemLicence.AUTH_GROUPS_VIEW)){
			// Got a comma separated list of authorised groups for viewing bitstreams
			alias = extractAliasFromPropKey(property, ItemLicence.AUTH_GROUPS_VIEW);
			matchedKey = ItemLicence.AUTH_GROUPS_VIEW;
		}  else {
			// property has a recognised prefix but not suffix, ignore.
			logger.warn("parseConfigProperty: Unrecognized suffix in licence property '" + property + "'");
		}
		
		if (alias != null){
			// Pull out the licence item instance from the hash which matches the alias (or create a new one)
			ItemLicence licence = licenceHash.get(alias);
			if (licence == null){
				// Create a new instance and store in the hash
				licence = new ItemLicence();
				
				licenceHash.put(alias, licence);
			}
			
			
			if (matchedKey == ItemLicence.AUTH_GROUPS_DEPOSIT || matchedKey == ItemLicence.AUTH_GROUPS_VIEW){
				// Comma separated list of group names - need to split and check which are valid
				String groupNames[] = value.split("\\s*,\\s*");
				Context c = null;
				try{
					c = new Context();
					for (int i = 0; i < groupNames.length; i++){
						try{
							logger.debug("parseConfigProperty: Checking group '" + groupNames[i] + '"');
							Group g = Group.findByName(c, groupNames[i]); 
							if (g == null){
								// Group not found - trace an error
								logger.error("parseConfigProperty: Error, group '" + groupNames[i] +"' not found!");
							} else {
								// Group found - add it to the array
								authorisedGroups.add(g);
							}
						} catch (Exception e){
							logger.error("parseConfigProperty: Exception caught when searching for group name'" + groupNames[i] + "'. Maybe group doesn't exist?");
						}
					}
					
					// Need to set the proValue pointer so the array of groups gets added to the properties hash
					propValue = authorisedGroups.toArray(new Group[authorisedGroups.size()]);
				} catch (Exception e){
					logger.warn("parseConfigProperty: Caught Exception "+ e.getMessage());
					logger.warn("parseConfigProperty: Authorised groups for deposit or viewing may not have been fully checked");
				}
				finally {
					// IMPORTANT must complete the context to free up the DB connection
					if (c != null){
						try{c.complete();} catch (Exception e){logger.warn("parseConfigProperty: Caught Exception "+ e.getMessage());}
					}
				}
			} else {
				propValue = value;
			}
			
			// Now store the prop
			
			licence.getProps().put(matchedKey, propValue);
		}
		
	}
	
	
	private void init() {
		// Read config and setup licences
		licenceHash = new Hashtable<String, ItemLicence>();
		
		Enumeration pe = ConfigurationManager.propertyNames();

		logger.debug("BaseLicenceManager: Looking for config prefix = " + configPrefix);
		
		while (pe.hasMoreElements()) {
			String key = (String) pe.nextElement();
			if (key.startsWith(configPrefix)) {
				logger.debug("init: Processing config property " + key);
				
				
				parseConfigProperty(key, ConfigurationManager.getProperty(key));
			}
		}


        // Now build the display order for the licences
        this.displayOrderedLicenceList = new LinkedList<ItemLicence>();
        Enumeration <String> e = licenceHash.keys();
		while (e.hasMoreElements()){
			String alias = e.nextElement();
			ItemLicence i = licenceHash.get(alias);

            int posToInsertAt = 0;
            try{
                posToInsertAt = Integer.parseInt(i.getProps().getProperty(ItemLicence.POSITION_KEY, "0"));
            } catch (NumberFormatException ne){
                // don't need to do anything here - the default of zero will be used
            }

            if (this.displayOrderedLicenceList.size() == 0){
                this.displayOrderedLicenceList.add(i);
            } else {
                boolean inserted = false;
                // Iterate across the list and insert at the point where a licence in the list has an equal or higher position hint
                for (int z = 0; z < this.displayOrderedLicenceList.size(); z++){
                    ItemLicence l = this.displayOrderedLicenceList.get(z);

                    int pos = 0;
                    try{
                        pos = Integer.parseInt(l.getProps().getProperty(ItemLicence.POSITION_KEY, "0"));
                    } catch (NumberFormatException ne){
                      // don't need to do anything here - the default of zero will be used
                    }

                    if (posToInsertAt <= pos){
                        this.displayOrderedLicenceList.add(z, i);
                        inserted = true;
                        break;
                    }
                }

                if (! inserted){
                    this.displayOrderedLicenceList.add(i);
                }
            }



		}



	}
	
	public ItemLicence[] getInstalledLicences() {
		ArrayList<ItemLicence> result = new ArrayList<ItemLicence>();
		Enumeration <String> e = licenceHash.keys();		
		while (e.hasMoreElements()){
			String alias = e.nextElement();
			result.add(licenceHash.get(alias));
		}
		
		return result.toArray(new ItemLicence[result.size()]);
	}

	public ItemLicence[] getInstalledLicencesInDisplayOrder(){
	    return this.displayOrderedLicenceList.toArray(new ItemLicence[displayOrderedLicenceList.size()]);
	}
	

}
