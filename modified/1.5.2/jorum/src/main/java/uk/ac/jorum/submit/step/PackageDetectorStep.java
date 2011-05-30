/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : PackageDetectorStep.java
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
package uk.ac.jorum.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.packager.PackageDetector;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.submit.AbstractProcessingStep;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.utils.ExceptionLogger;


/**
 * @author gwaller
 *
 */
public class PackageDetectorStep extends AbstractProcessingStep {

	private static Logger logger = Logger.getLogger(PackageDetectorStep.class);
	
	// GWaller 2/10/09 Read validation param from config
	public static final String VALIDATE_KEY = "packagedetectorstep.validate.manifest";
	public static final boolean validate = ConfigurationManager.getBooleanProperty(VALIDATE_KEY, true);
	
	// GWaller 17/11/09 Support for feed urls
	public static final String FEED_PREFIX = "feed://";
	
	class FeedProcessingException extends Exception{
		
		public FeedProcessingException(){
			super();
		}
		
		public FeedProcessingException(String message){
			super(message);
		}
		
	}
	
	
	/**
	 * Creates the configuratiokn used by the PackageIngester class - this is essentially a set of properties.
	 * @return the PackageParams instance
	 */
	private PackageParameters createPackagerConfig(){
		PackageParameters params = new PackageParameters();
		// Turn XML validation for manifest on/off depending on config
		params.addProperty(PackageIngester.VALIDATE_PARAM, new Boolean(validate).toString());
		
		// Don't fail if a CC licence isn't matched - the user can still select this via the GUI in a later step
		params.addProperty(PackageIngester.FAIL_NO_LICENCE_PARAM, "false");
		
		// GWaller 6/5/10 IssueID#263 For the web interface only take the first web link found in the metadata
		/* 
		 * !! NOTE !!
		 * 
		 * Setting this to be 1 mainly for an intraLibrary hack. It is possible for there to be multiple 
		 * links in  metadata representing the same resource e.g.
		 * - link to the real web page
		 * - link to the item in intraLibrary (i.e. the preview command link) which redirects to the real web page link
		 * 
		 * This is therefore making the assumption that the first listed link in the metadata is the link to web link for the resource
		*/
		params.addProperty(PackageIngester.MAX_METADATA_WEB_LINKS_TO_USE, "1");
		
		return params;
	}
	
	
	
	
	/**
	 * This method iterates through all the package detector classes as specified by
	 * detectorClasses, creates a detector instance, checks if the bitstream is a package supported 
	 * by the detector, and if so, calls "ingest" to process the package i.e.
	 * create a bundle for the file content, create bitstreams for the files, create a metadata bundle for the
	 * metadata, create a licence bundle for the licence.
	 * @param context the DSpace context
	 * @param subInfo the currently in progress submission created by XMLUI
	 * @param stream the stream of bits to process i.e. the file
	 * @param copyMetadataToItem boolean indicating if the metadata found in the package (if the stream is indeed a supported package)
	 *                           should be copied to the submissioninfo item. This can be used if only one package is ingested and the
	 *                           metadata should appear in the GUI to aid the user when submitting. If the user is however submitting 
	 *                           multiple packages in one submission, this should be set ot false to force the user to enter metadata
	 *                           for this "wrapper" object pointing to the processed packages.
	 * @param session the HttpSession bound to the current HTTP request
	 */
	private void checkPackageAndProcess(Context context, 
										SubmissionInfo subInfo,
										Bundle bundleContainingStream,
										Bitstream stream,
										HttpSession session,
										boolean copyMetadataToItem,
										boolean createArchive,
										Collection[] collections){
		
	
		Item item = subInfo.getSubmissionItem().getItem();
		
		if (collections == null){
			collections = new Collection[]{subInfo.getSubmissionItem().getCollection()};
		}
		
		// GWaller 9/1//09 IssueID #133 - archived content package should not be in wrapper
		Bundle archivedBundle = null;
		
		// Cycle through all the supported package detectors and see if we have a valid match
		for (Class<? extends PackageDetector> detector: PackageUtils.getDetectorClasses()){
			try{
				logger.debug("Checking for validPackage with detector: " + detector.getCanonicalName());
				
				PackageDetector detectorInst = detector.newInstance();
				detectorInst.setBitstream(stream); // need to set the stream the detector should look at
				
				// Check to see if we have a package this detector supports
				if (detectorInst.isValidPackage()){
					// Found package - the ingester class is stored in this instance
					logger.debug("Detected valid package");
					
					Class<? extends PackageIngester> ingesterClass = detectorInst.ingesterClass();
					
					// Create an ingester instance and call ingest
					logger.debug("Instantiating ingester: " + ingesterClass.getCanonicalName());
					PackageIngester ingester = ingesterClass.newInstance();
					
					// Create the packager configuration
					PackageParameters params = createPackagerConfig();
					
					// TODO: Support multiple bitstreams and read collections from manifest!
					logger.debug("Calling ingest on " + ingester);
					WorkspaceItem ingestedPackage = ingester.ingest(context, collections, stream.retrieve(), params, null);
					
					// Add the bitstream to the list of processed streams so we don't expand the package twice if the users moves back and forth
					logger.debug("adding stream name " + stream.getName() + " to processed list");
					
					
					// Need to add item metadata from package - do this if its only 1 package ingested
					// If there are multiple streams we want the user to enter the metadata to this "package wrapper" object
					if ( copyMetadataToItem ){
						/*
						 * Rather than deleting the original submission item in submissioninfo (i.e. the package zip), add new
						 * bitstreams containing the package data and metadata - this leaves the original zip there intact for
						 * archival purposes.
						 */
						BundleUtils.copyBundlesAndResequence(ingestedPackage, item, null);
						
						DCValue[] dcValues = ingestedPackage.getItem().getDC(Item.ANY, Item.ANY, Item.ANY);
						for (DCValue v : dcValues){
							item.addMetadata(v.schema, v.element, v.qualifier, v.language, v.value);
						}
					} else {
						// Dealing with multiple content packages so don't copy anything to this 'wrapper'
					}
					
					/*
					 *  Now we need to tidy up the ingested package WorkspaceItem - delete the wrapper and item otherwise it will appear in 
					 *	unfinished submissions and also have unnecessary duplicate rows in the item table
					 */
					if (copyMetadataToItem){ // Only delete if we have a single content package
						ingestedPackage.deleteAll();
						
						// GWaller 9/1//09 IssueID #133 - archived content package should not be in wrapper
						if (createArchive){
							archivedBundle = BundleUtils.getBundleByName(item, Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
						}
						
					} else {
						// GWaller 9/1//09 IssueID #133 - archived content package should not be in wrapper
						if (createArchive){
							archivedBundle = BundleUtils.getBundleByName(ingestedPackage.getItem(), Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
						}
					}

					/* 
					 * Now move the bitstream containing the original uploaded package into an archived bundle - means 
					 * it won't get processed again if the user moves back and forth between the submission steps
					 */
					
					// GWaller 9/1//09 IssueID #133 The archived bundle will be in this "item" instance if only a single content package was 
					//                              deposited. If however multiple packages were deposited, the "item" instance referes to the 
					//                              wrapper object - the archived bundle should not appear in this! It instead appears in the child
					//                              item installed above.
					if (createArchive){	
						// GWaller 12/1/10 IssueID #161 Before adding the archive bitstream to the related item, reset the sequence number
						//                              so that it will have a unique number when it is installed by installItem
						stream.setSequenceID(-1); // if set to < 0, installItem will reassign the sequence num to a unique val on install
						
						archivedBundle.addBitstream(stream);
						archivedBundle.update();
					}
					
					// GWaller 11/1/10 IssueID #157 Must archive the zip *before* calling install item so that the preview is created!
					if (! copyMetadataToItem){ // ie multiple packages submitted
						// Install the ingested package - this assigns a handle
						Item relatedItem = InstallItem.installItem(context, ingestedPackage);
						
						// Add the handle to the related bundle
						Bundle b = BundleUtils.getBundleByName(item, Constants.RELATED_CONTENT_PACKAGE_BUNDLE);
						BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
						
						BundleUtils.setBitstreamFromBytes(b, relatedItem.getHandle(), bs_format, relatedItem.getHandle().getBytes(), true);
					}
					
					bundleContainingStream.removeBitstream(stream);
					bundleContainingStream.update();
					
					// Break the for loop iterating across package detectors
					break;
				}
			} catch (Exception e){
				ExceptionLogger.logException(logger, e);
				// Problem creating the detector instance - misconfiguration. Just assume it is a regular file and let it through
			}
		}
		
	}
	
	
	 public int doProcessing(Context context,
	            HttpServletRequest request, HttpServletResponse response,
	            SubmissionInfo subInfo) throws ServletException, IOException,
	            SQLException, AuthorizeException{
		 
		 logger.debug("PackageDetectorStep::doProcessing Entering ...");
		 
		 int result = STATUS_COMPLETE;
		 boolean multipleFiles = false;
		 Item item = subInfo.getSubmissionItem().getItem();
		 String updatedUrl = null;
		 
		 // Check to see if the user submitted a URL as this may be a feed URL which we can process
		 try{
			 if (BundleUtils.checkUrl(subInfo)){
				 // User submitted a URL - now check to see if it is a feed url
				 String url = BundleUtils.getFirstUrlInUrlBundle(item);
				 if (url != null){
					 Collection[] collections = null;
					 
					 // Got a URL - check for the feed prefix
					 if (url.startsWith(FEED_PREFIX)){
						// We only want to support feed ingest via the GUI if the user is an admin
						 if (!AuthorizeManager.isAdmin(context)){
							 logger.warn("Non-admin user attempting to process a feed url <" + url + ">");
							 throw new FeedProcessingException("Non admin user attempted to enter a feed <" + url + ">");
						 } 
						 
						 // Create a FEED bundle
						 Bundle feedBundle = BundleUtils.getBundleByName(item, Constants.FEED_BUNDLE);
						 // Now store the def collection if supplied
						 int atPos = url.indexOf("@");
						 
						 String colHandle = subInfo.getCollectionHandle();
						 String forcedCollectionHandle = null;
						 if (atPos > -1){
							 forcedCollectionHandle = url.substring(FEED_PREFIX.length(), atPos);
							 // Check collection exists
							 DSpaceObject obj = HandleManager.resolveToObject(context, colHandle);
							 if (obj == null){
								 logger.error("Supplied feed collection handle <" + colHandle + "> was not found");
								 throw new FeedProcessingException();
							 } else if (! (obj instanceof Collection)){
								 logger.error("Supplied feed collection does not resolve to a DSpace collection: " + colHandle);
								 throw new FeedProcessingException();
							 } else {
								 logger.debug("Found feed collection: " + colHandle);
								 collections = new Collection[] {(Collection)obj};
							 }
							 
							 // Update the URL to remove the collection info
							 try{
								 updatedUrl = FEED_PREFIX + url.substring(atPos + 1);
								 BundleUtils.setFirstUrlInUrlBundle(context, item, url, updatedUrl);
							 } catch (IndexOutOfBoundsException ie){
								 // @ char was last char in URL!
								 logger.warn("Feed url doesn't contain any chars after @ symbol: " + url);
								 throw new FeedProcessingException();
							 }
						 } else {
							 // Used the default collection the user selected
							 collections = new Collection[] {(Collection)(HandleManager.resolveToObject(context, colHandle))};
							 updatedUrl = url;
						 }
						 
						 // Write the default collection handle and url to bitstreams in the feed bundle
						 BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
						 // set the URL as the primary bitstream
						 Bitstream feedUrlBitstream = BundleUtils.setBitstreamFromBytes(feedBundle, Constants.FEED_BUNDLE_URL_BITSTREAM_NAME, bs_format, updatedUrl.getBytes(), true);
						 BundleUtils.setBitstreamFromBytes(feedBundle, Constants.FEED_BUNDLE_DEF_COL_HANDLE_NAME, bs_format, colHandle.getBytes(), false);
						 
						 // Set forced collection handle if it was supplied
						 if (forcedCollectionHandle != null){
							 BundleUtils.setBitstreamFromBytes(feedBundle, Constants.FEED_BUNDLE_FORCE_COL_HANDLE_NAME, bs_format, forcedCollectionHandle.getBytes(), false);
						 }
						 
						 // Now process using the suitable ingester
						 checkPackageAndProcess(context, subInfo, feedBundle, feedUrlBitstream, request.getSession(), true, false, collections);
						 
					 } else{
						 logger.debug("User submitted a normal non feed url - no more processing required");
					 }
					 
				 } else {
					 // Item has a URL bundle but no url found!
					 throw new FeedProcessingException();
				 }
			 }
			 else {
				 logger.debug("File submitted");
				 
				// Uploaded file should be in a bitstream in the item bundle
				Bundle[] contentBundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
				
				logger.debug("Found " + contentBundles.length + " bundles with name " + Constants.CONTENT_BUNDLE_NAME);
				
				// NOTE: Does it make sense to have multiple ORIGINAL Bundles?? Is the first one only ever used?
				if (contentBundles.length > 0){
					// The code in JorumUploadStep appears to add the content to the first bundle so use this
					Bitstream streams[] = contentBundles[0].getBitstreams();
				
					logger.debug("Found " + streams.length + " bitstreams for first content bundle");
					
					// Only need to do any work if we have any streams!
					if (streams.length > 0){
						// If we are uploading multiple items at once then there would be multiple bitstreams
						if (streams.length > 1){
							multipleFiles = true;
						}
						
						// Cycle through all the streams and process them
						for (Bitstream s : streams){
							// Only copy the metadata to the submission item if we aren't dealing with multiple files
							checkPackageAndProcess(context, subInfo, contentBundles[0], s, request.getSession(), !multipleFiles, true, null);
						}
					}	
				}
			 }
		 } catch (FeedProcessingException f){
			 // Caught exception processing a feed - just return STATUC_COMPLETE and treat as a normal url
			 ExceptionLogger.logException(logger, f);
		 }
		 
		 logger.debug("PackageDetectorStep::doProcessing Leaving with " + result);
		 return result;
	 }
	
	 public int getNumberOfPages(HttpServletRequest request,
	            SubmissionInfo subInfo) throws ServletException{
		 return 1;
	 }
	
}
