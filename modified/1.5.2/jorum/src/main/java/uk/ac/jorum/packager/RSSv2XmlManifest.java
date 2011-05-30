/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : RSSv2XmlManifest.java
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageException;
import org.dspace.core.PluginInstantiationException;
import org.jdom.Document;
import org.jdom.Element;


/**
 * @author gwaller
 * 
 */
public class RSSv2XmlManifest extends XMLManifest {

	private static Logger log = Logger.getLogger(RSSv2XmlManifest.class);
	
	public static final String RSS_ITEM_ELEM = "item";
	public static final String RSS_LINK_ELEM = "link";
	public static final String RSS_CHANNEL_ELEM = "channel";
	public static final String RSS_TITLE_ELEM = "title";
	
	public RSSv2XmlManifest(Document doc) {
		super(doc);
	}


	/**
	 * Not implemented - elements are the individual RSS item blocks
	 * 
	 * @see uk.ac.jorum.packager.XMLManifest#getMetadataElements()
	 */
	@Override
	public List<Element> getMetadataElements() throws MetadataValidationException {
		throw new MetadataValidationException("Invalid method call");
	}

	/**
	 * Not implemented - md format is gathered from individual RSS item blocks
	 * 
	 * @see uk.ac.jorum.packager.XMLManifest#getMetadataFormat()
	 */
	@Override
	public MetadataFormat getMetadataFormat() throws MetadataFormatException, MetadataValidationException {
		throw new MetadataValidationException("Invalid method call");
	}

	public MetadataFormat getMetadataFormat(Element root) throws MetadataFormatException, MetadataValidationException {

		// Try and work out the format from the metadata elements
		List<Element> metadataElems = getMetadataElements(root);
		if (metadataElems.size() > 0) {
			
			for (Element mdElem:metadataElems){
				
				log.debug("Attempting to determing metadata format from element:" + mdElem.getName());
				
				for (MetadataFormat f : MetadataFormat.FORMATS) {
					if (f.isFormat(mdElem)) {
						format = f;
						break;
					}
				}
				
				if (format != null){
					// got a match - break the loop
					break;
				}
			}
			
			

			if (format == null) {
				throw new MetadataFormatException(metadataElems.get(0));
			}
		}


		if (format == null) {
			throw new MetadataFormatException();
		}

		return format;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.jorum.packager.XMLManifest#getResources()
	 */
	@Override
	public List<Element> getResources() throws MetadataValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Element> getMetadataElements(Element root) throws MetadataValidationException {
		List<Element> result;
		// The metadata for an item is the child elements under the item
		if (root.getName().equals(RSS_ITEM_ELEM)){
			result = root.getChildren();
		} else { // we may have been passed the root node - the md for the whole RSS doc is the child nodes excluding the child items 
			// return all non "item" child elements 
			List<Element> children = root.getChildren();
			ArrayList<Element> arrList = new ArrayList<Element>();
			for (Element c:children){
				if (! c.getName().equals(RSS_ITEM_ELEM)){
					arrList.add(c);
				}
			}
			result = arrList;
		}
		
		log.debug("Number of metadata elements returned is: " + result.size());
		
		return result;
	}


	/**
	 * Doesn't make sense to support getRootMetadataElementXpathSelector for the RSS ingester.
	 * This is used by "setLicenceInPackage" in the PackageIngester. As RSS is not a content package
	 * and more importantly it would contain multiple items it doesn't make sense to have an xpath expression
	 * to the root metadata element. In theory this could be for the "<channel>" item as this contains metadata
	 * for the entire feed but what about the individual feed items? 
	 */
	@Override
	public String getRootMetadataElementXpathSelector(MetadataFormat mdFormat) throws Exception {
		throw new Exception("Invalid method call");
	}


	@Override
	public void createNewManifest() throws PackageException, PluginInstantiationException, CrosswalkException,
			IOException, SQLException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void reconstructManifest(Document originalManifest) throws PackageException, PluginInstantiationException, CrosswalkException,
			IOException, SQLException {
		// TODO Auto-generated method stub
	
	}


	@Override
	public void setExportMetadataFormat(String exportMetadataFormat) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Document recreateOriginalManifestDocument(Item item) throws IOException, SQLException, PackageException,
			MetadataValidationException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
}
