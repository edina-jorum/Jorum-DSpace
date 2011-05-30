/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : IMSHtmlPreviewGenerator.java
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
package uk.ac.jorum.packager.preview;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.packager.IMSManifest;
import uk.ac.jorum.packager.XMLManifest;
import uk.ac.jorum.utils.ExceptionLogger;


/**
 * @author gwaller
 *
 */
@SuppressWarnings("deprecation")
public class IMSHtmlPreviewGenerator {

	/** log4j logger */
	private static Logger logger = Logger.getLogger(IMSHtmlPreviewGenerator.class);
	
	public static final String VELOCITY_TEMPLATE_DIR = "velocity_templates/";
	public static final String PREVIEW_LEFT_FRAME_TEMPLATE = VELOCITY_TEMPLATE_DIR + "IMSPackagePreview_left_frame.vm";
	public static final String NODE_TEMPLATE = VELOCITY_TEMPLATE_DIR + "node.vm";
	
	public static final String ITEM_TITLE = "itemTitle";
	public static final String TREE_ROOT = "treeRoot";
	public static final String URL_BITSTREAMS = "urlBitstreams";
	public static final String BITSTREAM_HANDLE_URL = "bitStreamHandleUrl";
	public static final String HANDLE_URL = "handleUrl";
	public static final String NODE_VTL = "nodeVTL";
	public static final String NODE = "node";
	public static final String FIRST_BITSTREAM = "firstBitstream";
	
	private static VelocityEngine ve;

	static{
		/*
	     *  now create a new VelocityEngine instance, and
	     *  configure it to use the category
	     */

	    ve = new VelocityEngine();

	    ve.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
	      "org.apache.velocity.runtime.log.Log4JLogChute" );

	    ve.setProperty("runtime.log.logsystem.log4j.logger", IMSHtmlPreviewGenerator.class.getCanonicalName());

	    // Set a classpath loader
	    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "previewloader");
	    ve.setProperty("previewloader.resource.loader.class", ClasspathResourceLoader.class.getCanonicalName());
	    ve.setProperty("velocimacro.library", VELOCITY_TEMPLATE_DIR + "VM_global_library.vm");
	    
	    try{
	    	ve.init();
	    } catch (Exception e){
	    	ExceptionLogger.logException(logger, e);
	    	ve = null;
	    }
	}
	
	private static String getDspaceUrl() throws Exception{
		// Is there a better way to get the Dspace URL - what if we have the JSP and XMLUI running??
		String dspaceUrl = ConfigurationManager.getProperty("dspace.url");
		if (dspaceUrl == null){
			// Error - can't generate preview!
			throw new Exception("Config property dspace.url is null");
		}
		
		return dspaceUrl;
	}
	
	private static String generateNodeTemplate(PreviewTreeNode node, String handleUrl) throws Exception{
		String result = "";
		VelocityContext context = new VelocityContext();
		Template nodeTemplate = ve.getTemplate(NODE_TEMPLATE);
		
		context.put(BITSTREAM_HANDLE_URL, handleUrl);
		context.put(NODE, node);
		
		if (node.hasChildren()){
			String nodeVtl = "";
			for (PreviewTreeNode child:node.getChildren()){
				nodeVtl += "\n" + generateNodeTemplate(child, handleUrl);
			}
			context.put(NODE_VTL, nodeVtl);
		} 
		
		StringWriter w = new StringWriter();
		nodeTemplate.merge(context, w);
		result = w.toString();
		
		return result;
	}
	
	
	private static VelocityContext buildVelocityContext(Item item) throws Exception{
		VelocityContext context = new VelocityContext();

		String title = "";
		DCValue[] titleValues = item.getDC("title", Item.ANY, Item.ANY);
		if (titleValues.length > 0){
			title = titleValues[0].value;
		}
		context.put(ITEM_TITLE, title);
        
		String bitstreamHandleUrl = getDspaceUrl() + "/bitstream/handle/" + item.getHandle();
		context.put(BITSTREAM_HANDLE_URL, bitstreamHandleUrl);
		
		String handleUrl = getDspaceUrl() + "/handle/" + item.getHandle();
		context.put(HANDLE_URL, handleUrl);
		
		// Create root tree node
		PreviewTreeNode rootNode = new PreviewTreeNode();
		rootNode.setNodeName(title);
		rootNode.setBitStream(null);
		rootNode.setFirst(0);
		
		/*
		 *  We have a content package - need to read original manifest and inspect resources and organizations
		 */
		IMSManifest imsManifest = null;
		try {
			imsManifest = (IMSManifest) IMSManifest.readOriginalManifest(item);
			
			/*
			 *  Create a map of the resource element 'identifier' and 'href' values
			 */
			Map<String, String> resourceMap = new HashMap<String, String>();
			for (Element resource : imsManifest.getResources()) {
				String identifierAtt = resource.getAttribute(XMLManifest.IDENTIFIER_ATTR).getValue();
				String hrefAtt = resource.getAttribute(XMLManifest.HREF_ATTR).getValue();
				logger.debug("Get resource attributes: Iden_ref=" + identifierAtt + ", href=" + hrefAtt);
				resourceMap.put(identifierAtt, hrefAtt);
			}
			
			/*
			 *  Determine organization elements from from manifest and iterate through them
			 */
			// Retrieve 'organizations' element
			Element organizationsElement   = imsManifest.getManifestDocument().getRootElement().getChild(IMSManifest.ORGANIZATIONS_ELEM, IMSManifest.IMS_CP_NS);
			// Retrieve all 'organization' elements for the manifest 'organization'
			@SuppressWarnings("unchecked")
			List<Element> organizationList = (List<Element>) organizationsElement.getChildren(IMSManifest.ORGANIZATION_ELEM, IMSManifest.IMS_CP_NS);
			for (Iterator<Element> orgIterator = organizationList.iterator(); orgIterator.hasNext();) 
			{
				Element organizationElement = orgIterator.next();
				@SuppressWarnings("unchecked")
				List<Element> itemsList = (List<Element>) organizationElement.getChildren(IMSManifest.ITEM_ELEM,IMSManifest.IMS_CP_NS);
				for (Element itemElement : itemsList)
				{
					rootNode = createItemNode(rootNode, rootNode, itemElement, item, resourceMap, context);
				}
			}
		} catch (Exception e) {
			logger.error("Problem reading in original IMS manifest file.", e);
		}
		
		logger.debug("Contents of rootNode=" + rootNode);
		context.put(TREE_ROOT, rootNode);
		
		String vtl = generateNodeTemplate(rootNode, bitstreamHandleUrl);
		context.put(NODE_VTL, vtl);
		
		// populate urls array
		ArrayList<Bitstream> urlBitstreams = new ArrayList<Bitstream>(20); 
		Bundle[] urlBundles = item.getBundles(Constants.URL_BUNDLE);
		for (Bundle b : urlBundles){
			Bitstream[] streams = b.getBitstreams();
			for (Bitstream s:streams){
				urlBitstreams.add(s);
			}
		}
		context.put(URL_BITSTREAMS, urlBitstreams);
		
		return context;
	}

	private static PreviewTreeNode createItemNode(PreviewTreeNode rootNode,
			PreviewTreeNode parentNode,
			Element itemElement, 
			Item item, 
			Map<String, String> resourceMap, 
			VelocityContext context){
		// Check whether 'item' element has the 'isvisible' attribute
		String isVisible = "false";
		if (itemElement.getAttributeValue(IMSManifest.IS_VISIBLE_ATTR) == null) 
		{
			// No 'isvisible' attribute set so default to 'true'
			isVisible = "true";
		} else {
			isVisible = itemElement.getAttributeValue(IMSManifest.IS_VISIBLE_ATTR);
		}
		
		// When 'isvisible is 'true' we need to create a tree node
		if (isVisible.equals("true"))
		{
			// Create new tree node for this 'item' and set it's name
			PreviewTreeNode itemNode = 	new PreviewTreeNode();
			if (itemElement.getChildText(IMSManifest.TITLE_ELEM, IMSManifest.IMS_CP_NS) != null)
			{
				// 'title' element exists for this 'item' so set the name of this tree node to the value of 'title' element
				itemNode.setNodeName(itemElement.getChildText(IMSManifest.TITLE_ELEM, IMSManifest.IMS_CP_NS));
			}
			else
			{
				// No 'title' element exists for this 'item' so set the name of this tree node to the generic name 'Item'
				itemNode.setNodeName("Item");
			}
			
			// Check whether 'item' has an 'identifierref' attribute and if so set the correct bitstream
			if (itemElement.getAttribute(IMSManifest.IDENTIFIER_REF_ATTR) != null)
			{
				// Determine the current element's Bitstream name
				String bsName = resourceMap.get(itemElement.getAttribute(IMSManifest.IDENTIFIER_REF_ATTR).getValue());
				logger.debug("Current item element's Bitstream name: " + bsName );
				
				// Determine Content Bundle Bitstreams
				Bundle[] contentBundle = null;
				try {
					contentBundle = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
					Bitstream[] contentBitstreams = null;
					
					for (Bundle bundle : contentBundle) {
						contentBitstreams = bundle.getBitstreams();
						// Iterate over bitstreams 
						for(Bitstream bs : contentBitstreams)
						{
							// If item element's Bitstream name matches the Bitstream name then set the node  Bitstream
							if (bs.getName().equals(bsName))
							{
								itemNode.setBitStream(bs);
								int first = rootNode.getFirst();
								if ( first == 0)
								{
									context.put(FIRST_BITSTREAM, bs);
									first++;
									rootNode.setFirst(first);
								}
							}
						}
					}
				} catch (SQLException e) {
					logger.error("Error: Problem content bundle - no point proceeding with this item.", e);
				}
			}
			
			// Create 'item' elements that are children of this 'item' element
			@SuppressWarnings("unchecked")
			List<Element> childItemList = (List<Element>) itemElement.getChildren(IMSManifest.ITEM_ELEM,IMSManifest.IMS_CP_NS);
			for (Element childItem : childItemList) 
			{
				itemNode = createItemNode(rootNode, itemNode, childItem, item, resourceMap, context);
			}
			// Add this item node to it's parent node
			parentNode.addChild(itemNode);
		}
		return parentNode;
	}

	public static Bitstream renderPage(Context c, 
								  Item item, 
								  Bundle previewBundle, 
								  VelocityContext veContext,
								  String templateName,
								  String bitstreamName,
								  boolean markAsPrimary) throws Exception{
		StringWriter w = new StringWriter();
        Template t = ve.getTemplate(templateName);
        t.merge(veContext, w );
        
        // Now store the merged template in a bitstream
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(c, "HTML");
		
        // GWaller 29/1/10 IssueID #170 - delete the preview stream if it already exists. We might be re-generating previews.
        Bitstream previewStream = previewBundle.getBitstreamByName(bitstreamName);
        if (previewStream != null){
        	previewBundle.removeBitstream(previewStream);
        }
        
		return BundleUtils.setBitstreamFromBytes(previewBundle, bitstreamName, bs_format, w.toString().getBytes(), markAsPrimary);
	}
	
	public static Bitstream renderLeftFrame(Context c, Item item, Bundle previewBundle, VelocityContext veContext) throws Exception{
		return renderPage(c, item, previewBundle, veContext, PREVIEW_LEFT_FRAME_TEMPLATE, Constants.PREVIEW_PACKAGE_BITSTREAM, true);
	}
	
	@SuppressWarnings("unused")
	public static void generatePreviewBitstream(Context c, Item item, Bundle previewBundle) throws Exception{
		if (ve != null){
			VelocityContext veContext = buildVelocityContext(item);
			
			Bitstream leftFrame = renderLeftFrame(c, item, previewBundle, veContext);
			
		} else {
			throw new Exception("Velocity not initialised");
		}
	}
}