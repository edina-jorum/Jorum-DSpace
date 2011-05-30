
package uk.ac.jorum.licence;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.exceptions.CriticalException;
import uk.ac.jorum.exceptions.NonCriticalException;
import uk.ac.jorum.utils.ExceptionLogger;

public abstract class LicenceController
{
	/** log4j category */
	private static Logger log = Logger.getLogger(LicenceController.class);
	
    /**
     * Some BitStream Names (BSN)
     */
    private static final String BSN_LICENSE_URL = "license_url";

    private static final String BSN_LICENSE_TEXT = "license_text";

    private static final String BSN_LICENSE_RDF = "license_rdf";

    private static final String BSN_LICENSE_NAME = "license_name";
    
    private static boolean enabled_p;
    
    private static LicenceManager[] managers;

    private static Hashtable<String, Templates> rdfTransformerHash;
    
    /**
     * Legacy CC bundle name - here for backwards compatibility. Remember existing items licensed under CC will have this bundle
     * All licence bitstreams should simply be stored in the LICENCE bundle from now and onwards.
     */
    public static final String LEGACY_CC_BUNDLE_NAME = "CC-LICENSE";
    public static final String LICENCE_BUNDLE_NAME = "ITEM-LICENSE";
    
    static
    {
        // we only check the property once
        enabled_p = ConfigurationManager
                .getBooleanProperty("webui.submit.enable-licencecontroller");

        if (enabled_p)
        {
            // if defined, set a proxy server for http requests to Creative
            // Commons site
            String proxyHost = ConfigurationManager
                    .getProperty("http.proxy.host");
            String proxyPort = ConfigurationManager
                    .getProperty("http.proxy.port");

            if ((proxyHost != null) && (proxyPort != null))
            {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
            }
        }
       
        Object [] licenceManagers = PluginManager.getPluginSequence(LicenceManager.class);
        rdfTransformerHash = new Hashtable<String,Templates>();
        managers = new LicenceManager[licenceManagers.length];
        for (int i = 0; i < managers.length; i++){
			managers[i] = (LicenceManager)licenceManagers[i];
			
			ItemLicence[] licences = managers[i].getInstalledLicences();
			for (ItemLicence l : licences){
				String rdfStyleSheet = l.getProps().getProperty(ItemLicence.RDF_STYLESHEET_KEY);
				if (rdfStyleSheet != null && rdfStyleSheet.length() > 0){
					try
			        {
						log.debug("LicenceController: Attempting to load RDF stylesheet from classpath: " + rdfStyleSheet);
						
						
			            InputStream rdfStylesheetStream = LicenceController.class.getResourceAsStream(rdfStyleSheet);
						if (rdfStylesheetStream != null){
							log.debug("LicenceController: Found RDF stylesheet");
							Templates templates = TransformerFactory.newInstance().newTemplates(new StreamSource(rdfStylesheetStream));
			            
							// Now add to the hashtable
							rdfTransformerHash.put(l.getProps().getProperty(ItemLicence.RDF_URL_KEY), templates);
						} else {
							throw new TransformerConfigurationException("Cannot find RDF Stylesheet on classpath: " + rdfStyleSheet);
						}
			            
			        }
			        catch (TransformerConfigurationException e)
			        {
			        	log.debug("LicenceController: Cannot find RDF Stylesheet on classpath: " + rdfStyleSheet);
			        	URLClassLoader urlLoader = (URLClassLoader)LicenceController.class.getClassLoader();
						URL[] urls = urlLoader.getURLs();
						for (URL u : urls){
							log.debug("Class path entry - " + u);
						}
			            throw new RuntimeException(e.getMessage(),e);
			        }
				}
			}
        }
        
        
        
    }
    
    /**
     * Simple accessor for enabling of LicenceManager
     */
    public static boolean isEnabled()
    {
        return enabled_p;
    }

    
    public static LicenceManager[] getLicenceManagers(){
    	return managers;
    }
    
    
    /**
     * This method will iterate across all the configures LicenceManager instances attempting to match the licence URL contained
     * in the Item supplied. If a match is found, the ItemLicence instance is returned.
     * @param item
     * @return ItemLicence instance which matches the licence URL in the item or null if not found
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static ItemLicence getItemLicence(Item item) throws SQLException, AuthorizeException, IOException{
    	ItemLicence result = null;
    	String licenceUrl = getLicenseURL(item);
    	
    	if (licenceUrl != null){
    		// We have a licence URL - now check to see if this is a URL which we support
    		
    		for (LicenceManager m : getLicenceManagers()){
    			ItemLicence[] licences = m.getInstalledLicences();
    			for (ItemLicence l : licences){
    				if (l.getProps().getProperty(ItemLicence.URL_KEY).equals(licenceUrl)){
        				result = l;
        				break;
        			}
    			}
    			
    			if (result != null){
    				break;
    			}
    		}
    	}
    	
    	return result;
    	
    }
    
    
    /**
     * This method checks to see if the supplied Item contains a supported licence URL assigned to it.
     * @param item the item to check
     * @return true if a supported licence was found, false otherwise
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static boolean hasSupportedLicence(Item item) throws SQLException, AuthorizeException, IOException{
    	boolean result = false;
    	
    	ItemLicence l = getItemLicence(item);
    	if (l != null){
    		result = true;
    	}
    	
    	return result;
    }
    
    /**
	 * This method examines the RELATED_CP bundle of the specified item and iterates across the items referenced
	 * in that bundle and check if the item contains an unsupported licence. If any unsupported licence is found, 
	 * the licence in that particular item is reset to the one specified and the return value is set to true.
	 *
	 * @param context the DSpace context to use
	 * @param item the DSpace Item to begin a nested licence search on - the RELATED_CP bundle will be examined for all items to check
	 * @param licenceUrl the licence URL which should be set if an unsupported licence is found. 
	 * @param checkCurrentItem check the licence in the item supplied as well as any items in the RELATED_CP bundle
	 * @param applyLicence if set to true, the unsupported licence will be reset to the licence supplied
	 * @return true if an unsupported licence was found in at least one item
	 */
	 private static boolean detectAndResetUnsupportedLicenceInRelatedItems(Context context, 
														   Item item, 
														   String licenceUrl,
														   boolean checkCurrentItem,
														   boolean applyLicence) throws IOException, SQLException, AuthorizeException, CriticalException{
		boolean foundUnsupportedLicence = false;
		
		Context itemContext = item.getContext();
		try{
			// MUST turn auth OFF temporarily for this context and items context.
			// Remember the items may have already been installed so you would normally need
			// admin privs to add a licence etc
			context.turnOffAuthorisationSystem();
			if (itemContext != null){
				itemContext.turnOffAuthorisationSystem();
			}
			
			if (checkCurrentItem){
				foundUnsupportedLicence = ! LicenceController.hasSupportedLicence(item);
			}
			
			// Check to see if this item has any "child" items (listed in the related bundle)
			Bundle[] relatedBundles = item.getBundles(Constants.RELATED_CONTENT_PACKAGE_BUNDLE);
			for (Bundle relatedBundle : relatedBundles){
				// Process the bitstreams in the bundle - the name of each bitstream will be the handle to a related (aka child) item
				// See uk.ac.jorum.submit.step.PackageDetectorStep method checkPackageAndProcess
				Bitstream[] bitstreams = relatedBundle.getBitstreams();
				for (Bitstream b : bitstreams){
					String handle = b.getName();
					DSpaceObject obj = HandleManager.resolveToObject(context, handle);
					if (obj != null && obj instanceof Item){
						Item relatedItem = (Item)obj;
						
						// Examine the license in this item and set if necessary
						if (!LicenceController.hasSupportedLicence(relatedItem)){
							foundUnsupportedLicence = true;
							
							// Set the licence
							if (applyLicence){
								LicenceController.setLicense(context, relatedItem, licenceUrl);
								
								// GWaller 16/2/10 IssueID #175 Must ensure that the licence which may be embedded in the item is consistent with the CC licence now in the CC bundle
					    		PackageIngester ingester = PackageUtils.getPackageIngester(relatedItem);
					    		// We have a content package
					    		if (ingester != null){
					    			try{
					    				ingester.updateEmbeddedLicence(context, relatedItem);
					    			} catch (NonCriticalException e){
					    				ExceptionLogger.logException(log, e);
					    				// It was non-critical so keep going
					    			}
					    		}
							}
							
							// Now process any related items in this item
							if (detectAndResetUnsupportedLicenceInRelatedItems(context, relatedItem, licenceUrl, false, applyLicence)){
								foundUnsupportedLicence = true;
							}
						}
					}
				}
				
			}
		
		} finally {
			// MUST RESET auth state in context !!!
			context.restoreAuthSystemState();
			if (itemContext != null){
				itemContext.restoreAuthSystemState();
			}
		}
		return foundUnsupportedLicence;
	}
	 
	public static boolean foundUnsupportedLicenceInItemOrRelated(Item item, Context context) throws IOException, SQLException, AuthorizeException{
		boolean result = false;
		try{
			result = detectAndResetUnsupportedLicenceInRelatedItems(context, item, null, true, false);
		} catch (CriticalException e){
			// This should never get executed - the CriticalException happens when the licence is set, we are only detecting not setting
			ExceptionLogger.logException(log, e);
		}
		return result;
	}
	
	public static void resetUnsupportedLicenceInRelatedItems(Item item, Context context, String licenceUrl) 
		throws IOException, SQLException, AuthorizeException, CriticalException{
		detectAndResetUnsupportedLicenceInRelatedItems(context, item, licenceUrl, false, true);
	}
    
	
    public static Bundle getLicenceBundle(Item item) throws SQLException{
    	Bundle[] legacyLicenceBundles = item.getBundles(LEGACY_CC_BUNDLE_NAME);
        Bundle[] licenceBundles = item.getBundles(LICENCE_BUNDLE_NAME);
        Bundle result = null;
        
        // Check the legacy bundle first
        if (legacyLicenceBundles.length > 0){
        	result = legacyLicenceBundles[0];
        } else if (licenceBundles.length > 0){
        	result = licenceBundles[0];
        }
        
        return result;
    }
	
        // create the licence bundle if it doesn't exist
        // If it does, remove it and create a new one.
    private static Bundle createLicenceBundle(Item item)
        throws SQLException, AuthorizeException, IOException
    {
    	removeLicense(item);
        
        return item.createBundle(LICENCE_BUNDLE_NAME);
    }


    
    /**
     * This is a bit of the "do-the-right-thing" method for licence stuff in an item
     * 
     */
    public static void setLicense(Context context, Item item,
            String license_url) throws SQLException, IOException,
            AuthorizeException
    {
    	// Find the ItemLicence instance from the licence url
        ItemLicence l = getItemLicenceMappedToUrl(license_url);
        
        if (l == null){
        	// Couldn't find a valid ItemLicence instance for the licence url - unsupported URL, throw an exception
        	throw new AuthorizeException("Unauthorized license requested. The following license URL is unsupported: " + license_url);
        }
        
    	
    	Bundle bundle = createLicenceBundle(item);
    	String rdf_licence_url = l.getProps().getProperty(ItemLicence.RDF_URL_KEY);
    	String license_name = l.getProps().getProperty(ItemLicence.NAME_KEY);
        
        
        // get some more information
        String license_text = fetchLicenseText(license_url);
        
        String license_rdf = null;
        if (rdf_licence_url != null){
        	license_rdf = fetchLicenseRDF(rdf_licence_url);
        }
        
        // set the format
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(
                context, "License");

        // set the URL bitstream
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_URL, bs_format,
                license_url.getBytes());

        //Allows user to view rendered licence rather than the text source of the CC web page
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_TEXT, BitstreamFormat.findByShortDescription(
                context, "HTML"),
              license_text.getBytes());

        // Add licence url and licence name to dc.rights and dc.rights.uri respectively 
        BundleUtils.clearAndSetMetadataElement(license_url, item, Constants.DC_SCHEMA, Constants.DC_RIGHTS, Constants.DC_RIGHTS_URI, BundleUtils.getDefaultLanguageQualifier());
        
        if (license_name != null){
        	BundleUtils.clearAndSetMetadataElement(license_name, 
        										   item, 
        										   Constants.DC_SCHEMA, 
        										   Constants.DC_RIGHTS, 
        										   null, 
        										   BundleUtils.getDefaultLanguageQualifier());
        	
        	// set the licence name
            setBitstreamFromBytes(item, bundle, BSN_LICENSE_NAME, BitstreamFormat.findByShortDescription(
                    context, "Text"),  license_name.getBytes());
        }
        
        // set the RDF bitstream
        if (license_rdf != null && license_rdf.length() > 0){
        	setBitstreamFromBytes(item, bundle, BSN_LICENSE_RDF, bs_format,
                license_rdf.getBytes());
        }
       
    }
    
    public static void setLicense(Context context, Item item,
                                  InputStream licenseStm, String mimeType)
            throws SQLException, IOException, AuthorizeException
    {
        Bundle bundle = createLicenceBundle(item);

        // generic "License" format -- change for CC?
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(
                context, "License");

        Bitstream bs = bundle.createBitstream(licenseStm);
        
        bs.setName((mimeType != null &&
                    (mimeType.equalsIgnoreCase("text/xml") ||
                     mimeType.equalsIgnoreCase("text/rdf"))) ?
                   BSN_LICENSE_RDF : BSN_LICENSE_TEXT);
        bs.setFormat(bs_format);
        bs.update();
    }

    public static void removeLicense(Item item)
            throws SQLException, IOException, AuthorizeException
    {
        // remove license bundle if one exists
    	Bundle licenceBundle = getLicenceBundle(item);
        if (licenceBundle != null){
        	item.removeBundle(licenceBundle);
        }
    }

    public static boolean hasLicense(Context context, Item item)
            throws SQLException, IOException
    {
    	Bundle licenceBundle = getLicenceBundle(item);

        // Check if there is a legacy license bundle or an item licence bundle 
        if (licenceBundle == null)
        {
            return false;
        }

        // verify it has correct contents
        try
        {
            // GWaller IssueID #303 The below check used to test for licence URL, text and RDF
            //                      Not all licences may have RDF so removing this check.
            if ((getLicenseURL(item) == null) || (getLicenseText(item) == null))
            {
                return false;
            }
        }
        catch (AuthorizeException ae)
        {
            return false;
        }

        return true;
    }

    
    public static String getLicenseName(Item item) throws SQLException,
    IOException, AuthorizeException
    {
    	return getStringFromBitstream(item, BSN_LICENSE_NAME);
    }
    
   
    /**
     * Gets the location of the appropriate licence icon 
     * 
     * 
     * @param item
     * @return String path to the icon
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public static String getLicenseIconLocation(Item item) throws SQLException,
    IOException, AuthorizeException
    {	
    	String result = "";
    	String name = getLicenseName(item);
    	String url = getLicenseURL(item);
    	
    	for (LicenceManager m : getLicenceManagers()){
			ItemLicence[] licences = m.getInstalledLicences();
			for (ItemLicence l : licences){
				if (l.getProps().getProperty(ItemLicence.URL_KEY).equals(url) ||
					l.getProps().getProperty(ItemLicence.NAME_KEY).equals(name)	){
    				result = l.getProps().getProperty(ItemLicence.ICON_KEY);
    				break;
    			}
			}
			
			if (result.length() > 0){
				break;
			}
		}
    	
    	return result;
    	
    }
    
    
    public static String licenceNameMappedToUrl(String url){
    	String result = "";
    	
    	ItemLicence l = getItemLicenceMappedToUrl(url);
    	if (l != null){
    		result = l.getProps().getProperty(ItemLicence.NAME_KEY);
    	}
    	
    	return result;
    }
    
    public static ItemLicence getItemLicenceMappedToUrl(String url){
    	ItemLicence result = null;
    	
    	for (LicenceManager m : getLicenceManagers()){
			ItemLicence[] licences = m.getInstalledLicences();
			for (ItemLicence l : licences){
				if (l.getProps().getProperty(ItemLicence.URL_KEY).equals(url)){
					result = l;
					break;
				}
			}
    	}
    	
    	return result;
    }
    
    public static String getLicenseURL(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_URL);
    }

    public static String getLicenseText(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_TEXT);
    }

    public static String getLicenseRDF(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getStringFromBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseRdfBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseTextBitstream(Item item) throws SQLException,
            IOException, AuthorizeException
    {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }


    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     */
    public static String fetchLicenseText(String license_url)
    {
        String text_url = license_url;
        byte[] urlBytes = fetchURL(text_url);

        return (urlBytes != null) ? new String(urlBytes) : "";
    }

    public static String fetchLicenseRDF(String rdf_license_url) throws IOException
    {
        StringWriter result = new StringWriter();
        
        try
        {
        	Templates templates = rdfTransformerHash.get(rdf_license_url);
        	if (templates != null){
        		templates.newTransformer().transform(
                        new StreamSource(rdf_license_url),
                        new StreamResult(result)
                        );
        	} else {
        		log.error("fetchLicenseRDF: ERROR RDF URL specified for licence but a RDF stylesheet was not found or an error occurred creating the transformer");
        	}
        }
        catch (TransformerException e)
        {
            throw new IOException(e.getMessage());
        }

        return result.getBuffer().toString();
    }

    // The following two helper methods assume that the CC
    // bitstreams are short and easily expressed as byte arrays in RAM

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
     */
    private static void setBitstreamFromBytes(Item item, Bundle bundle,
            String bitstream_name, BitstreamFormat format, byte[] bytes)
            throws SQLException, IOException, AuthorizeException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Bitstream bs = bundle.createBitstream(bais);

        bs.setName(bitstream_name);

        bs.setFormat(format);

        // commit everything
        bs.update();
    }

    /**
     * This helper method wraps a String around a byte array returned from the
     * bitstream method further down
     */
    private static String getStringFromBitstream(Item item,
            String bitstream_name) throws SQLException, IOException,
            AuthorizeException
    {
        byte[] bytes = getBytesFromBitstream(item, bitstream_name);

        if (bytes == null)
        {
            return null;
        }

        return new String(bytes);
    }

    /**
     * This helper method retrieves the bytes of a bitstream for an item under
     * the licence bundle, with the given bitstream name
     */
    private static Bitstream getBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
    	Bitstream result = null;
    	Bundle licenceBundle = getLicenceBundle(item);

        if (licenceBundle != null){
        	result = licenceBundle.getBitstreamByName(bitstream_name);
        }
        
        return result;
        
    }

    private static byte[] getBytesFromBitstream(Item item, String bitstream_name)
            throws SQLException, IOException, AuthorizeException
    {
        Bitstream bs = getBitstream(item, bitstream_name);

        // no such bitstream
        if (bs == null)
        {
            return null;
        }

        // create a ByteArrayOutputStream
	    // IssueID #572: Fixed file descriptor leak, IF 20/12/10
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            is = bs.retrieve();
            Utils.copy(is, baos);
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
        return baos.toByteArray();
    }

    /**
     * Fetch the contents of a URL
     */
    private static byte[] fetchURL(String url_string)
    {
        try
        {
            URL url = new URL(url_string);
            URLConnection connection = url.openConnection();
            byte[] bytes = new byte[connection.getContentLength()];

            // loop and read the data until it's done
            int offset = 0;

            while (true)
            {
                int len = connection.getInputStream().read(bytes, offset,
                        bytes.length - offset);

                if (len == -1)
                {
                    break;
                }

                offset += len;
            }

            return bytes;
        }
        catch (Exception exc)
        {
            return null;
        }
    }
}
