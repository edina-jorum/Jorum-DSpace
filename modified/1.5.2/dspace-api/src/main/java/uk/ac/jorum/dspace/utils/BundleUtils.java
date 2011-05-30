package uk.ac.jorum.dspace.utils;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import uk.ac.jorum.utils.ExceptionLogger;


/**
 * @author gwaller
 *
 */
/**
 * @author cgormle1
 *
 */
public class BundleUtils {

	private static Logger logger = Logger.getLogger(BundleUtils.class);

	
	public static int highestSequenceNumInBundles(Item item){
		int highestSeq = 0;
		try{
			Bundle[] bunds = item.getBundles();
		
			// find the highest current sequence number
	        for (int i = 0; i < bunds.length; i++)
	        {
	            Bitstream[] streams = bunds[i].getBitstreams();

	            for (int k = 0; k < streams.length; k++)
	            {
	                if (streams[k].getSequenceID() > highestSeq)
	                {
	                    highestSeq = streams[k].getSequenceID();
	                }
	            }
	        }

		} catch (Exception e){
			ExceptionLogger.logException(logger, e);
		}
				        
        return highestSeq;
	}
	
	/**
	 * This method copies all bundles and bitstreams from one item to another, creating bundles as necessary.
	 * If a bundle in the destination item exits, the bitstreams are meerly copied but the sequence numbers in the
	 * copied bitstreams are adjusted i.e. they start at a sequence number greater than the highest sequence number
	 * already existing in any bundle in the destination item
	 * NOTE: update is called on each item to ensure the sequence numbers are set before any copying.
	 * @param fromItem - source item to copy from NOTE: sequence ids for bitstreams in this instance will be altered also!
	 * @param toItem - destination item to store the copied bundles/bitstreams
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	public static void copyBundlesAndResequence(WorkspaceItem fromItem, Item toItem, String[] exclusions) throws SQLException, AuthorizeException{
		
		// Call update on the items to ensure the sequence numbers are set - do this with auth turned off as a non admin user may be doing this
		// on an installed item!
		try{
			toItem.getContext().turnOffAuthorisationSystem();
			toItem.update();
			fromItem.getItem().getContext().turnOffAuthorisationSystem();
			fromItem.getItem().update();
		} finally{
			toItem.getContext().restoreAuthSystemState();
			fromItem.getItem().getContext().restoreAuthSystemState();
		}
		
		// Get the higest sequence number in the item we are copying to (rem sequence numbers have to be unique)
		int highestSequence = highestSequenceNumInBundles(toItem);
		
		Bundle[] packageBundles = fromItem.getItem().getBundles();
		logger.debug("Found " + packageBundles.length + " bundles on submitted package item");
		for (Bundle packageBundle:packageBundles){
			
			// Check bundle name isn't in the exclusion list
			boolean exclusionFound = false;
			if (exclusions != null){
				for (int i = 0; i < exclusions.length; i++){
					if (exclusions[i].equals(packageBundle.getName())){
						exclusionFound = true;
						break;
					}
				}
			}
			
			// if we found an exclusion - move onto the next
			if (exclusionFound){
				continue;
			}
		
			
			logger.debug("Adding bundle to orig submission item: " + packageBundle );
			Bundle[] matchingBundles = toItem.getBundles(packageBundle.getName());
			if (matchingBundles.length > 0){
				// add the bitstreams to the matching bundle - use the first one found
				Bitstream[] packageStreams = packageBundle.getBitstreams();
				for (int i = 0; i < packageStreams.length; i++){
					// NOTE: THIS JUST COPIES THE POINTER - SHOULD REALLY CLONE HERE!!
					Bitstream packageBitStream = packageStreams[i];
					// Need to alter the sequence number of the bitstream if we are adding to an existing package
					// NOTE: THIS WILL RESET THE SEQ ID IN THE BITSTREAM WE WISH TO COPY TOO!!
					packageBitStream.setSequenceID(packageBitStream.getSequenceID() + highestSequence);
				
					// Update the db
					packageBitStream.update();
					matchingBundles[0].addBitstream(packageStreams[i]);
				}
				
				// Do not delete the bundle here, even thought he bitstreams were copied - causes a Postgres Exception!
				
			} else {
				// No matching bundle with the same name in the initial Item - just copy the whole bundle from the package
				//toItem.addBundle(packageBundle);
				
				Bundle newBundle = toItem.createBundle(packageBundle.getName());
				Bitstream[] streams = packageBundle.getBitstreams();
				for (Bitstream b : streams){
					b.setSequenceID(b.getSequenceID() + highestSequence);
					b.update();
					newBundle.addBitstream(b);
				}
				
			}
			
		}
	}
	
	
	/**
	 * Helper method to either find the first bundle matching the name supplied or create one if it isn't found.
	 * This method differs from the one in JorumUploadStep as it does not delete any previous 
	 * URL bundle.
	 * @param item the item the bundle should belong to
	 * @param bundleName the name of the bundle to find/create
	 * @return the first bundle found or a newly created one if it didn't previously exist
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	public static Bundle getBundleByName(Item item, String bundleName) throws 
					SQLException,
					AuthorizeException{
		Bundle result;
		
		Bundle[] bundles = item.getBundles(bundleName);
		if (bundles.length == 0){
			// Create the bundle
			result = item.createBundle(bundleName);
		} else {
			// Return the first one
			result = bundles[0];
		}
		
		return result;
	}
	
	/**
	 * This helper method takes some bytes and stores them as a bitstream for an
	 * item in the specified  bundle, with the given bitstream name.
	 * @param bundle  			The bundle to be written to
	 * @param bitstream_name  	The name to be given to the bundle
	 * @param format  			The format of the bundle
	 * @param bytes				The bytes to be written to the bundle
	 * @param setAsPrimary		If set to true, this stream is markes as the primary in the Bundle
	 * @throws SQLException
	 * @throws IOException
	 * @throws AuthorizeException
	 */
	// GWaller 20/8/09 Added param setAsPrimary and made public - used by IMSIngester
	// GWaller 12/11/09 Modified signature - return the bitstream created
	public static Bitstream setBitstreamFromBytes(Bundle bundle, 
											  String bitstream_name, 
											  BitstreamFormat format, 
											  byte[] bytes,
											  boolean setAsPrimary)
			throws SQLException, IOException, AuthorizeException {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		Bitstream bs = bundle.createBitstream(bais);

		bs.setName(bitstream_name);
		bs.setSource(bundle.getName());
		bs.setFormat(format);

		// commit everything
		bs.update();

		if (setAsPrimary){
			//set as primary bitstream
			bundle.setPrimaryBitstreamID(bs.getID());
			bundle.update();
		}
		
		return bs;

	}
	
	
	//CG 09/10/09
	/**
	 * Utility method to check for existence of a url bundle
	 * This should be used to check the type of submission when uploading via Manakin
	 * 
	 * @param subInfo
	 * @return
	 * @throws SQLException
	 */
	public static boolean checkUrl(SubmissionInfo subInfo) throws SQLException{
		Item item = subInfo.getSubmissionItem().getItem();
		if(item.getBundles(Constants.URL_BUNDLE).length>0 && item.getBundles(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE).length==0)
			return true;
		return false;
	}
	
	// START GWaller Support for feed URLs
	public static String getFirstUrlInUrlBundle(Item item) throws SQLException{
		String url = null;
		Bundle urlBundles[] = item.getBundles(Constants.URL_BUNDLE);
		if (urlBundles.length > 0){
			// Get the first bitstream in the first url bundle
			Bitstream streams[] = urlBundles[0].getBitstreams();
			// url shoudl be the bitstream name
			if (streams.length > 0){
				url = streams[0].getName();
			}
		}
		
		return url;
	}
	
	public static void setFirstUrlInUrlBundle(Context context, Item item, String origUrl, String newUrl) throws AuthorizeException, IOException, SQLException{
		
		Bundle urlBundles[] = item.getBundles(Constants.URL_BUNDLE);
		if (urlBundles.length > 0){
			// Get the first bitstream in the first url bundle
			Bitstream stream = urlBundles[0].getBitstreamByName(origUrl);
			
			// delete the stream if we found it
			if (stream != null){
				urlBundles[0].removeBitstream(stream);
			}
			
			BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
			// set the URL as the primary bitstream
			BundleUtils.setBitstreamFromBytes(urlBundles[0], newUrl, bs_format, newUrl.getBytes(), true);
		}
	}
	// END GWaller Support for feed URLs
	
	//CG 30/10/09  Refactored from method originally in JorumUploadStep
	/**
	 * Clears existing content in specified metadata element
	 * and adds new content.  
	 * 
	 * @param content  	The content to be added to the metadata	
	 * @param item 		The submission item
	 * @param schema 	The schema to use (dc)
	 * @param element 	The dc element
	 * @param qualifier Any qualifier for the dc element
	 * @param lang		The language of the entry
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	public static void clearAndSetMetadataElement(String content, Item item, String schema, String element, String qualifier, String lang) throws SQLException, AuthorizeException {
		// Remove existing values
		item.clearMetadata(schema, element, qualifier, Item.ANY);
		if (content != null) {
			item.addMetadata(schema, element, qualifier, lang, content.trim());
		}

		//If we don't write changes to the db and user navigates to previous screen, old value will persist
		item.update();

	}
	
	/**
	 * Helper method to simply check for the existance of a named bundle
	 * @param item the item the bundle should belong to
	 * @param bundleName the name of the bundle to find/create
	 * @return true if a Bundle was found matching the name supplied
	 * @throws SQLException
	 * @throws AuthorizeException
	 */
	public static boolean hasBundle(Item item, String bundleName) throws 
					SQLException,
					AuthorizeException{
		
		Bundle[] bundles = item.getBundles(bundleName);
		return bundles.length > 0;
	}
	
	
	/**
	 * Added by CG 10/11/09 to fix IssueID#134
	 * 
	 * Works out which bundle should be displayed in the uploaded files section of 
	 * the file upload step
	 * 
	 * @param item
	 * @return
	 * @throws SQLException
	 */
	public static Bundle[] getBundlesForDisplay(Item item, Context context) throws SQLException {
		// Get all potential bundles
		Bundle[] original = item.getBundles("ORIGINAL");
		int originalLength = original.length;
		Bundle[] archived = item.getBundles(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
		Bundle[] relatedCP = item.getBundles(Constants.RELATED_CONTENT_PACKAGE_BUNDLE);

		if (relatedCP.length > 0) {
			// If user uploads more than 1 content package
			// the original bundle will be empty, so we
			// have to display the archived CPs from the
			// related bundles
			// Follow ids of the related CP so we can then get the Archived CPs for display 
			Bundle[] bundles = getRelatedCPBundles(context, relatedCP);
			
			// if there is something in the original bitstream we have to display this as well
			if (originalLength > 0) {
				if ((original[0].getBitstreams().length > 0)) {
					int bundlesLength = bundles.length;
					// if someone uploads a normal file as well as a cp, need to display both
					Bundle[] result = new Bundle[originalLength + bundlesLength];
					System.arraycopy(original, 0, result, 0, originalLength);
					System.arraycopy(bundles, 0, result, originalLength, bundlesLength);
					return result;
				}
			}

			return bundles;
		} else if (originalLength > 0 && archived.length == 0) {
			// 2 cases when this occurs:

			// 1 User has just uploaded a cp - not been processed yet
			// therefore the archived cp has not been generated, so 
			// we should just display the contents of the original
			// bundle

			// 2. User has uploaded normal file(s)
			return original;
		}
		// For all other cases, we display the archived content package
		return archived;
	}


	/**
	 * Utility method which gets the ARCHIVED_CONTENT_PACKAGE_BUNDLE of related
	 * items, given the relatedCP of the wrapper item
	 * 
	 * @param context
	 * @param relatedCP
	 * @return
	 * @throws SQLException
	 */
	public static Bundle[] getRelatedCPBundles(Context context, Bundle[] relatedCP) throws SQLException {

		Bundle[] bundles = new Bundle[relatedCP[0].getBitstreams().length];

		int count = 0;
		for (Bundle b : relatedCP) {
			Bitstream[] bitstreams = b.getBitstreams();
			// Cycle through the related items
			// for each, get the ArchivedCP bundle
			for (Bitstream bit : bitstreams) {
				Item relatedItem = (Item) HandleManager.resolveToObject(context, bit.getName());
				bundles[count] = relatedItem.getBundles(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE)[0];
				count++;
			}
		}
		return bundles;
	}
	
	 /**
     * @return the default language qualifier for metadata
     */
    
    public static String getDefaultLanguageQualifier()
    {
       String language = "";
       language = ConfigurationManager.getProperty("default.language");
       if (language == null || language == "")
       {
    	   language = "en";
       }
       return language;
    }
	
    
    public static boolean itemInCollection(Collection c, Item item){
    	boolean result = false;
    	
    	try{
    		Collection itemCollections[] = item.getCollections();
    	
	    	for (Collection itemCol: itemCollections){
	    		if (c.equals(itemCol)){
	    			result = true;
	    			break;
	    		}
	    	}
    	} catch (SQLException e){
    		ExceptionLogger.logException(logger, e);
    	}
    	
    	return result;
    }
	
    
    // START GWaller 02/02/09 IssueID #175 Added methods to deal with licence manipulation inside packages
    /**
     * Renames all bundles matching the name supplied. The new name will be of the form:
     * <prefix>bundle name<suffix>_<unique integer> e.g.
     * "pre-ARCHIVED_CP_dd-MM-yyyy_HH:mm:ss_0" and  "pre-ARCHIVED_CP_dd-MM-yyyy_HH:mm:ss_1" and "pre-ARCHIVED_CP_dd-MM-yyyy_HH:mm:ss_2" etc
     * @param item the item containing the bundle
     * @param bundleName the name of the bundle(s) to rename
     * @param prefix prefix to use for the new name of the bundle(s)
     * @param suffix suffix to use for the new name of the bundle(s)
     * @return a Bundle pointer to the first renamed bundle
     * @throws AuthorizeException
     * @throws SQLException
     */
    public static Bundle renameBundle(Item item, String bundleName, String prefix, String suffix) throws AuthorizeException, SQLException{
    	Bundle firstRenamedBundle = null;
    	Bundle[] bundles = item.getBundles(bundleName);
    	int count = 0;
    	for (Bundle b:bundles){
    		String name;
    		do{
    			name = prefix + b.getName() + suffix + "_" + count++;
    		}
    		while (BundleUtils.hasBundle(item, name));
    		
    		b.setName(name);
    		b.update();
    		if (firstRenamedBundle == null){
    			firstRenamedBundle = b;
    		}
    	}
    	return firstRenamedBundle;
    }
    
    /**
     * Renames all bundles matching the name supplied. The new name will be of the form:
     * <new name>_<unique integer> e.g.
     * "newName_0"
     * @param item the item containing the bundle
     * @param bundleName the name of the bundle(s) to rename
     * @param newName the new name to use 
     * @return a Bundle pointer to the first renamed bundle
     * @throws AuthorizeException
     * @throws SQLException
     */
    public static Bundle renameBundle(Item item, String bundleName, String newName) throws AuthorizeException, SQLException{
    	Bundle firstRenamedBundle = null;
    	Bundle[] bundles = item.getBundles(bundleName);
    	int count = 0;
    	for (Bundle b:bundles){
    		String name;
    		do{
    			name = newName + "_" + count++;
    		}
    		while (BundleUtils.hasBundle(item, name));
    		
    		b.setName(name);
    		b.update();
    		if (firstRenamedBundle == null){
    			firstRenamedBundle = b;
    		}
    	}
    	return firstRenamedBundle;
    }
    
    /**
     * Simply returns a pointer to the first bitstream in a bundle or null if no bitstreams exist
     * @param bundle the bundle to examine
     * @return pointer to the first bitstream or null if none found
     */
    public static Bitstream getFirstBitStream(Bundle bundle){
    	Bitstream streams[] = bundle.getBitstreams();
    	Bitstream result = null;
    	if (streams.length > 0){
    		result = streams[0];
    	} 
    	
    	return result;
    }
    
    // END GWaller 02/02/09 IssueID #175 Added methods to deal with licence manipulation inside packages
    
}
