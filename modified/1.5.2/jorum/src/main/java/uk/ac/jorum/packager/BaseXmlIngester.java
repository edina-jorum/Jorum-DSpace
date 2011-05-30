/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : BaseXmlIngester.java
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.Context;
import org.jdom.Element;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.exceptions.CriticalException;
import uk.ac.jorum.exceptions.NonCriticalException;
import uk.ac.jorum.licence.LicenceController;
import uk.ac.jorum.utils.ExceptionLogger;

/**
 * @author gwaller
 *
 */
public abstract class BaseXmlIngester implements PackageIngester {

	/** log4j category */
	private static Logger log = Logger.getLogger(BaseXmlIngester.class);
	
	/**
     * Attempts to pull out the CC licence url from the text supplied in the metadata section and then
     * writes this along with the collections deposit licence to the item bundle. The CreativeCommons class
     * is used to obtain the licence text from creativecommons website.
     * @param context
     * @param collection
     * @param item
     * @param manifest
     * @throws MetadataFormatException
     * @throws MetadataValidationException
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void addLicense(Context context, 
    					   Collection collection,
                           Item item,
               			   XMLManifest manifest,
               			   Element mdRootElement,
               			   String forcedCCLicenceUrl) throws MetadataFormatException, 
               			   								MetadataValidationException,
               			   								SQLException,
               			   								IOException,
               			   								AuthorizeException
    {
    	String ccLicenceUrl = null;
    	
    	if (forcedCCLicenceUrl != null && forcedCCLicenceUrl.length() > 0){
    		log.debug("Overridding licence in package to the following CC URL: " + forcedCCLicenceUrl);
    		ccLicenceUrl = forcedCCLicenceUrl;
    	} else {
    		/* 
        	 * NOTE: Remember for the Jorum open service we only support CC licences.
        	 * If we can't match a CC url in the licence text supplied in the manifest then
        	 * we can't assign a CC licence and an exception shoudl be thrown to block the deposit.
        	 */
        	String licenceText;
        	if (mdRootElement != null){
        		// GWaller 19/2/10 IssueID #199 The licence xpath expression should be run from the mdRootElement if we have specified one
        		//                              Required incase we are dealing with say an RSS feed and we want to isolate the licence from the
        		//                              current item we are processing.
        		licenceText = manifest.getMetadataFormat(mdRootElement).getLicenceText(mdRootElement);
        	} else{
        		licenceText = manifest.getMetadataFormat().getLicenceText(manifest.getManifestDocument());
        	}
        	
        	// Must extract a CC licence URL from the licence text which was part of the XML manifest
       	 	ccLicenceUrl = manifest.matchCCLicenceUrl(licenceText); 
       	 	
       	 	if (ccLicenceUrl == null){
    	 		// Unable to find the URL for the CC licence - throw an exception
    	 		throw new MetadataValidationException("CC licence url not found in licence text: " + licenceText);
    	 	}
    	}
    	

    	// Pass in null to choose the default deposit licence set for the collection i.e. the Jorum T&Cs
        PackageUtils.addDepositLicense(context, null, item, collection);

        LicenceController.setLicense(context, item, ccLicenceUrl);        
    }
	
	private void addToCollections(Collection [] collections, Item item) throws SQLException, AuthorizeException{
		for (int i = 1; i < collections.length; i++){
    		// Only add it to a new collection ie now the WorkspaceItem collection
    		if (!BundleUtils.itemInCollection(collections[i], item)){
    			log.debug("classifyItem: Adding item to collection " + collections[i].getName() + " handle = " + collections[i].getHandle());	        			
    			collections[i].addItem(item);
    		}
    	}
	}
    
    
    protected void classifyItem(Context context,
    							String[] classificationsFromManifest, 
    							Collection[] collections, 
    							WorkspaceItem wi,
    							boolean alterOwningCollectionIfNecessary){
    	log.debug("classifyItem: Entering ...");
    	log.debug("classifyItem: Classifications from manifest:");
    	for (int i = 0; i < classificationsFromManifest.length; i++){
    		log.debug("classifyItem: Classifications from manifest [" + i + "] <" + classificationsFromManifest[i] + ">");
    	}
    	
    	
    	Item item = wi.getItem();
    	ArrayList<Collection> exactMatchCollections = new ArrayList<Collection>();
    	ArrayList<Collection> subStringMatchCollections = new ArrayList<Collection>();
    	
    	// GWaller 12/1/10 IssueID #160 Ensure duplicates do not exist in the classifications from manifest
    	HashSet<String> classificationsFromManifestSet = new HashSet<String>();
    	
    	try{
    		if (classificationsFromManifest != null && classificationsFromManifest.length > 0){
    			for (String manifestClassification : classificationsFromManifest ){
    				String manifestClassificationLower = manifestClassification.toLowerCase();
        			
    				// GWaller 12/1/10 IssueID #160 Ensure duplicates do not exist in the classifications from manifest
    				if (classificationsFromManifestSet.contains(manifestClassificationLower)){
    					// already seen this collection - move onto the next
    					continue;
    				} else {
    					// add it into the set so we don't process it twice
    					classificationsFromManifestSet.add(manifestClassificationLower);
    				}
    				
    	    		// See if we can find a collection to use
    	    		Collection[] allCollections = Collection.findAll(context);
    	    		
    	    		for (Collection c: allCollections){
    	    			log.debug("classifyItem: testing against collection < " + c.getName() + ">");
    	    			
    	    			// convert collection to lower case
    	    			String lowerCaseCol = c.getName().toLowerCase();
    	    			
    	    			if (lowerCaseCol.equals(manifestClassificationLower)){
    	    				// found an exact match
    	    				exactMatchCollections.add(c);
    	    				log.debug("classifyItem: Found exact match collection name = " + c.getName() + " handle = " + c.getHandle());
    	    				break;
    	    			} else if (lowerCaseCol.contains(manifestClassificationLower)){
    	    				// found a substring match
    	    				subStringMatchCollections.add(c);
    	    				log.debug("classifyItem: Found substring match collection name = " + c.getName() + " handle = " + c.getHandle());
    	    			}
    	    		}
    			}
    			
	    	}
	    	
    		log.debug("classifyItem: alterOwningCollectionIfNecessary = " + alterOwningCollectionIfNecessary);
    		
	    	if (alterOwningCollectionIfNecessary){
	    		Collection collectionToMoveTo = null;
	    		// change owning collection to the first exact match collection or first substring
	    		if (exactMatchCollections.size() > 0){
	    			collectionToMoveTo = exactMatchCollections.get(0);
	    		} else if (subStringMatchCollections.size() > 0){
	    			collectionToMoveTo = subStringMatchCollections.get(0);
	    		}
	    		
	    		if (collectionToMoveTo != null && wi.getCollection() != collectionToMoveTo){
	    			// Must update the collection in the WorkspaceItem
	    			
	    			log.debug("classifyItem: Altering WorkspaceItem collection to " + collectionToMoveTo.getName() + " handle = " + collectionToMoveTo.getHandle());
	    			
	    			// *NOTE* Remember at this stage the item does not actually have an Owning Collection! This is set when the item 
	    			// is installed by InstallItem - it simply takes the collection from the WorkspaceItem and uses this as the owning collection
	    			wi.setCollection(collectionToMoveTo);
	    			
	    		}
	    		
	    		// We only want ot use classifiation oulled form the manifest if alterOwningCollectionIfNecessary is true (ie not a submission via the web UI)
	    		log.debug("classifyItem: Processing exact collection matches ... ");
		    	addToCollections(exactMatchCollections.toArray(new Collection[exactMatchCollections.size()]), item);
		    	log.debug("classifyItem: Processing substring collection matches ... ");
		    	addToCollections(subStringMatchCollections.toArray(new Collection[exactMatchCollections.size()]), item);
	    	}
	    	
	    	log.debug("classifyItem: Processing passed in collections ... ");
	    	addToCollections(collections, item);
	    	
	    	
	    	
    	} catch (Exception e){
    		// If we get an exception trying to classify the item - just log it and gracefully continue. 
    		// Remember the item will already be stored in the owning collection (ie the first collection given to the ingester)
    		log.warn("Caught excetpion trying to classify item ... item will only appear in owning collection: " + e.getMessage());
    		ExceptionLogger.logException(log, e);
    	}
    	
    	log.debug("classifyItem: Leaving ...");
    }
    
	
	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#ingest(org.dspace.core.Context, org.dspace.content.Collection[], java.io.InputStream, org.dspace.content.packager.PackageParameters, java.lang.String)
	 */
	public abstract WorkspaceItem ingest(Context context, Collection[] collections, InputStream in, PackageParameters params,
			String license) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException;
	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#postInstallHook(org.dspace.core.Context, org.dspace.content.Item)
	 */
	public abstract void postInstallHook(Context context, Item item) throws NonCriticalException, CriticalException;

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#replace(org.dspace.core.Context, org.dspace.content.Item, java.io.InputStream, org.dspace.content.packager.PackageParameters)
	 */
	public abstract Item replace(Context context, Item item, InputStream in, PackageParameters params) throws PackageException,
			UnsupportedOperationException, CrosswalkException, AuthorizeException, SQLException, IOException;

}
