/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : IMSIngester.java
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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.rmi.dgc.VMID;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.Item;
import org.jdom.Element;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.exceptions.CriticalException;
import uk.ac.jorum.exceptions.NonCriticalException;
import uk.ac.jorum.licence.LicenceController;
import uk.ac.jorum.packager.preview.IMSHtmlPreviewGenerator;
import uk.ac.jorum.utils.ExceptionLogger;
import uk.ac.jorum.utils.URLChecker;

/**
 * @author gwaller
 * 
 */
public class IMSIngester extends BaseXmlIngester {

	/** log4j category */
	private static Logger log = Logger.getLogger(IMSIngester.class);

	/** Filename of manifest, relative to package toplevel. */
	public static final String MANIFEST_FILE = "imsmanifest.xml";
	
	// bitstream format name of IMS SIP format..
	private static final String MANIFEST_BITSTREAM_FORMAT = "DSpace IMS SIP";

	private static final String IMS_PRESERVE_CONFIG_KEY = "ims.submission.preserveManifest";
	
	private static final boolean preserveManifest = ConfigurationManager.getBooleanProperty(
			IMS_PRESERVE_CONFIG_KEY, false);
	
	private static final String IMS_FAIL_EXTRA_CONFIG_KEY = "ims.submission.failIfExtraFilesInZip";
	
	private static final boolean failExtra = ConfigurationManager.getBooleanProperty(
			IMS_FAIL_EXTRA_CONFIG_KEY, false);
	
	private static final String IMS_FAIL_MISSING_CONFIG_KEY = "ims.submission.failIfMissingFiles";
	
	private static final boolean failMissing = ConfigurationManager.getBooleanProperty(
			IMS_FAIL_MISSING_CONFIG_KEY, false);


	private static final String DATE_FORMAT = "dd-MM-yyyy_HH:mm:ss";
	public static final String MANIFEST_BACKUP_STR = "_original_jorum_backup_";

    
	
	
	protected XMLManifest createManifest(InputStream is, boolean validate) throws IOException, MetadataValidationException{
		return IMSManifest.create(is, validate);
	}
	
	
	/**
	 * Assets either appear in nested file elements or if no child found simply in the href attribute
	 * @param resourceElement
	 * @return
	 */
	protected String[] getAssetListFromResource(Element resourceElement){
		List fileElements = resourceElement.getChildren(XMLManifest.FILE_ELEM, resourceElement.getNamespace());
		ArrayList<String> result = new ArrayList<String>(10); 
		
		if (fileElements.size() > 0){
			// Pull out the href from each element
			for (int i = 0; i < fileElements.size(); i++){
				Object f = fileElements.get(i);
				if (f instanceof Element){
					String href = ((Element)f).getAttributeValue(XMLManifest.HREF_ATTR);
					if (href != null && href.length() > 0){
						// GWaller 29/10/09 Bug#118 - hrefs must be URL decoded
						try{
							result.add(URLDecoder.decode(href, "UTF-8"));
						} catch (UnsupportedEncodingException e){
							ExceptionLogger.logException(log, e);
						}
					}
				}
			}
		} else {
			// GWaller 29/10/09 Bug#118 - hrefs must be URL decoded
			String href = resourceElement.getAttributeValue(XMLManifest.HREF_ATTR);
			try{
				result.add(URLDecoder.decode(href, "UTF-8"));
			} catch (UnsupportedEncodingException e){
				ExceptionLogger.logException(log, e);
			}
		}
		
		return result.toArray(new String[result.size()]);
	}
	
	
	
	/**
	 * This method will find web links in the manifest and add them as bitstreams to the URL bundle of the item.
	 * If the manifest contains resource elements which with have web links or point to physical files in the package,
	 * the metadata of the manifest is *not* scanned for web links. 
	 * @param context
	 * @param item
	 * @param manifest
	 * @param manifestResourceElements
	 * @param maxMetadataWebLinksToInclude set to a negative value to include all web links found in metadata
	 * @return The list of resource elements which do not contain web links but instead point to physical files. 
	 * @throws MetadataValidationException
	 * @throws PackageValidationException
	 * @throws AuthorizeException
	 * @throws IOException
	 * @throws SQLException
	 */
	private List<Element> findAndProcessWebLinks(Context context,
										Item item,
										XMLManifest manifest,
										List<Element> manifestResourceElements,
										int maxMetadataWebLinksToInclude) throws MetadataValidationException,
																						PackageValidationException,
																						AuthorizeException,
																						IOException,
																						SQLException{
		ArrayList<Element> nonWebLinkResourceElements = new ArrayList<Element>();
		ArrayList<String> webLinksFound = new ArrayList<String>();
		
		// First process all the resource elements from the manifest and pull out web links
		for (Iterator<Element> mi = manifestResourceElements.iterator(); mi.hasNext();) {
			
			Element mfile = (Element) mi.next();
			/*
			 * We now need to get a list of either filepaths or urls from this resource elem.
			 * Asset paths/urls can appear in nested file elements under the resource or 
			 * as a single href attribute value
			 */
			String[] assets = getAssetListFromResource(mfile);
			
			for (int i = 0; i < assets.length; i++){
				// Pull out the location of the file (or could be a full web link!)
				String pathOrURL = assets[i];
				if (pathOrURL == null){
					throw new PackageValidationException("Invalid IMS Manifest: " + 
														  XMLManifest.RESOURCE_ELEM + 
														  " element without " + 
														  XMLManifest.HREF_ATTR +
														  " attribute.");
				}
				
				String trimmedURL = pathOrURL.trim();
				int urlSchemeLen = URLChecker.isURL(trimmedURL);
				
				// If a URL scheme exists e.g. http:// then we have a link
				if (urlSchemeLen > 0){
					// Found a url - add it to the list to process
					webLinksFound.add(trimmedURL);
				} else {
					// No URL scheme so should be processed as a physical file later
					// Add this to the list of elements which should be processed by the caller
				    // CG Added check here to stop duplicate elements being added - IssueID #554
				    if(!nonWebLinkResourceElements.contains(mfile)){
				        nonWebLinkResourceElements.add(mfile);
				    } 

				}
			}
			
		}
		
		// Check to see if we found any links or we detected files to process - the metadata shoudl only be scanned if we didn't find anything!
		if (webLinksFound.size() == 0 && nonWebLinkResourceElements.size() == 0){
			// Scan the metadata for possible web links
			try{
				String[] webLinksFromMetadata = manifest.getMetadataFormat().geWebLinksFromMetadata(manifest.getManifestDocument());
				int numAdded = 0;
				for (String link:webLinksFromMetadata){
					if (maxMetadataWebLinksToInclude < 0 || numAdded < maxMetadataWebLinksToInclude){
						webLinksFound.add(link);
						numAdded++;
					}
				}
			} catch (MetadataFormatException e){
				// this will be thrown if we can't determien the metadata format. We can't therefore find any links.
				log.warn("Could not determine metadata format from manifest when attempting to find possible web link identifiers in metadata");
			}
		}
		
		
		// Now add the web links to the web resource bundle
		for (String link:webLinksFound){
			// Got a URL - must create a bitstream in the bundle for it - remember this 
			// won't be in the ZIP as a file so no bitstream created yet
			BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
			// Don't use the contentBundle - get the URL budle so the URL is rendered correctly in the XMLUI
			BundleUtils.setBitstreamFromBytes(BundleUtils.getBundleByName(item, Constants.URL_BUNDLE), link, bs_format, link.getBytes(), false);
		}
		
		
		return nonWebLinkResourceElements;
		
	}
	
	
    // GWaller 26/08/09 Modified to support array of Collections
	/**
	 *  
	 * @see org.dspace.content.packager.PackageIngester#ingest(org.dspace.core.Context, org.dspace.content.Collection[], java.io.InputStream, org.dspace.content.packager.PackageParameters, java.lang.String)
	 */
	public WorkspaceItem ingest(Context context, 
							    Collection[] collections, 
							    InputStream in, 
							    PackageParameters params,
							    String license) throws PackageException, 
							    					   CrosswalkException, 
							    					   AuthorizeException, 
							    					   SQLException, 
							    					   IOException {
		ZipInputStream zip = new ZipInputStream(in);
		//HashMap<String, Bitstream> fileIdToBitstream = new HashMap<String, Bitstream>();
		WorkspaceItem wi = null;
		boolean success = false;
		HashMap<String, Boolean> packageFiles = new HashMap<String, Boolean>();
		HashSet<String> schemaFiles = new HashSet<String>();

		boolean validate = params.getBooleanProperty(VALIDATE_PARAM, true);
		boolean failNoLicence = params.getBooleanProperty(FAIL_NO_LICENCE_PARAM, true);
		String forcedCCLicence = params.getProperty(FORCED_CC_LICENCE);

		// GWaller 6/5/10 IssueID#263 Support for web links not in a manifest resource element
		String maxMetadataWebLinksToIncludeStr = params.getProperty(MAX_METADATA_WEB_LINKS_TO_USE);
		int maxMetadataWebLinksToInclude = 1; // default to 1 to only pick out the first link found
		if (maxMetadataWebLinksToIncludeStr != null){
			try{
				maxMetadataWebLinksToInclude = Integer.parseInt(maxMetadataWebLinksToIncludeStr);
			} catch (NumberFormatException e){
				// non-valid integer, do nothing and use the default
			}
			
		}
		
		try {
			/*
			 * 1. Read all the files in the Zip into bitstreams first, because
			 * we only get to take one pass through a Zip input stream. Give
			 * them temporary bitstream names corresponding to the same names
			 * they had in the Zip
			 */
			XMLManifest manifest = null;
			MetadataFormat format;
			
			// GWaller 26/08/09 Modified to support array of Collections - use first collection as owner
			wi = WorkspaceItem.create(context, collections[0], false);
			Item item = wi.getItem();
			Bundle contentBundle = item.createBundle(Constants.CONTENT_BUNDLE_NAME);
			Bundle mdBundle = item.createBundle(Constants.METADATA_BUNDLE_NAME);
			ZipEntry ze;
			while ((ze = zip.getNextEntry()) != null) {
				if (ze.isDirectory())
					continue;
				Bitstream bs = null;
				String fname = ze.getName();
				
				log.debug("Processing entry in zip: " + fname);
				
				if (fname.equals(MANIFEST_FILE)) {
					
					log.debug("Found IMS manifest: " + fname);
					
					if (preserveManifest) {
						
						log.debug("Preserving manifest in bitstream" + fname);
						
						// GWaller 3/2/10 IssueID #175 Use new archiveManifest method 
						bs = archiveManifest(context, item, mdBundle, new PackageUtils.UnclosableInputStream(zip), fname);
						
						manifest = createManifest(bs.retrieve(), validate);
					} else {
						
						log.debug(IMS_PRESERVE_CONFIG_KEY + " not set in config - manifest won't be saved in a bitstream");
						
						manifest = createManifest(new PackageUtils.UnclosableInputStream(zip), validate);
						continue;
					}
				} else {
					log.debug("Creating bitstream from zip entry " + fname);
					bs = contentBundle.createBitstream(new PackageUtils.UnclosableInputStream(zip));
					bs.setSource(fname);
					bs.setName(fname);
				}
				
				// An IMS package can contain XSD or DTDs used for manifest in teh zip at the top level 
				// (see http://www.imsglobal.org/content/packaging/cpv1p1p3/imscp_bestv1p1p3.html Section 6.1.1)
				// "The Package must contain any directly referenced controlling files used DTD, XSD) in the root of the distribution medium (archive file, CD-ROM, etc.)"
				
				// therefore only add the file to packageFiles if it doesn't have a xsd or dtd extension and is at the top level
				int extensionDot = fname.lastIndexOf(".");
				if (extensionDot != -1){
					// Check top level - if contains slash then it must be the last char
					// See http://www.pkware.com/documents/casestudies/APPNOTE.TXT for ZIP spec (all slashes are forward slashes)
					int slashIndex = fname.indexOf("/");
					boolean topLevel = false;
					if (slashIndex == -1 || (slashIndex == fname.length() - 1)){
						// at top level
						topLevel = true;
					}
					
					// check XSD
					if (fname.regionMatches(false, extensionDot, ".xsd", 0, 4) && topLevel){
						log.debug("Adding entry (read from zip) to schemaFiles hash: <" + fname + ">");
						schemaFiles.add(fname);
					// check DTD	
					} else if (fname.regionMatches(false, extensionDot, ".dtd", 0, 4) && topLevel){
						log.debug("Adding entry (read from zip) to schemaFiles hash: <" + fname + ">");
						schemaFiles.add(fname);
					} else {
						// either didn't match extension or the file wasn't at the root or both!
						log.debug("Adding entry (read from zip) to packageFiles hash: <" + fname + ">");
						packageFiles.put(fname, new Boolean(false));
					}
				} else {
					// No dot found - just add to packageFiles hash
					log.debug("Adding entry (read from zip) to packageFiles hash: <" + fname + ">");
					packageFiles.put(fname, new Boolean(false));
				}
				
				
				bs.setSource(fname);
				bs.update();
			}
			zip.close();

			if (manifest == null){
				throw new PackageValidationException("No IMS Manifest found (filename=" + MANIFEST_FILE
						+ ").  Package is unacceptable.");
			}
			
			/*
			 * 2. Grovel a file list out of IMS Manifest and compare it to the
			 * files in package, as an integrity test.
			 */
			List<Element> manifestContentFiles = manifest.getResources();

			
			// GWaller 6/5/10 IssueID#263 Support for web links not in a manifest resource element
			// Web links are now processed before files - this makes the code a lot simpler to follow
			List<Element> resourceFileElements = findAndProcessWebLinks(context, item, manifest, manifestContentFiles, maxMetadataWebLinksToInclude);
			
			// Use a hashset to store names of detected missing files i.e. file referenced in the manifest but not in the zip
			HashSet<String> missingFiles = new HashSet<String>();
			
			// Compare manifest files with the ones found in package
			for (Iterator<Element> mi = resourceFileElements.iterator(); mi.hasNext();) {
				// First locate corresponding Bitstream and make
				// map of Bitstream to <file> ID.
				Element mfile = (Element) mi.next();
				String mfileId = mfile.getAttributeValue(XMLManifest.IDENTIFIER_ATTR);
				if (mfileId == null)
					throw new PackageValidationException("Invalid IMS Manifest: " + 
														  XMLManifest.RESOURCE_ELEM + 
														  " element without " + 
														  XMLManifest.IDENTIFIER_ATTR +
														  " attribute.");
				
				/*
				 * We now need to get a list of either filepaths from this resource elem.
				 * Asset paths can appear in nested file elements under the resource or 
				 * as a single href attribute value
				 */
				String[] assets = getAssetListFromResource(mfile);
				
				for (int i = 0; i < assets.length; i++){
					// Pull out the location of the file (remember web links have already been processed - this must be a reference to a physical file)
					String path = assets[i];
					if (path == null){
						throw new PackageValidationException("Invalid IMS Manifest: " + 
															  XMLManifest.RESOURCE_ELEM + 
															  " element without " + 
															  XMLManifest.HREF_ATTR +
															  " attribute.");
					}
					
					String trimmedPath = path.trim();
					
					Bitstream bs = contentBundle.getBitstreamByName(trimmedPath);
					if (bs == null) {
						log.warn("Cannot find bitstream for filename=\"" + trimmedPath
								+ "\", skipping it..may cause problems later.");
						missingFiles.add(trimmedPath);
					} else {
						//fileIdToBitstream.put(mfileId, bs);

						/*
						
						GWaller 6/10/09 
						Commenting out code which changed the bitstream name to the 
						actual file name i.e. removed path information. This was legacy code
						copied from the METS package ingester - surely its better to keep the 
						full path as the name for rebuilding the package later????
						
						// Now that we're done using Name to match to <file>,
						// set default bitstream Name to last path element;
						// Zip entries all have '/' pathname separators
						// NOTE: set default here, hopefully crosswalk of
						// a bitstream techMD section will override it.
						String fname = bs.getName();
						int lastSlash = fname.lastIndexOf('/');
						if (lastSlash >= 0 && lastSlash + 1 < fname.length()){
							bs.setName(fname.substring(lastSlash + 1));
						}
						*/

						// TODO: Better logic to get MIME type - use mime-util
						// Set Default bitstream format - guess from extension
						BitstreamFormat bf = FormatIdentifier.guessFormat(context, bs);
						bs.setFormat(bf);

						// finally, build compare lists by deleting matches.
				
						log.debug("Checking for entry in packageFiles hash: <" + trimmedPath + ">");
						if (packageFiles.get(trimmedPath) != null){
							log.debug("Entry found - setting seen boolean to true");
							packageFiles.put(trimmedPath, new Boolean(true));
						} else {
							log.debug("Entry missing - adding to missingFiles hash: <" + trimmedPath + ">");
							missingFiles.add(trimmedPath);
						}
						
					}
				}
			}

			// Make sure Manifest file doesn't get flagged as missing
			// or extra, since it won't be mentioned in the manifest.
			if (packageFiles.get(MANIFEST_FILE) != null){
				packageFiles.remove(MANIFEST_FILE);
			}
			
			// Any discrepency in file lists is a fatal error:
			HashSet<String> extraFiles = new HashSet<String>();
			Set<String> keys = packageFiles.keySet();
			Iterator<String> keyIter = keys.iterator();
			while (keyIter.hasNext()){
				String key = keyIter.next();
				Boolean value = packageFiles.get(key);
				if (value != null && value == false){
					extraFiles.add(key);
				}
			}
			
			
			if ((failExtra || failMissing) && !(extraFiles.isEmpty() && missingFiles.isEmpty())) {
				StringBuffer msg = new StringBuffer("Package is unacceptable: contents do not match manifest.");
				if (!missingFiles.isEmpty()) {
					msg.append("\nPackage is missing these files listed in Manifest:");
					for (Iterator mi = missingFiles.iterator(); mi.hasNext();)
						msg.append("\n\t" + (String) mi.next());
					
					if (failMissing){
						throw new PackageValidationException(msg.toString());
					}
				}
				if (!extraFiles.isEmpty()) {
					msg.append("\nPackage contains extra files NOT in manifest:");
					for (Iterator mi = extraFiles.iterator(); mi.hasNext();)
						msg.append("\n\t" + (String) mi.next());
					
					if (failExtra){
						throw new PackageValidationException(msg.toString());
					}
				}
				
			}

			/*
			 * 3. crosswalk the metadata
			 */
			log.debug("Attempting to crosswalk the metadata in the manifest");
			List<Element> metadataElems = manifest.getMetadataElements();
			log.debug("Number of metadata elements returned is: " + metadataElems.size());
			if (metadataElems.size() > 0){
				log.debug("Attempting to determing metadata format from element:" + metadataElems.get(0).toString());
				
				// Get the metadata format - throws an exception if not supported
				format = manifest.getMetadataFormat();
				
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
		        }
		        catch (CrosswalkObjectNotSupported e)
		        {
		            log.warn("Skipping metadata for inappropriate type of object: Object="+item.toString()+", error="+e.toString());
		        }
			} else {
				// Now metadata found - throw an exception
				throw new MetadataValidationException("Zero metadata elements found");
			}

			// GWaller 11/1/10 IssueID #159 Add metadata specified on in params
			Enumeration paramKeys = params.keys();
			while (paramKeys.hasMoreElements()){
				Object keyObj = paramKeys.nextElement();
				if (keyObj instanceof String){
					// Check begins with "dc."
					String key = (String)keyObj;
					if (key.startsWith("dc.")){
						// split the string
						String[] parts = key.split("\\."); // Rem need to escape the '.' as split takes a regex
						if (parts.length == 2){
							item.addDC(parts[1], null, "en", params.getProperty(key));
						} else if (parts.length == 3){
							item.addDC(parts[1], parts[2], "en", params.getProperty(key));
						}
					}
				}
			}
			
			
			// Sanity-check the resulting metadata on the Item:
			PackageUtils.checkMetadata(item);

			// TODO: Need correct way of assigning primary bitstream - a scorm package can have multipel files mapped 
			//       to the same resource identifier attribute so we can't use the fileIdToBitstream hasmap
			/*
			 * 4. Set primary bitstream - use the first file listed in manifest
			 */
			/*if (manifestContentFiles.size() > 0){
				Element pbsFile = manifestContentFiles.get(0);
				if (pbsFile != null) {
					Bitstream pbs = (Bitstream) fileIdToBitstream.get(pbsFile.getAttributeValue(IMSManifest.IDENTIFIER_ATTR));
					if (pbs == null)
						log.error("Got Primary Bitstream file ID=" + pbsFile.getAttributeValue(IMSManifest.IDENTIFIER_ATTR)
								+ ", but found no corresponding bitstream.");
					else {
						Bundle bn[] = pbs.getBundles();
						if (bn.length > 0)
							bn[0].setPrimaryBitstreamID(pbs.getID());
						else
							log.error("Sanity check, got primary bitstream without any parent bundle.");
					}
				}
			}*/
			

			// Read the licence info from the metadata and store in the bundle
			// GWaller 26/8/09 - use the first Collection as the owner and use its deposit licence
			try{
				addLicense(context, collections[0], item, manifest, null, forcedCCLicence);
			} catch (Exception m){
				// If we got this exception then a CC licence wasn't matched using the regex for the metadata standard
				
				// If we have been instructed to fail if no valid licence then throw the exception, otherwise continue
				if (failNoLicence){
                    if (m instanceof MetadataFormatException)
                    {
                        throw (MetadataFormatException)m;
                    }
                    else if (m instanceof AuthorizeException)
                    {
                        throw (AuthorizeException)m;
                    }
                    else if (m instanceof MetadataValidationException)
                    {
                        throw (MetadataValidationException)m;
                    }
					else if (m instanceof SQLException)
                    {
                        throw (SQLException)m;
                    }
                    else if (m instanceof IOException)
                    {
                        throw (IOException)m;
                    }

				}
			}

			// commit any changes to bundles
			Bundle allBn[] = item.getBundles();
			for (int i = 0; i < allBn.length; ++i) {
				allBn[i].update();
			}

			// Now classify the item
			classifyItem(context,
						 format.geClassificationText(manifest.getManifestDocument()), 
						 collections, 
						 wi,
						 params.getBooleanProperty(ALTER_OWNING_COL_PARAM, false));
			
			
			// Update the WorkspaceItem so the changes are written to the db
			wi.update();
			
			// Everything went well!
			success = true;
			
			log.info(LogManager.getHeader(context, "ingest", "Created new Item, db ID=" + String.valueOf(item.getID())
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


	// TODO: IMS CP replace not implemented yet
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dspace.content.packager.PackageIngester#replace(org.dspace.core.Context
	 * , org.dspace.content.Item, java.io.InputStream,
	 * org.dspace.content.packager.PackageParameters)
	 */
	public Item replace(Context context, Item item, InputStream in, PackageParameters params) throws PackageException,
			UnsupportedOperationException, CrosswalkException, AuthorizeException, SQLException, IOException {
		throw new UnsupportedOperationException("The replace operation is not implemented.");
	}


	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.BaseXmlIngester#postInstallHook(org.dspace.core.Context, org.dspace.content.Item)
	 */
	public void postInstallHook(Context context, Item item) throws NonCriticalException, CriticalException{

		// Generate an IMS preview bundle and bitstream
		Bundle previewBundle = null;
		try{
			previewBundle = BundleUtils.getBundleByName(item, Constants.PREVIEW_PACKAGE_BUNDLE);
			IMSHtmlPreviewGenerator.generatePreviewBitstream(context, 
															item, 
															previewBundle);
		} catch (Exception e){
			ExceptionLogger.logException(log, e);
			
			// Remove the preview bundle
			if (previewBundle != null){
				try{
					item.removeBundle(previewBundle);
				} catch (Exception e2){
					ExceptionLogger.logException(log, e2);
				}
			}
		}
		
		// GWaller 16/2/10 IssueID #175 Now ensure the embedded licence is set to the same CC licence store in the CC bundle
		updateEmbeddedLicence(context, item);
		
	}


	// START GWaller 02/02/09 IssueID #175 Added methods to deal with licence manipulation inside packages
	
	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#updateEmbeddedLicence(org.dspace.core.Context, org.dspace.content.Item)
	 */
	public void updateEmbeddedLicence(Context context, Item item) throws NonCriticalException, CriticalException {
		boolean manifestChanged = true;
		Bitstream existingManifestBitStream = null;
		String licence_url = null;
		String licence_name = null;
		try{
			licence_url = LicenceController.getLicenseURL(item);
			licence_name = LicenceController.licenceNameMappedToUrl(licence_url);
			
			// Need to check for null licence - this can happen if the item doesn't have a CC licence!
			if (licence_url == null || licence_name == null){
				log.debug("Null CC licence info found - cannot update embedded licence, returning");
				return;
			}
			
			/* The licence information can be in 2 places for an IMS package item:
			 
			   1. The imsmanifest.xml file which is stored in the "METADATA" bundle if the config "ims.submission.preserveManifest" is set to true
			   2. The imsmanifest.xml file which is stored in the Zip archive which was deposited. This should be in the "ARCHIVED_CP" bundle
			   
			   In order to change the licence information in the Zip archive, the entire archive needs rebuilt.
			   
			   The best solution is to modify the imsmanifest.xml in the "METADATA" bundle first (if it exists) and then use this for the new Zip archive.
			 */
			existingManifestBitStream = PackageUtils.getBitstreamByName(item, MANIFEST_FILE, Constants.METADATA_BUNDLE_NAME);
			if (existingManifestBitStream != null){
				manifestChanged = updateLicenceInfoInManifest(context, 
															  item, 
															  existingManifestBitStream, 
															  existingManifestBitStream.retrieve(), 
															  true, 
															  licence_url, 
															  licence_name);
			}
			
			
			
		} catch (Exception e){
			// We got an exception trying to ensure the licence info in the package matched the one in the DSpace bundle.
			// This is potentially a critical fault - cannot have an item live with a licence mismatch.
			// Throw a critical exception
			throw new CriticalException("Error ensuring manifest licence matched licence in bundle", e);
		}
		
		if (!manifestChanged){
			// We found the manifest in the METADATA bundle and it was fine - no need to alter the archived package
			log.debug("postInstallHook: Manifest file found in METADATA bundle and licence info was correct");
		} else {
			// Manifest was either changed or we didn't find one in the metadata bub=ndle and still need to check Zip
			InputStream manifestStream = null;
			
			try{
				// Check if we found the manifest in the bundle
				if (existingManifestBitStream != null){
					// We found the manifest in the METADATA bundle but had to make alterations - need to rebuild the zip
					// Reset the pointer to point to the new manifest
					existingManifestBitStream = getManifestBitstreamFromItem(item);
					if (existingManifestBitStream != null){
						manifestStream = existingManifestBitStream.retrieve();
	
					} else {
						// could not find the new manifest!! Throw an exeption
						throw new CriticalException("Could not find modified manifest to write to Zip");
					}
				} else {
					// We still need to check the manifest within the zip and make alterations as necessary, possibly rebuilding the Zip
					Bundle archivedPackageBundles[] = item.getBundles(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
					if (archivedPackageBundles != null && archivedPackageBundles.length > 0){
						// Return the first bitstream in the first archived bundle
						Bitstream streams[] = archivedPackageBundles[0].getBitstreams();
						if (streams != null && streams.length > 0){
							InputStream in = streams[0].retrieve();
							try{
								manifestStream = getManifestStreamfromPackageZip(new ZipInputStream(in));
								if (manifestStream == null){
									throw new CriticalException("Cannot find manifest in Zip arhcive");
								}
								manifestChanged = updateLicenceInfoInManifest(context, 
										  item, 
										  null, 
										  manifestStream, 
										  false, 
										  licence_url, 
										  licence_name);
								if (! manifestChanged){
									// the manifest in the Zip was fine - no need to do anything more
									log.debug("Manifest in zip archive was ok - do not need to change licence info");
								} else {
									log.debug("Manifest in zip archive was incorrect - rebuilding zip with new manifest");
									
									// reset the manifestStream pointer to the newly created manifest
									existingManifestBitStream = getManifestBitstreamFromItem(item);
									if (existingManifestBitStream != null){
										manifestStream = existingManifestBitStream.retrieve();
					
									} else {
										// could not find the new manifest!! Throw an exeption
										throw new CriticalException("Could not find modified manifest to write to Zip");
									}
									
								}
								
							} finally{
								try {in.close();} catch (Exception x){}
							}
						} else {
							throw new CriticalException("Bundle " + Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE + " does not contain any bitstreams!");
						}
					} else {
						throw new CriticalException("Bundle " + Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE + " not found");
					}
				}
				
				// At this point we should have a valid pointer to the manifestStream to include in the new Zip
				if (manifestChanged){ // need to check again as we may have examined the archived Zip and the manifest was infact fine
					if (manifestStream == null){
						// Shouldn't happen! - throw an exception
						throw new CriticalException("Manifest stream is null but we have to rebuild the Zip");
					} else {
						// Need to backup the orig ARCHIVED_CP bundle
						Bundle renamedBundle = BundleUtils.renameBundle(item, 
																		Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE,
																		Constants.BACKUP_CONTENT_PACKAGE_BUNDLE
																		);
						// Create ZipInputStream for the original archive in the orig ARCHIVED_CP bundle
						Bitstream origPackageBitStream = BundleUtils.getFirstBitStream(renamedBundle);
						if (origPackageBitStream == null){
							// Error, we couldn't find the bitstream for the orig archive to copy from !
							// NOTE: at this stage we have already renamed the ARCHIVED_CP bundle but if it didin't contain the original Zip anyway, 
							//       its a good thing that it is renamed so it won't let a use attempt to download the package via the GUI !
							throw new CriticalException("Error: Attempting to rebuild Zip from original submitted but the original cannot be found!");
						}
						
						// Now create the ZipInputStream
						ZipInputStream in = new ZipInputStream(origPackageBitStream.retrieve());
						
						// Create a new ARCHIVED_CP bundle to hold the new Zip with the fixed manifest
						Bundle archivedCPBundle = BundleUtils.getBundleByName(item, Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
						
						// Create a ZipOutputStream for the newly created zip
						// Need to create physical file in the export dir first and use this as the outputstream
						// NOTE: passing in the VMKID as the prefix in an attempt to guarantee the file is unique across all JVMs (remember this code
						//       can be executed at the same time from the web interface (by multiple people) and from the command line
						File exportDir = new File(ItemExport.getExportDownloadDirectory(context.getCurrentUser().getID()));
						if (!exportDir.exists()){
							boolean success = exportDir.mkdirs();
							if (! success){
								// Use the system temp dir!
								exportDir = null;
							}
						}
						File outputZipFile = File.createTempFile(new VMID().toString(), ".zip", exportDir);
						ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZipFile));
						
						try{
							IMSDisseminator.copyPackageOverrideManifest(manifestStream, in, out);
							in.close();
							in = null;
							out.close();
							out = null;
							
							// Now we have created the new zip file on disk - need to create a bitstream for it!
							Bitstream zipStream = archivedCPBundle.createBitstream(new FileInputStream(outputZipFile));
							// Now set the name and sequence number!
							zipStream.setName(origPackageBitStream.getName());
							// The sequence number must be the same as the original zip bitstream otherwise the "Download as Original Package" button will 
							// serve up the old Zip instead of the new one! (the link uses the sequence number).
							int currentHighestSeqNum = BundleUtils.highestSequenceNumInBundles(item);
							zipStream.setSequenceID(origPackageBitStream.getSequenceID());
							// IMPORTANT - need to resequence the original zip stream now!
							origPackageBitStream.setSequenceID(currentHighestSeqNum + 1);
							origPackageBitStream.update();
							// Finally update the new stream
							zipStream.update();
							
						} finally {
							try {
								if (in != null) in.close();
								if (out != null) out.close();
								// Delete the temp file
								outputZipFile.delete();
							} catch (Exception x){}
						}
					}
				}
					
				
				
			} catch (Exception e){
				throw new CriticalException("Error updating manifest in Zip archive", e);
			} finally {
				if (manifestStream != null){
					try {manifestStream.close();} catch (Exception x){}
				}
			}
		}		
	}
	
	
	private Bitstream getManifestBitstreamFromItem(Item item){
		Bitstream result = null;
		try{
			Bundle b = BundleUtils.getBundleByName(item, Constants.METADATA_BUNDLE_NAME);
			if (b != null){
				Bitstream s = b.getBitstreamByName(MANIFEST_FILE);
				if (s != null){
					result = s;
				}
			}
		} catch (Exception e){
			
		}
		return result;
	}


	private InputStream getManifestStreamfromPackageZip(ZipInputStream in){
		InputStream manifest = null;
		
		ZipEntry inEntry;
        try{
        	
        	while((inEntry = in.getNextEntry()) != null) {
        		if (inEntry.getName().equals(IMSIngester.MANIFEST_FILE)){
        			// Found the manifest
        			manifest = in;
        		}
        	}
        } catch (IOException e){
        	
        }
		
		return manifest;
		
	}
	
	private Bitstream archiveManifest(Context context, Item item, Bundle bundle, InputStream is, String name) throws SQLException, AuthorizeException, IOException{
		// NOTE: the method createBitstream below will eventually close the InputStream - no need to close it in this method
		Bitstream bs = bundle.createBitstream(is);
		bs.setName(name);
		bs.setSource(name);

		// Get magic bitstream format to identify manifest.
		BitstreamFormat manifestFormat = null;
		manifestFormat = PackageUtils.findOrCreateBitstreamFormat(context, MANIFEST_BITSTREAM_FORMAT,
				"application/xml", MANIFEST_BITSTREAM_FORMAT + " package manifest");
		bs.setFormat(manifestFormat);
		
		// Need to set the sequence number
		int higestCurrSequenceNum = BundleUtils.highestSequenceNumInBundles(item);
		bs.setSequenceID(higestCurrSequenceNum + 1);
		bs.update();
		
		return bs;
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageIngester#updateLicenceInfoInManifest(org.dspace.core.Context, org.dspace.content.Item, org.dspace.content.Bitstream, java.io.InputStream, boolean, java.lang.String, java.lang.String)
	 */
	public boolean updateLicenceInfoInManifest(Context context, 
											   Item item,
											   Bitstream bitstreamContainingManifest,
											   InputStream manifestStream,
											   boolean backupBitstream,
											   String licenceUrl, 
											   String licenceName) throws SQLException, 
																		  IOException, 
																		  AuthorizeException, 
																		  MetadataValidationException,
																		  CriticalException{
		boolean manifestChanged = false;
		boolean needToChangeManifestLicence = true;
		
		InputStream streamToUse = (bitstreamContainingManifest != null)?bitstreamContainingManifest.retrieve():manifestStream;
		
		try{
			if (streamToUse != null){ 
				// Parse the XML manifest again - do not need to validate hence the false
				XMLManifest manifest = createManifest(manifestStream, false);
				
				// Get the current licence URL - we may not need to change it!
				String currLicenceText = manifest.getMetadataFormat().getLicenceText(manifest.getManifestDocument());
				
				// Check in case the manifest didn't contain licence text !
				if (currLicenceText != null){
					// Must extract a CC licence URL from the licence text which was part of the XML manifest
		   	 		String currCCLicenceUrl = manifest.matchCCLicenceUrl(currLicenceText); 
					if (currCCLicenceUrl != null && currCCLicenceUrl.compareToIgnoreCase(licenceUrl) == 0){
						// they are the same!
						needToChangeManifestLicence = false;
					}
				}
				
				if (needToChangeManifestLicence) {
					try{
						manifest.getMetadataFormat().setLicenceText(manifest.getManifestDocument(), 			
																licenceName + " " + licenceUrl, 
																((XMLManifest)manifest).getRootMetadataElementXpathSelector(manifest.getMetadataFormat()),
																manifest.getMetadataElementPrefix());
						manifestChanged = true;
					} catch (Exception e){
						throw new MetadataValidationException(e);
					}
					// Now the manifest has been changed in memory
					
					// First backup the existing stream so we don't loose it just in case
					if (backupBitstream && bitstreamContainingManifest != null){
						bitstreamContainingManifest.setName(bitstreamContainingManifest.getName() + MANIFEST_BACKUP_STR + new SimpleDateFormat(DATE_FORMAT).format(new Date()));
						bitstreamContainingManifest.update();
					} else {
						Bundle owningBundles[] = bitstreamContainingManifest.getBundles();
						for (Bundle b : owningBundles){
							b.removeBitstream(bitstreamContainingManifest);
							b.update();
						}
					}
					
					
					// Now write the new manifest to a new bitstream
					try{
						archiveManifest(context,
									item,
									BundleUtils.getBundleByName(item, Constants.METADATA_BUNDLE_NAME), 
									manifest.getXmlAsStream(), 
									MANIFEST_FILE);
					// GWaller IssueID #484 XML from the XMLOutputter should be treated as UTF-8 encoded	
					} catch (UnsupportedEncodingException e){
						// An exception was thrown whilst obtaiing the XML stream - not much we can do here except raise a critical exception
						throw new CriticalException("Could not store manifest with updated licence info due to error obtaining the bytes of the oringinal manifest: " + e.getMessage());
					}
					
				}
				
			} else {
				// Manifest stream is null - cannot update something we can't find!
				// Throw a critical exception to indicate the stream was null.
				throw new CriticalException("Manifest stream is null - cannot update licence information");
			}
		} finally{
			if (streamToUse != null){ 
				streamToUse.close();
			}
		}
		return manifestChanged;
	}

	// END GWaller 02/02/09 IssueID #175 Added methods to deal with licence manipulation inside packages
	
	
}
