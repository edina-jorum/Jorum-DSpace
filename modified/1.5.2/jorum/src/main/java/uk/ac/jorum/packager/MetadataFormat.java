/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : MetadataFormat.java
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


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.jdom.Attribute;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.IllegalAddException;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import uk.ac.jorum.utils.ExceptionLogger;
import uk.ac.jorum.utils.URLChecker;

/**
 * @author gwaller
 * 
 */
public class MetadataFormat {

	public final static MetadataFormat IMSMDV1P2 = new MetadataFormat(
			"IMSMD",
			"lom",
			"http://www.imsglobal.org/xsd/imsmd_v1p2", 
			"//jfoo:rights/jfoo:description/jfoo:langstring",
			"rights/description/langstring",
			"//jfoo:classification/jfoo:taxonpath/jfoo:taxon/jfoo:entry/jfoo:langstring",
			"//jfoo:technical/jfoo:location"
			);

	public final static MetadataFormat LOM = new MetadataFormat(
			"LOM", 
			"lom",
			"http://ltsc.ieee.org/xsd/LOM",
			"//jfoo:rights/jfoo:description/jfoo:string", 
			"rights/description/string",
			"//jfoo:classification[jfoo:purpose/jfoo:value=\"discipline\"]/jfoo:taxonPath/jfoo:taxon/jfoo:entry/jfoo:string",
			"//jfoo:technical/jfoo:location"
			);

	public final static MetadataFormat DC = new MetadataFormat(
			"DC", 
			null,
			"http://purl.org/dc/elements/1.1/",
			"//jfoo:rights",
			"rights", 
			"//jfoo:subject",
			"//jfoo:identifier");
	
	public final static MetadataFormat QDC = new MetadataFormat(
			"QDC", 
			"qualifieddc",
			"http://purl.org/dc/terms/",
			"//jbar:rights[@xsi:type=\"dcterms:URI\"]",
			"rights", 
			"//jfoo:subject",
			"//jfoo:identifier");

	public static MetadataFormat FORMATS[] = {IMSMDV1P2, LOM, DC, QDC};
	
	/** log4j category */
	private static Logger log = Logger.getLogger(MetadataFormat.class);

	/** This is the prefix that will be used for elements belongin to the namespace of the metadata format e.g refer to 
	 * jfoo:rights for DC would mean the rights element belonging to the namespace http://purl.org/dc/elements/1.1/
	 */
	public final static String JORUM_NAMESPACE_PREFIX = "jfoo";

	
	public static final String PI_HREF_ATTR = "href";
	private String dspaceConfigStr;
	private String namespaceURI;
	private String licenceXpathExpression;
	private String classificationXpath;
	private String licenceElementsToCreateFromRootMetadataElement;
	private String rootMetadataElement;
	// GWaller 6/5/10 IssueID#263 Support for web links not in a manifest resource element
	private String webLinkIdentifierXpath;
	
	/**
	 * Construxctor for a MetadataFormat instance
	 * @param dspaceConfigStr
	 * @param rootMetadataElementName set to null if a "root" node for the metadata block is not required e.g. in the case of DC. Otherwise set to
	 * the name of the element which should be the root of the metadata XML block e.g. "lom" for LOM metadata
	 * @param namespaceURI
	 * @param licenceXpathExpression
	 * @param licenceElementsToCreateFromRootMetadataElement
	 * @param classificationXpath
	 */
	private MetadataFormat(String dspaceConfigStr, 
						   String rootMetadataElementName,
						   String namespaceURI, 
						   String licenceXpathExpression,
						   String licenceElementsToCreateFromRootMetadataElement, 
						   String classificationXpath,
						   String webLinkIdentifierXpath) {
		this.dspaceConfigStr = dspaceConfigStr;
		this.namespaceURI = namespaceURI;
		this.licenceXpathExpression = licenceXpathExpression;
		this.classificationXpath = classificationXpath;
		this.licenceElementsToCreateFromRootMetadataElement = licenceElementsToCreateFromRootMetadataElement;
		this.rootMetadataElement = rootMetadataElementName;
		// GWaller 6/5/10 IssueID#263 Support for web links not in a manifest resource element
		this.webLinkIdentifierXpath = webLinkIdentifierXpath;
	}

	public XPath getXpathInstanceWithNamespace(String expr) throws JDOMException {
		XPath xpathIns = XPath.newInstance(expr);
		xpathIns.addNamespace(JORUM_NAMESPACE_PREFIX, this.namespaceURI);
		xpathIns.addNamespace(DisseminationCrosswalk.XSI_NS);
		return xpathIns;
	}

	/**
	 * @return the dspaceConfigStr
	 */
	public String getDspaceConfigStr() {
		return dspaceConfigStr;
	}

	/**
	 * @return the namespaceURI
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	public boolean isFormat(Element elem) {
		return elem.getNamespaceURI().compareToIgnoreCase(this.namespaceURI) == 0;
	}

	/**
	 * @return the licenceXpathExpression
	 */
	public String getLicenceXpathExpression() {
		return licenceXpathExpression;
	}

	/**
	 * @return the licenceElementsToCreateFromRootMetadataElement
	 */
	public String getLicenceElementsToCreateFromRootMetadataElement() {
		return licenceElementsToCreateFromRootMetadataElement;
	}

	/**
	 * @return the classificationXpath
	 */
	public String getClassificationXpath() {
		return classificationXpath;
	}

	
	
	/**
	 * @return the rootMetadataElement
	 */
	public String getRootMetadataElement() {
		return rootMetadataElement;
	}

	private String stringFromXpathResult(Object xpathResult) {
		String result = null;
		if (xpathResult instanceof Element) {
			// Element slected via xpath - crudely just get the text below this
			// node. To be more accurate, the xpath should select an attribute
			// or TEXT node
			result = ((Element) xpathResult).getTextTrim();
		} else if (xpathResult instanceof Attribute) {
			// More accurate Xpath - simply return the value
			result = ((Attribute) xpathResult).getValue();
		} else if (xpathResult instanceof Text) { // Will also catch CDATA
													// (subclass of Text)
			// More accurate Xpath - simply return the text with whitespace
			// trimmed
			result = ((Text) xpathResult).getTextTrim();
		} else if (xpathResult instanceof Comment) {
			// More accurate Xpath - simply return the text
			result = ((Comment) xpathResult).getText();
		} else if (xpathResult instanceof ProcessingInstruction) {
			// Support a PI with a href attribute pointing to the licence
			result = ((ProcessingInstruction) xpathResult).getPseudoAttributeValue(PI_HREF_ATTR);
		} else {
			// Fall back - just call toString
			result = xpathResult.toString();
		}

		return result;
	}

	// START GWaller 02/02/09 IssueID #175 Added methods to deal with licence
	// manipulation inside packages
	private void setStringInXpathResult(Object xpathResult, String value) throws MetadataValidationException {
		if (xpathResult instanceof Element) {
			// Element selected via xpath
			((Element) xpathResult).setText(value);
		} else if (xpathResult instanceof Attribute) {
			// More accurate Xpath selecting an attribute
			((Attribute) xpathResult).setValue(value);
		} else if (xpathResult instanceof Text) { // Will also catch CDATA
													// (subclass of Text)
			// More accurate Xpath - simply set the text with the value supplied
			((Text) xpathResult).setText(value);
		} else if (xpathResult instanceof Comment) {
			// More accurate Xpath - comment selected
			((Comment) xpathResult).setText(value);
		} else if (xpathResult instanceof ProcessingInstruction) {
			// Support a PI by using the href attribute
			((ProcessingInstruction) xpathResult).setPseudoAttribute(PI_HREF_ATTR, value);
		} else {
			// Unsupported JDOM element selected - need to throw an exception
			throw new MetadataValidationException("Attempting to set value (" + value
					+ ") of an unsupported JDOM Element in manifest. Element is: " + xpathResult.toString());
		}

	}

	public void setLicenceText(Document manifest, String licenceText, String rootMdElementXpath,
			String metadataElementPrefix) throws MetadataValidationException {
		try {
			Object licenceNode = getXpathInstanceWithNamespace(this.licenceXpathExpression).selectSingleNode(manifest);
			if (licenceNode == null) {
				// Need to create it!

				// Get the root metadata element to add to
				Object rootMDElement = getXpathInstanceWithNamespace(rootMdElementXpath).selectSingleNode(manifest);
				if (rootMDElement != null && rootMDElement instanceof Element) {
					Element root = (Element) rootMDElement;
					Namespace ns = Namespace.getNamespace(metadataElementPrefix, this.namespaceURI);

					try {
						manifest.getRootElement().addNamespaceDeclaration(ns);
					} catch (IllegalAddException e) {
						// thrown if a prefix clash happens - don't need to
						// worry. This just means the namespace was there
						// already!
					}

					String[] childrenToCreate = (this.getLicenceElementsToCreateFromRootMetadataElement()).split("/");
					for (int i = 0; i < childrenToCreate.length; i++) {
						Element child = root.getChild(childrenToCreate[i], ns);
						if (child == null) {
							// Child not found - need to create it
							child = new Element(childrenToCreate[i], ns);
							root.addContent(child);
						}

						// We no should have a valid child pointer and this will
						// be come the root so we can create the other children
						// off it
						root = child;
					}

					// root should now point to the last element which should
					// contain the licence. Set the licenceNode pointer and let
					// setStringInXpathResult so the work of setting the licence
					// in the element
					licenceNode = root;

				} else {
					throw new MetadataValidationException("Could not find root metadata element using XPath: "
							+ rootMdElementXpath);
				}

			}

			// Now set the licence text in the relevant node
			this.setStringInXpathResult(licenceNode, licenceText);

		} catch (JDOMException e) {
			ExceptionLogger.logException(log, e);
			// Wrap the exception in a MEtadataValidationException and throw
			throw new MetadataValidationException(e);
		}
	}
	// END GWaller 02/02/09 IssueID #175 Added methods to deal with licence
	// manipulation inside packages

	// GWaller 19/2/10 IssueID #199 Changed param to Object - could be an Element or Document
	public String getTextByXPath(Object rootForXpath, String xpathExperssion) throws MetadataValidationException {
		String licence = null;
		Object context = rootForXpath;
		
		
		log.debug("getTextByXPath: xpathExperssion= " + xpathExperssion);
		if (xpathExperssion == null) {
			return null;
		}

		if (rootForXpath != null && rootForXpath instanceof Element){
			 XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
	            log.debug("getTextByXPath: Running xpath expression using the following Element as context :");
	            log.debug(outputPretty.outputString((Element)rootForXpath));
	            context = new Document((Element)((Element)rootForXpath).clone());
		}
	
		try {
			XPath xpathIns = getXpathInstanceWithNamespace(xpathExperssion);
			if(this.dspaceConfigStr.equals("QDC")){
				// bit of a hack to add elements namespace for QDC metadata
				// Note that this namespace not declared in constructor as wouldn't be able
				// to differentiate from normal DC, so http://purl.org/dc/terms/ declared there.
				xpathIns.addNamespace("jbar","http://purl.org/dc/elements/1.1/");
			}
			Object licenceValue = xpathIns.selectSingleNode(context);

			// Now check the object returned - can be Element, Attribute, Text,
			// CDATA, Comment, ProcessingInstruction,
			// Boolean, Double, String, or null if no item was selected

			if (licenceValue != null) {
				licence = stringFromXpathResult(licenceValue);
			}

		} catch (JDOMException e) {
			ExceptionLogger.logException(log, e);
			// Wrap the exception in a MEtadataValidationException and throw
			throw new MetadataValidationException(e);
		}

		log.debug("getTextByXPath: result = <" + licence + ">");

		return licence;
	}

	public ArrayList<String> getAllTextByXPath(Document manifest, String xpathExperssion) throws MetadataValidationException {
		ArrayList<String> values = new ArrayList<String>();

		log.debug("getAllTextByXPath: xpathExperssion= " + xpathExperssion);
		if (xpathExperssion == null) {
			return null;
		}

		try {
			List<Object> valueList = getXpathInstanceWithNamespace(xpathExperssion).selectNodes(manifest);

			// Now check the object returned - can be Element, Attribute, Text,
			// CDATA, Comment, ProcessingInstruction,
			// Boolean, Double, String, or null if no item was selected

			if (valueList != null && valueList.size() > 0) {

				for (Object o : valueList) {
					values.add(stringFromXpathResult(o));
				}

			}

		} catch (JDOMException e) {
			ExceptionLogger.logException(log, e);
			// Wrap the exception in a MEtadataValidationException and throw
			throw new MetadataValidationException(e);
		}

		log.debug("getAllTextByXPath: result has " + values.size() + " entries");

		return values;
	}

	// GWaller 19/2/10 IssueID #199 Changed param to Object - could be an Element or Document
	public String getLicenceText(Object rootForXpath) throws MetadataValidationException {
		return this.getTextByXPath(rootForXpath, this.licenceXpathExpression);
	}

	public String[] geClassificationText(Document manifest) throws MetadataValidationException {
		ArrayList<String> values = this.getAllTextByXPath(manifest, this.classificationXpath);
		return values.toArray(new String[values.size()]);
	}
	
	// GWaller 6/5/10 IssueID#263 Support for web links not in a manifest resource element
	public String[] geWebLinksFromMetadata(Document manifest) throws MetadataValidationException {
		ArrayList<String> values =  this.getAllTextByXPath(manifest, this.webLinkIdentifierXpath);
		
		// Iterate across the possible list of links and check it is a real URL
		ArrayList<String> checkedLinks = new ArrayList<String>();
		for (String v:values){
			String trimmed = v.trim();
			if (URLChecker.isURL(trimmed) > 0){
				checkedLinks.add(trimmed);
			}
			
		}
		
		return checkedLinks.toArray(new String[checkedLinks.size()]);
		
	}
}
