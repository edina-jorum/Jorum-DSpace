/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : IMSManifest.java
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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.PluginInstantiationException;
import org.dspace.core.PluginManager;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * @author gwaller
 * @author cgormley
 */
public class IMSManifest extends XMLManifest {

	/** log4j category */
	static Logger log = Logger.getLogger(IMSManifest.class);

	/** IMS namespace */
	public static final String IMS_CP_URI = "http://www.imsglobal.org/xsd/imscp_v1p1";
	public static final String IMS_XSD = "http://www.imsglobal.org/xsd/imscp_v1p1.xsd";
	public static Namespace IMS_CP_NS = Namespace.getNamespace("imscpv1p1", IMS_CP_URI);
	public static Namespace IMS_CP_NS_UNNAMED = Namespace.getNamespace(IMS_CP_URI);
	public static Namespace XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

	/**
	 * The XPath expressoion that isolates the top level element from which
	 * metadat aelements in the appropriate namespace can be added as children.
	 * 
	 * e.g.
	 * 
	 * For LOM this should pick the "metadata" element below <manifest
	 * xmlns="http://www.imsglobal.org/xsd/imscp_v1p1"
	 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	 * xmlns:lom="http://ltsc.ieee.org/xsd/LOM" xsi:schemaLocation=
	 * "http://www.imsglobal.org/xsd/imscp_v1p1 http://www.imsglobal.org/xsd/imscp_v1p1p3.xsd"
	 * identifier="MANIFEST1"> <metadata> <schema>IMS Content</schema>
	 * <schemaversion>1.2.2</schemaversion> <lom:lom xsi:schemaLocation=
	 * "http://ltsc.ieee.org/xsd/LOM http://ltsc.ieee.org/xsd/lomv1.0/lomLoose.xsd"
	 * >
	 * 
	 * This is the same for IMSMD and DC.
	 * 
	 */
	private static final String IMS_ROOT_METADATA_ELEMENT_XPATH = "/*[local-name() = 'manifest']/*[local-name() = 'metadata']";

	private List<Element> contentFiles = null;

	private static final String MANIFEST_ID_PREFIX = "MANIFEST-";
	private static final String ORGANIZATION_ID_PREFIX = "ORG-";
	private static final String DEFAULT_ATTR = "default";
	private static final String STRUCTURE_ATTR = "structure";
	public static final String IS_VISIBLE_ATTR = "isvisible";
	private static final String TYPE_ATTR = "type";
	public static final String ITEM_ELEM = "item";
	public static final String TITLE_ELEM = "title";
	private static final String MANIFEST_ELEM = "manifest";
	public static final String ORGANIZATION_ELEM = "organization";
	public static final String ORGANIZATIONS_ELEM = "organizations";
	private static final String SCHEMA_VERSION_ELEM = "schemaversion";
	private static final String SCHEMA_ELEM = "schema";
	private static final String METADATA_ELEM = "metadata";
	private static final String RESOURCES_ELEM = "resources";
	private static final String SCHEMA_LOCATION_ATTR = "schemaLocation";
	private static final String SCORM_NAMESPACE_ID = "http://www.adlnet.org/xsd";

	public IMSManifest(Document manifestDoc) {
		super(manifestDoc);
		log.debug("IMSManifest constructor - root element is: " + this.manifestRoot.toString());
	}

	public IMSManifest(Item item) throws PackageException {
		// TODO Auto-generated constructor stub
		try {
			this.urlBundle = item.getBundles(Constants.URL_BUNDLE);
			this.contentBundle = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
			this.relatedBundle = item.getBundles(Constants.RELATED_CONTENT_PACKAGE_BUNDLE);
			this.metadata = item.getBundles(Constants.METADATA_BUNDLE_NAME);
		} catch (SQLException e) {
			throw new PackageException(
					"Error: Problem reading either content or url bundle - no point proceeding with this item.", e);
		}

		this.bitstreamInfoMap = getBitstreamInfoMap(contentBundle, urlBundle, relatedBundle);
		this.item = item;
	}

	/**
	 * Reconstruct manifest for an item submitted as a content package and set
	 * manifestDocument and manifestRoot
	 * 
	 * @param originalManifest
	 * @throws PackageException
	 * @throws PluginInstantiationException
	 * @throws CrosswalkException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void reconstructManifest(Document originalManifest) throws PackageException, PluginInstantiationException,
			CrosswalkException, IOException, SQLException {

		Element originalManifestRoot = originalManifest.getRootElement();

		// Get content from originalManifest
		List<Element> clonedContent = originalManifestRoot.cloneContent();

		// We have to ensure that the manifest has the correct namespaces and schema declarations
		// Get the original identifier
		String identifier = validateAttribute(originalManifestRoot.getAttribute(IDENTIFIER_ATTR).getValue(),
				MANIFEST_ID_PREFIX);

		//TODO: SCORM code should be in SCORMManifest class
		// Check for SCORM namespace - we should preserve it if it exists
		List<Namespace> scormNamespaces = new ArrayList<Namespace>();
		for (Iterator<Namespace> iterator = originalManifestRoot.getAdditionalNamespaces().iterator(); iterator
				.hasNext();) {
			Namespace namespace = iterator.next();
			String uri = namespace.getURI();
			if (uri != null) {
				if (uri.startsWith(SCORM_NAMESPACE_ID)) {
					System.out.println("We have a scorm package");
					scormNamespaces.add(namespace);
				}
			}
		}

		// Get the value of schemaLocation attribute and split on white space
		Attribute schemaLocationAtt = originalManifestRoot.getAttribute(SCHEMA_LOCATION_ATTR, XSI);
		String[] split = null;
		if (schemaLocationAtt != null) {
			String schemaLocationValue = schemaLocationAtt.getValue();
			if (schemaLocationValue != null) {
				split = schemaLocationValue.split(" ");
			}
		}

		// Create a new root element from scratch
		// Populate namespace declaration, schemaLocation and identifier as appropriate
		Element manifestRoot = new Element(IMSManifest.MANIFEST_ELEM, IMS_CP_NS_UNNAMED);
		manifestRoot.addNamespaceDeclaration(XSI);

		StringBuilder schemaLocation = new StringBuilder(IMS_CP_URI).append(" ").append(IMS_XSD);

		// If any scorm namespaces were detected, we should add appropriate entry to schemaLocation
		for (Iterator<Namespace> iterator = scormNamespaces.iterator(); iterator.hasNext();) {
			Namespace scormNamespace = iterator.next();
			manifestRoot.addNamespaceDeclaration(scormNamespace);
			// need to get the associated xsd file
			for (int i = 0; i < split.length; i++) {
				String entry = split[i];
				String id = scormNamespace.getURI();
				if (entry.equals(id)) {
					// The xsd should be the following entry in the array
					String xsd = split[i + 1];
					if (xsd != null)
						schemaLocation.append(" ").append(id).append(" ").append(xsd);
				}
			}
		}

		// Add schemaLocation to root
		manifestRoot.setAttribute(SCHEMA_LOCATION_ATTR, schemaLocation.toString(), XSI);
		manifestRoot.setAttribute(IDENTIFIER_ATTR, identifier);

		// Add clonedContent to this new root element
		manifestRoot.addContent(clonedContent);

		// Create a new Document and set the root
		Document updatedManifest = new Document();
		updatedManifest.setRootElement(manifestRoot);

		//remove existing metadata
		manifestRoot.getChild(IMSManifest.METADATA_ELEM, IMS_CP_NS_UNNAMED).removeContent();
		// Slot in the metadata
		addMetadataToManifest(manifestRoot);
		log.debug("Added metadata to manifest");

		// Have to check resources first so we know the link between resources and bitstream names.
		Element resources = manifestRoot.getChild(IMSManifest.RESOURCES_ELEM, IMS_CP_NS_UNNAMED);
		checkOriginalResources(resources);

		Element organizations = manifestRoot.getChild(IMSManifest.ORGANIZATIONS_ELEM, IMS_CP_NS);
		checkOriginalOrganisation(organizations);

		// ensure organisation contains an item.  If not it will be deleted and an empty organisations element will be added
		checkOriginalOrganisationContainsItem(organizations, manifestRoot);

		// Check for any unmatched bitstreams (excluding those whose name ends with .xsd) and add them. 
		Set<Entry<String, Map<String, String>>> entrySet = bitstreamInfoMap.entrySet();
		for (Iterator<Entry<String, Map<String, String>>> iterator = entrySet.iterator(); iterator.hasNext();) {
			Entry<String, Map<String, String>> entry = iterator.next();
			Map<String, String> bsMap = entry.getValue();
			String key = entry.getKey();
			String matched = bsMap.get(MATCHED_KEY);

			//NOTE originally we were preventing schema files being added to the manifest - this has now been removed
			//			if (matched.equals("false") && !key.endsWith(".xsd")) {
			if (matched.equals("false")) {
				//add to resources section of manifest

				log.debug("Adding unmatched bitstream " + key + " to manifest");
				Element resource = new Element(XMLManifest.RESOURCE_ELEM, IMS_CP_NS_UNNAMED);
				resource.setAttribute(XMLManifest.HREF_ATTR, bsMap.get(XMLManifest.HREF_ATTR));
				resource.setAttribute(XMLManifest.IDENTIFIER_ATTR, bsMap.get(XMLManifest.IDENTIFIER_REF_ATTR));
				resource.setAttribute(IMSManifest.TYPE_ATTR, "webcontent");
				resources.addContent(resource);

			}
		}

		this.manifestDocument = updatedManifest;
		this.manifestRoot = manifestRoot;
		log.debug("XMLManifest constructor - root element is: " + this.manifestRoot.toString());
	}

	/**
	 * Create a new IMSManifest for an item that wasn't ingested as a content package. 
	 * Sets manifestDocument and manifestRoot
	 * @throws PackageException
	 * @throws PluginInstantiationException
	 * @throws CrosswalkException
	 * @throws IOException
	 */
	public void createNewManifest() throws PackageException, PluginInstantiationException, CrosswalkException,
			IOException, SQLException {

		Element manifestRoot = new Element(IMSManifest.MANIFEST_ELEM, IMS_CP_NS_UNNAMED);
		manifestRoot.addNamespaceDeclaration(XSI);
		manifestRoot.setAttribute(SCHEMA_LOCATION_ATTR, IMS_CP_URI + " " + IMS_XSD, XSI);

		// TODO:Maybe we need a different identifier here
		// The identifier cannot contain a :, *  or /.  See http://www.w3.org/TR/REC-xml-names/#NT-NCName
		manifestRoot.setAttribute(XMLManifest.IDENTIFIER_ATTR, IMSManifest.MANIFEST_ID_PREFIX
				+ item.getHandle().replace("/", "."));

		// Add the metadata
		manifestRoot.addContent(new Element(IMSManifest.METADATA_ELEM, IMS_CP_NS_UNNAMED));
		addMetadataToManifest(manifestRoot);
		log.debug("Added metadata to manifest");

		manifestRoot.addContent(new Element(IMSManifest.ORGANIZATIONS_ELEM, IMS_CP_NS_UNNAMED));
		addDefaultOrganisations(manifestRoot);
		log.debug("Added organisations to manifest");

		// Add the resources
		manifestRoot.addContent(new Element(IMSManifest.RESOURCES_ELEM, IMS_CP_NS_UNNAMED));
		addResourcesToManifest(manifestRoot);
		log.debug("Added resources to manifest");

		Document manifestDocument = new Document(manifestRoot);
		String xml = new XMLOutputter(Format.getPrettyFormat()).outputString(manifestDocument);
		log.debug(xml);

		this.manifestDocument = manifestDocument;
		this.manifestRoot = this.manifestDocument.getRootElement();
		log.debug("XMLManifest constructor - root element is: " + this.manifestRoot.toString());

	}

	/**
	 * Crosswalks the metadata and adds to manifest
	 * @param manifestRoot
	 * @throws PluginInstantiationException
	 * @throws CrosswalkException
	 */
	private void addMetadataToManifest(Element manifestRoot) throws PluginInstantiationException, CrosswalkException {

		DisseminationCrosswalk xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
				DisseminationCrosswalk.class,
				Constants.SupportedDisseminationMetadataFormats.getMetadataFormat(this.exportMetadataFormat));
		Element md = null;
		try {
			md = xwalk.disseminateElement(this.item);
		} catch (Exception e) {
			throw new CrosswalkException("Problem crosswalking metadata.", e);
		}
		manifestRoot.getChild(IMSManifest.METADATA_ELEM, IMS_CP_NS_UNNAMED).addContent(md);
	}

	/**
	 * Adds default organisation to a manifest
	 * @param manifestRoot
	 */
	private void addDefaultOrganisations(Element manifestRoot) {
		Element organizations = manifestRoot.getChild(IMSManifest.ORGANIZATIONS_ELEM, IMS_CP_NS_UNNAMED).setAttribute(
				IMSManifest.DEFAULT_ATTR, IMSManifest.ORGANIZATION_ID_PREFIX + "1");
		Element organization = new Element(IMSManifest.ORGANIZATION_ELEM, IMS_CP_NS_UNNAMED);
		organization.setAttribute(XMLManifest.IDENTIFIER_ATTR, IMSManifest.ORGANIZATION_ID_PREFIX + "1");
		organization.setAttribute(IMSManifest.STRUCTURE_ATTR, "hierarchical");

		//Title of the item
		organization.addContent(new Element(IMSManifest.TITLE_ELEM, IMS_CP_NS_UNNAMED).addContent(item.getName()));

		setUnmatchedOrgElementsFromBs(organization);

		organizations.addContent(organization);
	}

	/**
	 * Recursively parse the organisation section of original manifest.  Ensure that item identifiers match up with
	 * available bitstreams.  If not these elements should be removed
	 * @param element
	 */
	private void checkOriginalOrganisation(Element element) {

		// loop through child nodes
		List<Object> children = element.getContent();
		for (int i = 0; i < children.size(); i++) {

			// handle child by node type
			Object child = children.get(i);
			if (child instanceof Element) {
				String elementName = ((Element) child).getName();
				log.debug("Element Name: " + elementName);
				//check if any attributes
				List<Attribute> attributes = ((Element) child).getAttributes();
				for (Iterator<Attribute> iterator = attributes.iterator(); iterator.hasNext();) {
					Attribute attribute = iterator.next();
					String attributeName = attribute.getName();
					String attributeValue = attribute.getValue();
					log.debug("Attribute name:" + attributeName + ", value:" + attributeValue);

					// Ensure identifier is valid
					if (attributeName.equals(XMLManifest.IDENTIFIER_ATTR)) {
						((Element) child).setAttribute(IDENTIFIER_ATTR,
								validateAttribute(attributeValue, ITEM_ID_PREFIX));
					}

					else if (attributeName.equals(XMLManifest.IDENTIFIER_REF_ATTR)) {
						Set<Entry<String, Map<String, String>>> entrySet = bitstreamInfoMap.entrySet();
						boolean found = false;

						attributeValue = validateAttribute(attributeValue, RESOURCE_ID_PREFIX);
						((Element) child).setAttribute(IDENTIFIER_REF_ATTR, attributeValue);
						for (Iterator<Entry<String, Map<String, String>>> itr = entrySet.iterator(); itr.hasNext();) {
							Entry<String, Map<String, String>> entry = itr.next();
							String idRef = entry.getValue().get(XMLManifest.IDENTIFIER_REF_ATTR);
							if (idRef != null) {
								if (idRef.equals(attributeValue)) {
									found = true;
									break;
								}
							}
						}
						if (!found) {
							// Can't find a matching bitstream - removing the current element and then decrementing the index
							//  so that the next loop looks at the next element. 
							children.remove(i--);
						}
					}
				}

				// handle child elements with recursive call
				checkOriginalOrganisation((Element) child);
			}
		}
	}

	/**
	 * Use Xpath to check for the existence of an item in the organizations section
	 * If no result, we delete the original organizations element and replace it with 
	 * an empty organizations element
	 * @param organizations
	 * @param manifestRoot 
	 */
	private void checkOriginalOrganisationContainsItem(Element organizations, Element manifestRoot) {
		try {
			// Check if there is any item within the organizations element
			XPath xpathIns = XPath.newInstance("//imscpv1p1:item");
			xpathIns.addNamespace("imscpv1p1", IMS_CP_URI);

			Object result = xpathIns.selectSingleNode(organizations);
			if (result == null || !(result instanceof Element)) {
				log.debug("No items found - we should remove the organisation");

				// If no items in organisations, we should remove all other content
				// No point in keeping it if it doesn't contain anything that links to
				// an entry in the resources section
				List<Element> children = manifestRoot.getChildren();
				int orgIndex = children.indexOf(organizations);
				// Remove current organization and replace with empty organization at same position
				Element orgElement = new Element(ORGANIZATIONS_ELEM, IMS_CP_NS_UNNAMED);
				children.remove(orgIndex);
				children.add(orgIndex, orgElement);
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Recursively parse resources section of original manifest and ensure resources listed match the bitstreams 
	 * available.  If not, remove element from document.
	 * @param element
	 * @throws UnsupportedEncodingException
	 */
	private void checkOriginalResources(Element element) throws UnsupportedEncodingException {

		// loop through child nodes
		List<Object> children = element.getContent();
		for (int i = 0; i < children.size(); i++) {

			// handle child by node type
			Object child = children.get(i);
			if (child instanceof Element) {
				String elementName = ((Element) child).getName();
				log.debug("Element Name: " + elementName);

				if (elementName.equals(XMLManifest.RESOURCE_ELEM)) {
					//we want to add the identifier to the map
					String identifier = ((Element) child).getAttribute(XMLManifest.IDENTIFIER_ATTR).getValue();
					String href = ((Element) child).getAttribute(XMLManifest.HREF_ATTR).getValue();

					// Note we have to decode the href from the manifest (See IssueID #118). The bitstream names are already
					// decoded so we should look up the bitstreamInfoMap with a decoded href key
					Map<String, String> bsMap = bitstreamInfoMap.get(URLDecoder.decode(href, "UTF-8"));

					if (bsMap == null || bsMap.get(XMLManifest.HREF_ATTR) == null) {
						// Can't find a matching bitstream - removing the current element and then decrementing the index
						//  so that the next loop looks at the next element. 
						children.remove(i--);
					} else {
						bsMap.put(MATCHED_KEY, "true");
						// add the identifier as identifierref

						// ensure form of identifier is valid
						identifier = validateAttribute(identifier, RESOURCE_ID_PREFIX);
						((Element) child).setAttribute(IDENTIFIER_ATTR, identifier);
						bsMap.put(XMLManifest.IDENTIFIER_REF_ATTR, identifier);
						bitstreamInfoMap.put(href, bsMap);
					}

				} else {
					//The file element could have an href attribute - we have to check if it's valid
					Attribute hrefAttr = ((Element) child).getAttribute(XMLManifest.HREF_ATTR);
					if (hrefAttr != null) {
						String href = hrefAttr.getValue();
						if (href != null) {
							// Note we have to decode the href from the manifest (See IssueID #118). The bitstream names are already
							// decoded so we should look up the bitstreamInfoMap with a decoded href key
							Map<String, String> bsMap = bitstreamInfoMap.get(URLDecoder.decode(href, "UTF-8"));
							if (bsMap == null) {
								// Can't find a matching bitstream - removing the current element and then decrementing the index
								//  so that the next loop looks at the next element. 
								children.remove(i--);
							} else {
								bsMap.put(MATCHED_KEY, "true");
								bitstreamInfoMap.put(href, bsMap);
							}

						}
					}
				}
				// handle child elements with recursive call
				checkOriginalResources((Element) child);
			}
		}
	}

	/**
	 * Have to call this method for identifierref and identifier attributes in imsmanifest.
	 * Packages ingested via Mr Cute have invalid values.  They start and end with { and }
	 * respectively, which means they are not valid as NCNames.
	 * @param identifier
	 * @param prefix Prefix to prepend returned identifier
	 * @return
	 */
	private String validateAttribute(String identifier, String prefix) {
		return identifier.replace("{", prefix).replace("}", "");
	}

	public Element getOriginalOrganisationElement(XMLManifest originalManifest) throws IOException, SQLException,
			PackageException, MetadataValidationException {
		return originalManifest.getManifestDocument().getRootElement()
				.getChild(IMSManifest.ORGANIZATIONS_ELEM, IMS_CP_NS);

	}

	/**
	 * Adds any bitstreams marked as unmatched to jDom organisation element
	 * @param organization
	 */
	private void setUnmatchedOrgElementsFromBs(Element organization) {
		for (Map<String, String> itemMap : bitstreamInfoMap.values()) {
			if (!itemMap.get(MATCHED_KEY).equals("true")) {
				String name = itemMap.get(XMLManifest.HREF_ATTR);
				log.debug("Bitstream " + name + " was unmatched.  Adding now");
				//TODO: Visible is set to true here - should this be the default?
				addOrgElement(organization, "true", itemMap, name);
			}
		}
	}

	/**
	 * Add an individual organisation to the manifest document
	 * 
	 * @param organization
	 * @param visible
	 * @param itemMap
	 * @param name
	 */
	private void addOrgElement(Element organization, String visible, Map<String, String> itemMap, String name) {
		Element itemElement = new Element(IMSManifest.ITEM_ELEM, IMS_CP_NS_UNNAMED);
		itemElement.setAttribute(XMLManifest.IDENTIFIER_ATTR, itemMap.get(XMLManifest.IDENTIFIER_ATTR));
		itemElement.setAttribute(XMLManifest.IDENTIFIER_REF_ATTR, itemMap.get(XMLManifest.IDENTIFIER_REF_ATTR));
		itemElement.setAttribute(IMSManifest.IS_VISIBLE_ATTR, visible);
		itemElement.addContent(new Element(IMSManifest.TITLE_ELEM, IMS_CP_NS_UNNAMED).addContent(name));

		organization.addContent(itemElement);
	}

	/**
	 * Add resources to the manifest document
	 * @param manifestRoot
	 */
	private void addResourcesToManifest(Element manifestRoot) {

		Element resources = manifestRoot.getChild(IMSManifest.RESOURCES_ELEM, IMS_CP_NS_UNNAMED);

		if (relatedBundle.length > 0) {
			setResources(resources, relatedBundle[0].getBitstreams());
		}

		// Set content and urls
		if (contentBundle.length > 0) {
			setResources(resources, contentBundle[0].getBitstreams());
		}
		if (urlBundle.length > 0) {
			setResources(resources, urlBundle[0].getBitstreams());
		}

	}

	/**
	 * Adds an individual resource element to the manifest document
	 * 
	 * @param resources
	 * @param bitstreams
	 */
	private void setResources(Element resources, Bitstream[] bitstreams) {
		for (Bitstream bitstream : bitstreams) {
			Element resource = new Element(XMLManifest.RESOURCE_ELEM, IMS_CP_NS_UNNAMED);

			String source = bitstream.getSource();
			if (source != null) {
				if (source.equals(Constants.RELATED_CONTENT_PACKAGE_BUNDLE)) {
					// For related bundles, determine the url, rather than just the handle
					log.debug("Related bitstream found - work out url");
					resource.setAttribute(XMLManifest.HREF_ATTR,
							new StringBuilder(ConfigurationManager.getProperty("dspace.url")).append(File.separator)
									.append(bitstream.getName()).toString());
				} else {
					resource.setAttribute(XMLManifest.HREF_ATTR, bitstream.getName());
				}
			} else {
				resource.setAttribute(XMLManifest.HREF_ATTR, bitstream.getName());
			}

			resource.setAttribute(XMLManifest.IDENTIFIER_ATTR, XMLManifest.RESOURCE_ID_PREFIX + bitstream.getChecksum());
			resource.setAttribute(IMSManifest.TYPE_ATTR, "webcontent");
			resources.addContent(resource);
		}
	}

	/**
	 * Create a new manifest object from a serialized IMS manifest XML document.
	 * Parse document read from the input stream, optionally validating.
	 * 
	 * @param is
	 *            input stream containing serialized XML
	 * @param validate
	 *            if true, enable XML validation using schemas in document. Also
	 *            validates any sub-documents.
	 * @throws MetadataValidationException
	 *             if there is any error parsing or validating the METS.
	 * @return new XLMManifest object.
	 */
	public static XMLManifest create(InputStream is, boolean validate) throws IOException, MetadataValidationException {
		return new IMSManifest(parseManifest(is, validate));
	}

	/**
	 * Gets all <code>resource</code> elements which make up the files stored in
	 * the IMS manifest
	 * 
	 * @return a List of <code>Element</code>s.
	 */
	@Override
	public List<Element> getResources() throws MetadataValidationException {
		if (contentFiles != null)
			return contentFiles;

		Element resourcesSec = manifestRoot.getChild(IMSManifest.RESOURCES_ELEM, IMS_CP_NS);
		if (resourcesSec == null)
			throw new MetadataValidationException("Invalid IMS Manifest: DSpace requires a "
					+ IMSManifest.RESOURCES_ELEM + " element, but it is missing.");

		contentFiles = new ArrayList<Element>();
		Iterator<Element> resourceIter = resourcesSec.getChildren(XMLManifest.RESOURCE_ELEM, IMS_CP_NS).iterator();
		while (resourceIter.hasNext()) {
			Element resourceElem = resourceIter.next();
			contentFiles.add(resourceElem);
		}
		return contentFiles;
	}

	/**
	 * Find the metadata element and return all its child elements as a List.
	 * This list can then be sent to an IngestionCrosswalk class to crosswalk
	 * the metadata.
	 * 
	 * Note: The IMSCP schema defines that 2 additional elements may appear as
	 * children which are not part of the metadata itself. These are "schema"
	 * and "version" - these elements if they appear in the XML doc will not be
	 * returned in this list. The caller can therefore assume that the list
	 * returned is purely the metadata for the content package.
	 * 
	 * @return list of elements within the 'metadata' element
	 * @throws MetadataValidationException
	 *             if 'metadata' element is not found
	 */
	@Override
	public List<Element> getMetadataElements() throws MetadataValidationException {
		Element metadataSec = manifestRoot.getChild(IMSManifest.METADATA_ELEM, IMS_CP_NS);
		if (metadataSec == null)
			throw new MetadataValidationException("Invalid IMS Manifest: DSpace requires a "
					+ IMSManifest.METADATA_ELEM + " element, but it is missing.");

		List<Element> result = new ArrayList<Element>();

		List<Element> children = metadataSec.getChildren();
		for (Element child : children) {
			// check to see if we have the schema or schemaversion element (in
			// the IMS CP namespace!)
			if (child.getNamespace().equals(IMS_CP_NS)) {
				if ((child.getName().compareToIgnoreCase(IMSManifest.SCHEMA_ELEM) == 0)
						|| (child.getName().compareToIgnoreCase(IMSManifest.SCHEMA_VERSION_ELEM) == 0)) {
					continue;
				}
			} else {
				// wasn't schema or schmeaversion element - must be the real
				// metadata so add it to the list
				result.add(child);
			}
		}

		return result;

	}

	private List<Element> getOrganizationElements() throws MetadataValidationException {
		Element organizations = manifestRoot.getChild(IMSManifest.ORGANIZATIONS_ELEM, IMS_CP_NS);
		if (organizations == null)
			throw new MetadataValidationException("Invalid IMS Manifest: DSpace requires a "
					+ IMSManifest.ORGANIZATIONS_ELEM + " element, but it is missing.");

		List<Element> result = new ArrayList<Element>();

		List<Element> children = organizations.getChildren();
		for (Element child : children) {
			// check to see if we have the schema or schemaversion element (in
			// the IMS CP namespace!)
			//			if (child.getNamespace().equals(IMS_CP_NS)) {
			//				if ((child.getName().compareToIgnoreCase(SCHEMA_ELEM) == 0)
			//						|| (child.getName().compareToIgnoreCase(SCHEMA_VERSION_ELEM) == 0)) {
			//					continue;
			//				}
			//			} else {
			// wasn't schema or schmeaversion element - must be the real
			// metadata so add it to the list
			result.add(child);
			//			}
		}

		return result;

	}

	/**
	 * If the metadata format has not been termined previously, the
	 * METADATA_ELEM element is found in the manifest and the first child
	 * element which is not part of the IMS namespace is analysed to determine
	 * its namespace. This is matched against the MetadataFormat enum and the
	 * corresponding match is returned (if one if found).
	 * 
	 * @return the metadata format determined by namespace analysis
	 * @throws MetadataFormatException
	 *             if an unsuported metadata format is found or the metadata
	 *             element can't be found/has no children
	 * @throws MetadataValidationException
	 *             if an error occurred obraining the child elements of the
	 *             metadata element
	 */
	@Override
	public MetadataFormat getMetadataFormat() throws MetadataFormatException, MetadataValidationException {

		if (format == null) {
			// Try and work out the format from the metadata elements
			List<Element> metadataElems = getMetadataElements();
			if (metadataElems.size() > 0) {
				for (MetadataFormat f : MetadataFormat.FORMATS) {
					if (f.isFormat(metadataElems.get(0))) {
						format = f;
						break;
					}
				}

				if (format == null) {
					throw new MetadataFormatException(metadataElems.get(0));
				}
			}

		}

		if (format == null) {
			throw new MetadataFormatException();
		}

		return format;
	}

	/**
	 * Not implemented - we use the entire XML doc for IMS md parsing
	 * 
	 * @see uk.ac.jorum.packager.XMLManifest#getMetadataElements(org.jdom.Element)
	 */
	@Override
	public List<Element> getMetadataElements(Element mdRootNode) throws MetadataValidationException {
		throw new MetadataValidationException("Invalid method call");
	}

	/**
	 * Not implemented - we use the entire XML doc for IMS md parsing
	 * 
	 * @see uk.ac.jorum.packager.XMLManifest#getMetadataFormat(org.jdom.Element)
	 */
	@Override
	public MetadataFormat getMetadataFormat(Element mdRootNode) throws MetadataFormatException,
			MetadataValidationException {
		throw new MetadataValidationException("Invalid method call");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.jorum.packager.XMLManifest#getRootMetadataElementXpathSelector(
	 * uk.ac.jorum.packager.MetadataFormat)
	 */
	@Override
	public String getRootMetadataElementXpathSelector(MetadataFormat mdFormat) throws Exception {
		String result = IMS_ROOT_METADATA_ELEMENT_XPATH;

		// Some metadata formats have a root below the one chosen above e.g. lom
		// and imsmd. Need to isolate this if need be.
		if (mdFormat.getRootMetadataElement() != null) {
			result += "/*[local-name() = '" + mdFormat.getRootMetadataElement() + "' and namespace-uri() = '"
					+ mdFormat.getNamespaceURI() + "']";
		}

		return result;
	}

	@Override
	public void setExportMetadataFormat(String exportMetadataFormat) {
		// TODO Auto-generated method stub
		this.exportMetadataFormat = exportMetadataFormat;
	}

	public static XMLManifest readOriginalManifest(Item item) throws IOException, MetadataValidationException,
			PackageException, SQLException {
		XMLManifest originalManifest = recreateOriginalManifest(item);
		log.debug("Recreated original manifest");
		return originalManifest;
	}

	private static XMLManifest recreateOriginalManifest(Item item) throws IOException, SQLException, PackageException,
			MetadataValidationException {
		InputStream is = null;
		XMLManifest originalManifest = null;
		try {
			is = item.getBundles(Constants.METADATA_BUNDLE_NAME)[0].getBitstreamByName((IMSIngester.MANIFEST_FILE))
					.retrieve();
			//Create a manifest object with the original manifest.
			// Note that the InputStream will be closed in this method
			originalManifest = IMSManifest.create(is, false);
		} catch (AuthorizeException e) {
			e.printStackTrace();
			throw new PackageException("Error: Problem reading item metadata bundle ", e);
		}

		return originalManifest;
	}

	/**
	 * Load original manifest and return it as a jDom document
	 * @param item
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws PackageException
	 * @throws MetadataValidationException
	 */
	@Override
	public Document recreateOriginalManifestDocument(Item item) throws IOException, SQLException, PackageException,
			MetadataValidationException {
		InputStream is = null;
		try {
			is = item.getBundles(Constants.METADATA_BUNDLE_NAME)[0].getBitstreamByName((IMSIngester.MANIFEST_FILE))
					.retrieve();
		} catch (AuthorizeException e) {
			e.printStackTrace();
			throw new PackageException("Error: Problem reading item metadata bundle ", e);
		}
		// Note that InputStream will be closed in parseManifest
		return parseManifest(is, false);
	}
}
