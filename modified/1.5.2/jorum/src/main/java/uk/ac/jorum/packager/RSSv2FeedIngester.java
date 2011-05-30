/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : RSSv2FeedIngester.java
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
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.jdom.Element;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.exceptions.CriticalException;
import uk.ac.jorum.exceptions.NonCriticalException;
import uk.ac.jorum.packager.detector.RSSv2FeedDetector;

/**
 * @author gwaller
 *
 */
public class RSSv2FeedIngester extends BaseXmlIngester {

	private static Logger log = Logger.getLogger(RSSv2FeedIngester.class);
	
	private void storeAndXWalkMetadata(RSSv2XmlManifest manifest, Element rootMdElem, Context context, Item item) throws
	CrosswalkException, AuthorizeException, SQLException, IOException, MetadataFormatException, MetadataValidationException{
		/*
		 * crosswalk the metadata
		 */
		log.debug("Attempting to crosswalk the metadata in the manifest");
		List<Element> metadataElems = manifest.getMetadataElements(rootMdElem);
		
		if (metadataElems.size() > 0){
			
			
			// Get the metadata format - throws an exception if not supported
			MetadataFormat format = manifest.getMetadataFormat(rootMdElem);
			
			log.debug("Metadata format determined to be: " + format.getDspaceConfigStr());
			
			log.debug("Getting the IngestionCrosswalk class - using config string <" + format.getDspaceConfigStr() + ">");
			IngestionCrosswalk xwalk = manifest.getCrosswalk(format.getDspaceConfigStr());

	        if (xwalk == null){
	            throw new MetadataValidationException("Cannot process manifest: "+
	                "No crosswalk found in DSpace config for MDTYPE=" + format.getDspaceConfigStr());
	        }
	        
	        try
	        {
	        	log.debug("Calling ingest on " + xwalk.toString() + " to crosswalk the metadata");
	            xwalk.ingest(context, item, metadataElems);
	            
	            // Hack - the title may just be in a <title> elem not in a metadata namesaoce such as <dc:title>
	            DCValue t[] = item.getDC( "title", null, Item.ANY);
	            if (t == null || t.length == 0){
	            	// try and pull it out from a <title> element
	            	Element titleElem = rootMdElem.getChild(RSSv2XmlManifest.RSS_TITLE_ELEM);
	            	if (titleElem != null){
	            		String titleVal = titleElem.getValue();
	            		if (titleVal != null){
	            			item.addDC("title", null, Item.ANY, titleVal.trim());
	            		}
	            		
	            	}
	            }
	            
	        }
	        catch (CrosswalkObjectNotSupported e)
	        {
	            log.warn("Skipping metadata for inappropriate type of object: Object="+item.toString()+", error="+e.toString());
	        }
		}
	}
	
	private void commitWorkspaceItemAndBundleChanges(Item item, WorkspaceItem wi) throws AuthorizeException, SQLException, IOException{
		// commit any changes to bundles
		Bundle allBn[] = item.getBundles();
		for (int i = 0; i < allBn.length; ++i) {
			allBn[i].update();
		}

		wi.update();
	}
	
	
	private WorkspaceItem process(RSSv2XmlManifest manifest,
								  Element rootMdElement, 
								  Context context, 
								  Collection[] collections,
								  boolean failNoLicence,
								  String forcedCCLicenceUrl) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException{
		
		WorkspaceItem wi = WorkspaceItem.create(context, collections[0], false);
		
		Item item = wi.getItem();

		// Populate metadata in this wrapper item
		storeAndXWalkMetadata(manifest, rootMdElement, context, item);
		
		// Sanity-check the resulting metadata on the Item:
		PackageUtils.checkMetadata(item);
		
		// Pull out the "link" elements and store in the URL_BUNDLE
		List<Element> linkElems = rootMdElement.getChildren(RSSv2XmlManifest.RSS_LINK_ELEM);
		if (linkElems != null && linkElems.size() > 0){
			// Get the URL_BUNDLE
			Bundle urlBundle = BundleUtils.getBundleByName(item, Constants.URL_BUNDLE);
			BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
			for (Element link:linkElems){
				String linkValue = link.getValue();
				if (linkValue != null){
					String trimmedLink = linkValue.trim();
					BundleUtils.setBitstreamFromBytes(urlBundle, trimmedLink, bs_format, trimmedLink.getBytes(), true);
				}
				
			}
		}
		
		// Read the licence info from the metadata and store in the bundle
		// GWaller 26/8/09 - use the first Collection as the owner and use its deposit licence
		try{
			addLicense(context, collections[0], item, manifest, rootMdElement, forcedCCLicenceUrl);
		} catch (MetadataValidationException m){
			// If we got this exception then a CC licence wasn't matched using the regex for the metadata standard
			
			// If we have been instructed to fail if no valid licence then throw the exception, otherwise continue
			if (failNoLicence){
				throw m;
			}
		}
		
		commitWorkspaceItemAndBundleChanges(item, wi);
		
		return wi;
	}
	
	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#ingest(org.dspace.core.Context, org.dspace.content.Collection[], java.io.InputStream, org.dspace.content.packager.PackageParameters, java.lang.String)
	 */
	public WorkspaceItem ingest(Context context, Collection[] collections, InputStream in, PackageParameters params,
			String license) throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException {
		WorkspaceItem wi = null;
		boolean success = false;
		boolean failNoLicence = params.getBooleanProperty(FAIL_NO_LICENCE_PARAM, true);
		String forcedCCLicence = params.getProperty(FORCED_CC_LICENCE);
		
		try{	
			// Pass in null as the feedBundle means we don't save the stream in the bundle - this all needs refactored so that the
			// bundle is already supplied in the packager PackageIngester interface! - we wouldn'tneed to get the feed again as the detector 
			// already read it and stored it in th ebundle!
			RSSv2XmlManifest manifest = new RSSv2XmlManifest(RSSv2FeedDetector.getRssDocument(context, in, null));
			
			Element channelElem = manifest.getRootElement().getChild(RSSv2XmlManifest.RSS_CHANNEL_ELEM);
			if (channelElem == null){
				throw new MetadataValidationException("Could not find element " + RSSv2XmlManifest.RSS_CHANNEL_ELEM);
			}
			
			// process the md for the wrapper
			wi = process(manifest, channelElem, context, collections, failNoLicence, forcedCCLicence);
			
			// Store the wrapper in all the collections necessary
            if (collections.length > 1){
            	for (int i = 1; i < collections.length; i++){
            		collections[i].addItem(wi.getItem());
            	}
            }
			
			// Iterate accross all child items and create a new Item, store metadata, web link and install
			List<Element> rssItems = channelElem.getChildren(RSSv2XmlManifest.RSS_ITEM_ELEM);
			for (Element rssItem:rssItems){
				WorkspaceItem rssWorkspaceItem = process(manifest, rssItem, context, collections, failNoLicence, forcedCCLicence);
				
				// Mark installed child item as related to this wrapper item
				Item relatedItem = InstallItem.installItem(context, rssWorkspaceItem);
				
				// Add the handle to the related bundle
				Bundle b = BundleUtils.getBundleByName(wi.getItem(), Constants.RELATED_CONTENT_PACKAGE_BUNDLE);
				BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
				
				BundleUtils.setBitstreamFromBytes(b, relatedItem.getHandle(), bs_format, relatedItem.getHandle().getBytes(), true);
			
	            if (collections.length > 1){
	            	for (int i = 1; i < collections.length; i++){
	            		collections[i].addItem(rssWorkspaceItem.getItem());
	            	}
	            }
			}
			

			success = true;
			
			log.info(LogManager.getHeader(context, "ingest", "Created new Item, db ID=" + String.valueOf(wi.getItem().getID())
					+ ", WorkspaceItem ID=" + String.valueOf(wi.getID())));
			
			return wi;
			
		} catch (SQLException se) {
			// disable attempt to delete the workspace object, since
			// database may have suffered a fatal error and the
			// transaction rollback will get rid of it anyway.
			wi = null;

			// Pass this exception on to the next handler.
			throw se;
		} finally {
			// kill item (which also deletes bundles, bitstreams) if ingest
			// fails
			if (!success && wi != null)
				wi.deleteAll();
		}
		
		
	}

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#postInstallHook(org.dspace.core.Context, org.dspace.content.Item)
	 */
	public void postInstallHook(Context context, Item item) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#replace(org.dspace.core.Context, org.dspace.content.Item, java.io.InputStream, org.dspace.content.packager.PackageParameters)
	 */
	public Item replace(Context context, Item item, InputStream in, PackageParameters params) throws PackageException,
			UnsupportedOperationException, CrosswalkException, AuthorizeException, SQLException, IOException {
		throw new UnsupportedOperationException("The replace operation is not implemented.");
	}

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#setLicenceInPackage(org.dspace.core.Context, org.dspace.content.Item, java.lang.String, java.lang.String)
	 */
	public void setLicenceInPackage(Context context, Item item, String licenceUrl, String licenceName) throws SQLException, 
	  IOException, 
	  AuthorizeException, 
	  MetadataValidationException {
		/* !!! NOTE:
		 
		 The current implementation of the RSS ingester simply creates web resource links. As it won't create any 
		 packages which could hold licence information, we don't need to do anythhing here. If the implmentation however was
		 changed to support downloading of the end resource in a feed link e.g. potentially an IMS CP, the 
		 related items would have to be iterated across and the licence set in those packages.
		*/
	}


	/** 
	 *  The current implementation of the RSS ingester simply creates web resource links. As it won't create any 
	 *	packages which could hold licence information, we don't need to do anythhing here. If the implmentation however was
	 *	changed to support downloading of the end resource in a feed link e.g. potentially an IMS CP, the 
	 *	related items would have to be iterated across and the licence set in those packages.
	 *
	 * @see org.dspace.content.packager.PackageIngester#updateLicenceInfoInManifest(org.dspace.core.Context, org.dspace.content.Item, org.dspace.content.Bitstream, java.io.InputStream, boolean, java.lang.String, java.lang.String)
	 */
	public boolean updateLicenceInfoInManifest(Context context, Item item, Bitstream bitstreamContainingManifest,
			InputStream manifestStream, boolean backupBitstream, String licenceUrl, String licenceName)
			throws SQLException, IOException, AuthorizeException, MetadataValidationException, CriticalException {
		/* !!! NOTE:
		 
		 The current implementation of the RSS ingester simply creates web resource links. As it won't create any 
		 packages which could hold licence information, we don't need to do anythhing here. If the implmentation however was
		 changed to support downloading of the end resource in a feed link e.g. potentially an IMS CP, the 
		 related items would have to be iterated across and the licence set in those packages.
		*/
		return false;
	}

	/**
	 *  The current implementation of the RSS ingester simply creates web resource links. As it won't create any 
	 *	packages which could hold licence information, we don't need to do anythhing here. If the implmentation however was
	 *	changed to support downloading of the end resource in a feed link e.g. potentially an IMS CP, the 
	 *	related items would have to be iterated across and the licence set in those packages.
	 *
	 * @see org.dspace.content.packager.PackageIngester#updateEmbeddedLicence(org.dspace.core.Context, org.dspace.content.Item)
	 */
	public void updateEmbeddedLicence(Context context, Item item) throws NonCriticalException, CriticalException {
		/* !!! NOTE:
		 
		 The current implementation of the RSS ingester simply creates web resource links. As it won't create any 
		 packages which could hold licence information, we don't need to do anythhing here. If the implmentation however was
		 changed to support downloading of the end resource in a feed link e.g. potentially an IMS CP, the 
		 related items would have to be iterated across and the licence set in those packages.
		*/
		
	}

	
	
}
