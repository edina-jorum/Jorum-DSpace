/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : XMLManifest.java
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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.PluginInstantiationException;
import org.dspace.core.PluginManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import uk.ac.jorum.utils.ExceptionLogger;

/**
 * @author gwaller
 *
 */
public abstract class XMLManifest {

	/** log4j category */
	private static Logger log = Logger.getLogger(XMLManifest.class);


	// START GWaller 52/02/09 IssueID #175 Moved MetadataFormat to sep class. Add in element deletion list
	private static XMLManifestElementDeletion[] ELEMENT_DELETION_LIST = {
			// Remove the o-ex rights element - not dependant on Metadataformat
			new XMLManifestElementDeletion(null, "//o-ex:rights", new Namespace[] { Namespace.getNamespace("o-ex",
					"http://odrl.net/1.1/ODRL-EX") }),
			// Remove the Technical section from LOM
			new XMLManifestElementDeletion(MetadataFormat.LOM, "//" + MetadataFormat.JORUM_NAMESPACE_PREFIX
					+ ":technical", null) };

	// END GWaller 52/02/09 IssueID #175 Moved MetadataFormat to sep class. Add in element deletion list

	protected static String localSchemas;

	/** prefix of config lines identifying local XML Schema (XSD) files */
	private final static String CONFIG_XSD_PREFIX = "ims.xsd.";

	/** config element for regex to pull out licence URL from the XML element value text */
	protected final static String CONFIG_LICENCE_URL_REGEX = "licence.url.regex";

	/** config element specifying which group (i.e. what matches within round brackets) in the URL regex to use as the URL - numbers start from 1 */
	protected final static String CONFIG_LICENCE_URL_REGEX_GROUP = "licence.url.regex.groupnum";

	protected final String DEFAULT_CC_URL_REGEX = ".*(http:\\/\\/creativecommons\\.org[\\S]*).*$";

	/** Prefix of DSpace configuration lines that map IMS metadata type to
	 * crosswalk plugin names.
	 */
	private final static String CONFIG_METADATA_PREFIX = "xml.submission.crosswalk.";

	protected static final String MATCHED_KEY = "matched";
	public static final String IDENTIFIER_REF_ATTR = "identifierref";
	public static final String IDENTIFIER_ATTR = "identifier";
	protected static final String ITEM_ID_PREFIX = "ITEM-";
	protected static final String RESOURCE_ID_PREFIX = "RES-";
	public static final String HREF_ATTR = "href";
	protected static final String RESOURCE_ELEM = "resource";
	protected static final String FILE_ELEM = "file";

	protected Document manifestDocument;
	protected Element manifestRoot;
	protected MetadataFormat format = null;
	protected String metadataPrefix = null;

	protected Map<String, Map<String, String>> bitstreamInfoMap = null;
	protected Bundle[] urlBundle = null;
	protected Bundle[] relatedBundle = null;
	protected Bundle[] contentBundle = null;
	protected Bundle[] metadata = null;
	protected Item item = null;
	protected String exportMetadataFormat = null;

	// Create list of local schemas at load time, since it depends only
	// on the DSpace configuration.
	static {
		String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
		File xsdPath1 = new File(dspace_dir + "/config/schemas/");
		File xsdPath2 = new File(dspace_dir + "/config/");

		Enumeration pe = ConfigurationManager.propertyNames();
		StringBuffer result = new StringBuffer();
		while (pe.hasMoreElements()) {
			// config lines have the format:
			//  mets.xsd.{identifier} = {namespace} {xsd-URL}
			// e.g.
			//  mets.xsd.dc =  http://purl.org/dc/elements/1.1/ dc.xsd
			// (filename is relative to {dspace_dir}/config/schemas/)
			String key = (String) pe.nextElement();
			if (key.startsWith(CONFIG_XSD_PREFIX)) {
				String spec = ConfigurationManager.getProperty(key);
				String val[] = spec.trim().split("\\s+");
				if (val.length == 2) {
					File xsd = new File(xsdPath1, val[1]);
					if (!xsd.exists())
						xsd = new File(xsdPath2, val[1]);
					if (!xsd.exists())
						log.warn("Schema file not found for config entry=\"" + spec + "\"");
					else {
						try {
							String u = xsd.toURL().toString();
							if (result.length() > 0)
								result.append(" ");
							result.append(val[0]).append(" ").append(u);
						} catch (java.net.MalformedURLException e) {
							log.warn("Skipping badly formed XSD URL: " + e.toString());
						}
					}
				} else
					log.warn("Schema config entry has wrong format, entry=\"" + spec + "\"");
			}
		}
		localSchemas = result.toString();
		log.debug("Got local schemas = \"" + localSchemas + "\"");
	}

	public XMLManifest(Document manifestDoc) {
		this.manifestDocument = manifestDoc;
		this.manifestRoot = this.manifestDocument.getRootElement();
		log.debug("XMLManifest constructor - root element is: " + this.manifestRoot.toString());
	}

	public XMLManifest() {

	}

	/**
	 * Create a new manifest object from a serialized manifest XML document.
	 * Parse document read from the input stream, optionally validating.
	 * NOTE: This method attempts to close the InputStream, is
	 * @param is input stream containing serialized XML
	 * @param validate if true, enable XML validation using schemas
	 *   in document.  Also validates any sub-documents.
	 * @throws MetadataValidationException if there is any error parsing
	 *          or validating the METS.
	 * @return new Document object.
	 */
	public static Document parseManifest(InputStream is, boolean validate) throws IOException,
			MetadataValidationException {
		SAXBuilder builder = new SAXBuilder(validate);

		// Set the SAX parser to expand entity references so no need to check in value strings
		builder.setExpandEntities(true);

		// Set validation feature
		if (validate) {
			builder.setFeature("http://apache.org/xml/features/validation/schema", true);
		}
       
		log.debug("Mainfest XML validation set to: " + validate);

		// Tell the parser where local copies of schemas are, to speed up
		// validation.  Local XSDs are identified in the configuration file.
		if (localSchemas.length() > 0)
			builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", localSchemas);

		// Parse the manifest file
		Document manifestDocument;
		try {
			manifestDocument = builder.build(is);

			// XXX for temporary debugging

			XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
			log.debug("Got IMS DOCUMENT:");
			//            log.debug(outputPretty.outputString(manifestDocument));

		} catch (JDOMException je) {
			throw new MetadataValidationException("Error validating IMS manifest in " + is.toString(), je);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
					ExceptionLogger.logException(log, e);
				}
			}
		}

		return manifestDocument;
	}

	/**
	 * Creates a brand new manifest for items that were not originally content packages
	 * @throws PackageException
	 * @throws PluginInstantiationException
	 * @throws CrosswalkException
	 * @throws IOException
	 * @throws SQLException
	 */
	public abstract void createNewManifest() throws PackageException, PluginInstantiationException, CrosswalkException,
			IOException, SQLException;

	/**
	 * Reconstruct original manifest for items submitted as content packages
	 * 
	 * @param originalManifest
	 * @throws PackageException
	 * @throws PluginInstantiationException
	 * @throws CrosswalkException
	 * @throws IOException
	 * @throws SQLException
	 */
	public abstract void reconstructManifest(Document originalManifest) throws PackageException,
			PluginInstantiationException, CrosswalkException, IOException, SQLException;

	/**
	 * Set the metadata format for an export
	 * @param exportMetadataFormat
	 */
	public abstract void setExportMetadataFormat(String exportMetadataFormat);

	/**
	 * Load original manifest and return it as a jDom document
	 * @param item
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws PackageException
	 * @throws MetadataValidationException
	 */
	public abstract Document recreateOriginalManifestDocument(Item item) throws IOException, SQLException,
			PackageException, MetadataValidationException;

	/**
	 * Construct a LinkedHashMap containing maps of the url, content and related bitstream info (as appropriate) we need
	 * to construct the organisation section of the manifest
	 * @param contentBundle array of content bitstreams
	 * @param urlBundle array of url bitstreams
	 * @param relatedBundle TODO
	 * @return Map of Maps with bitstream info
	 */
	protected LinkedHashMap<String, Map<String, String>> getBitstreamInfoMap(Bundle[] contentBundle,
			Bundle[] urlBundle, Bundle[] relatedBundle) {
		LinkedHashMap<String, Map<String, String>> bsInfoMap = new LinkedHashMap<String, Map<String, String>>();

		// The content bundle should always exists, although it might not contain any bitstreams
		int contentBundleLength = contentBundle.length;
		Bitstream[] contentBitstreams = null;
		int contentBitstreamLength = 0;

		if (contentBundleLength > 0) {
			contentBitstreams = contentBundle[0].getBitstreams();
			contentBitstreamLength = contentBitstreams.length;
		}

		int urlBundleLength = urlBundle.length;
		int relatedBundleLength = relatedBundle.length;

		if (contentBitstreamLength > 0 && urlBundleLength > 0) {

			log.debug("Populate bsInfoMap with url and content bitstreams");
			Bitstream[] urlBitstreams = urlBundle[0].getBitstreams();
			int urlBitstreamLength = urlBitstreams.length;
			Bitstream[] bitstreams = new Bitstream[contentBitstreamLength + urlBitstreamLength];
			System.arraycopy(contentBitstreams, 0, bitstreams, 0, contentBitstreamLength);
			System.arraycopy(urlBitstreams, 0, bitstreams, contentBitstreamLength, urlBitstreamLength);
			populateBitstreamInfo(bsInfoMap, bitstreams);

		} else if (contentBitstreamLength > 0 && urlBundleLength == 0) {
			log.debug("Populate bsInfoMap with content bitstreams");
			populateBitstreamInfo(bsInfoMap, contentBundle[0].getBitstreams());
		} else if (contentBitstreamLength == 0 && urlBundleLength > 0) {
			log.debug("Populate bsInfoMap with url bitstreams");
			populateBitstreamInfo(bsInfoMap, urlBundle[0].getBitstreams());
		} else if (relatedBundleLength > 0) {
			log.debug("Populate bsInfoMap with related bitstreams");
			populateBitstreamInfo(bsInfoMap, relatedBundle[0].getBitstreams());
		}

		return bsInfoMap;
	}

	/**
	 * Put details of each bitstream in a LinkedHashMap
	 * @param bsInfoMap
	 * @param bitstreams
	 */
	protected void populateBitstreamInfo(LinkedHashMap<String, Map<String, String>> bsInfoMap, Bitstream[] bitstreams) {

		for (Bitstream bitstream : bitstreams) {

			Map<String, String> bitstreamDetails = new HashMap<String, String>();

			String checksum = bitstream.getChecksum();
			String bsName = bitstream.getName();

			bitstreamDetails.put(IDENTIFIER_ATTR, new StringBuilder(ITEM_ID_PREFIX).append(checksum).toString());
			bitstreamDetails
					.put(IDENTIFIER_REF_ATTR, new StringBuilder(RESOURCE_ID_PREFIX).append(checksum).toString());

			String source = bitstream.getSource();
			if (source != null) {
				if (source.equals(Constants.RELATED_CONTENT_PACKAGE_BUNDLE)) {
					// For related bundles, determine the url, rather than just the handle
					log.debug("Related bitstream found - work out url");
					bitstreamDetails.put(HREF_ATTR, new StringBuilder(ConfigurationManager.getProperty("dspace.url"))
							.append(File.separator).append(bsName).toString());
				} else {
					bitstreamDetails.put(HREF_ATTR, bsName);
				}
			} else {
				bitstreamDetails.put(HREF_ATTR, bsName);
			}

			bitstreamDetails.put(MATCHED_KEY, "false");
			bsInfoMap.put(bsName, bitstreamDetails);
		}
	}

	public String matchCCLicenceUrl(String licenceText) throws MetadataValidationException {
		String url = null;

		if (licenceText == null) {
			// throw an excpetion - we can't get a licence if the text is null
			throw new MetadataValidationException("Null licence text supplied");
		}

		String ccUrlRegex = ConfigurationManager.getProperty(CONFIG_LICENCE_URL_REGEX);
		if (ccUrlRegex == null) {
			log.warn("Configuration property '" + CONFIG_LICENCE_URL_REGEX + "' not set - using default regex '"
					+ DEFAULT_CC_URL_REGEX + "'");
		}

		// Pull out the URL
		log.debug("Attempting to pull out licence url from string " + licenceText);
		log.debug("Using regex: " + ccUrlRegex);

		// Obtain a Matcher instance from the regex - no flags tp Patter - than can be supplied as modifiers in the pattern eg (?s)
		Matcher matcher = Pattern.compile(ccUrlRegex).matcher(licenceText);

		// Now run the regex against the string
		if (!matcher.matches()) {
			// Couldn't match pattern
			log.debug("Matcher returned false - pattern was not found");
		} else {
			int matchingGroups = matcher.groupCount();

			log.debug("Matcher found " + matchingGroups + " groups");

			String groupToMatch = ConfigurationManager.getProperty(CONFIG_LICENCE_URL_REGEX_GROUP);
			int groupToMatchVal = 1;
			if (groupToMatch == null) {
				log.warn("Configuration property '" + CONFIG_LICENCE_URL_REGEX_GROUP
						+ "' not set. Will therefore attempt to match on group 1");
			} else {
				try {
					groupToMatchVal = Integer.parseInt(groupToMatch);
					if (groupToMatchVal < 0) {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException e) {
					log.error("Invalid number specified for configuration property '"
							+ CONFIG_LICENCE_URL_REGEX_GROUP + "' - using 1 instead.");
					ExceptionLogger.logException(log, e);
				}
			}

			if (groupToMatchVal <= matchingGroups) {
				// Pull out the url using the group specified - this can return null if no match found
				url = matcher.group(groupToMatchVal);
			}
		}

		if (url != null) {
			log.debug("Found licence URL: " + url);
		} else {
			log.debug("Licence URL not found - regex either did not match or invalid group specified.");
		}

		return url;
	}

	/**
	 * @return the manifest
	 */
	public Element getRootElement() {
		return manifestRoot;
	}

	/**
	 * @return the manifestDocument
	 */
	public Document getManifestDocument() {
		return manifestDocument;
	}

	/** Find crosswalk for the indicated metadata type (e.g. "DC", "MODS")
	 * The crosswalk plugin name MAY be indirected in config file,
	 * through an entry like
	 *  ims.submission.crosswalk.{mdType} = {pluginName}
	 *   e.g.
	 *  ims.submission.crosswalk.DC = mysite-QDC 
	 */
	public IngestionCrosswalk getCrosswalk(String type) {
		String xwalkName = ConfigurationManager.getProperty(CONFIG_METADATA_PREFIX + type);
		if (xwalkName == null)
			xwalkName = type;
		return (IngestionCrosswalk) PluginManager.getNamedPlugin(IngestionCrosswalk.class, xwalkName);
	}

	// GWaller 3/2/10 IssueID #175 Added method to return the XML as an input stream
	public InputStream getXmlAsStream() throws UnsupportedEncodingException {
		InputStream stream = null;
		XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
		String xml = outputPretty.outputString(manifestDocument);

		// GWaller IssueID #484 XML from the XMLOutputter should be treated as UTF-8 encoded
		stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));

		return stream;
	}

	public abstract List<Element> getResources() throws MetadataValidationException;

	public abstract List<Element> getMetadataElements() throws MetadataValidationException;

	public abstract MetadataFormat getMetadataFormat() throws MetadataFormatException, MetadataValidationException;

	public abstract List<Element> getMetadataElements(Element mdRootNode) throws MetadataValidationException;

	public abstract MetadataFormat getMetadataFormat(Element mdRootNode) throws MetadataFormatException,
			MetadataValidationException;

	// GWaller 02/02/09 IssueID #175 Xpath selector for the IMS metadata element
	public abstract String getRootMetadataElementXpathSelector(MetadataFormat mdFormat) throws Exception;
	// GWaller 02/02/09 IssueID #175 Add method to return the prefix for metadata elements currently used in the manifest
	public String getMetadataElementPrefix() {
		if (this.metadataPrefix == null) {
			try {
				List<Element> mdElements = getMetadataElements();
				if (mdElements != null && mdElements.size() > 0) {
					this.metadataPrefix = mdElements.get(0).getNamespacePrefix();
				}
			} catch (Exception e) {

			}
		}

		// Could not detect it - simply use a null string
		if (this.metadataPrefix == null) {
			this.metadataPrefix = "";
		}

		return this.metadataPrefix;
	}

	/**
	 * Decide whether to create a new manifest or re-populate the original manifest
	 * 
	 * @throws PluginInstantiationException
	 * @throws PackageException
	 * @throws CrosswalkException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void populate() throws PluginInstantiationException, PackageException, CrosswalkException, IOException,
			SQLException {

		if (metadata.length == 0) {
			log.debug("Not originally a content package - create a new manifest");
			createNewManifest();

		} else {
			log.debug("Was a content package - have to read original manifest");
			// Reconstruct and check the original manifest
			reconstructManifest(recreateOriginalManifestDocument(item));
		}
	}

}
