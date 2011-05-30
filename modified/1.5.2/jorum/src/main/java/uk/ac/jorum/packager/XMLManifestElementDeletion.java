/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : XMLManifestElementDeletion.java
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

import org.jdom.Namespace;

/**
 * This class can be used to store information about an element which should be removed from an XML manifest upon ingest.
 * This is required as some packages may contain information int he manifest which is not relevant to be stored in Jorum
 * e.g. the o-ex:rights element which is stored in intraLibrary packages. This would contain licence information which would not
 * be appropriate for open content. If the item is being stored in Jorum then it must have a CC licence and therefore this 
 * extra information in the manifest would not be consistent or accurate.
 *
 * Please note: An instance should contain an xpath expression pointing to a node in the manifest which should be completely earsed from
 *              the manifest. This included any child nodes of the selected node! 
 *
 * @author gwaller
 *
 */
public class XMLManifestElementDeletion {
	
	private MetadataFormat format;
	private String xpath;
	private Namespace[] namespaces;
	
	/**
	 * Constructor to create an instance representing an element(s) to be erased from the manifest file.
	 *
	 * @param format If this is a non-null value, the element to be removed is dependant on a particular MetadataFormat and will
	 * only be removed if the MetadataFormat of the manifest is the same. Also the namespace of the metadata format can used in the 
	 * xpath expression by using the prefix MetadataFormat.JORUM_NAMESPACE_PREFIX
	 * @param xpath The xpath expression to select a node to be removed from the manifest. Note: if this node contains child notdes, they will also be erased.
	 * @param namespaces Array of Namespace instances which are used in the xpath expression, can be null
	 */
	public XMLManifestElementDeletion(MetadataFormat format, String xpath, Namespace[] namespaces){
		this.format = format;
		this.xpath = xpath;
		this.namespaces = namespaces;
	}

	/**
	 * @return the format
	 */
	public MetadataFormat getFormat() {
		return format;
	}

	/**
	 * @return the xpath
	 */
	public String getXpath() {
		return xpath;
	}

	/**
	 * @return the namespaces
	 */
	public Namespace[] getNamespaces() {
		return namespaces;
	}
	
	
	
	
}
