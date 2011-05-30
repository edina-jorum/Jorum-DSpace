/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : PreviewTreeNode.java
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

import java.util.ArrayList;
import org.dspace.content.Bitstream;


/**
 * @author gwaller
 *
 */
public class PreviewTreeNode {

	String nodeName;
	ArrayList<PreviewTreeNode> children;
	Bitstream bitStream;
	private int first = 0;
	
	public PreviewTreeNode(){
		children = new ArrayList<PreviewTreeNode>();
	}
	
	public void addChild(PreviewTreeNode n){
		children.add(n);
	}

	
	
	/**
	 * @return the bitStream
	 */
	public Bitstream getBitStream() {
		return bitStream;
	}

	/**
	 * @param bitStream the bitStream to set
	 */
	public void setBitStream(Bitstream bitStream) {
		this.bitStream = bitStream;
	}

	public PreviewTreeNode findOrCreateNode(String path){
		PreviewTreeNode result = null;
		
		String childToFind = path;
		String remainder = null;
		int slashPos = path.indexOf("/");
		if (slashPos > -1){
			childToFind = path.substring(0, slashPos);
			try{
				remainder = path.substring(slashPos + 1);
			}catch (IndexOutOfBoundsException e){
				remainder = null;
			}
		} 
		
		// Now find child if it exists
		for (PreviewTreeNode c : children){
			if (c.getNodeName().equals(childToFind)){
				result = c;
			}
		}
		
		if (result == null){
			// didn't find child - create a new node
			result = new PreviewTreeNode();
			result.setNodeName(childToFind);
			this.addChild(result);
		} 
		
		// Now create nodes for the path remainder if necessary
		if (remainder != null){
			result = result.findOrCreateNode(remainder);
		}
			
		
		
		return result;
	}
	
	
	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
	 * @return the children
	 */
	public ArrayList<PreviewTreeNode> getChildren() {
		return children;
	}
	
	public boolean hasChildren(){
		return children.size() > 0;
	}

	/**
	 * @return the first
	 */
	public int getFirst() {
		return first;
	}

	/**
	 * @param first the first to set
	 */
	public void setFirst(int first) {
		this.first = first;
	}
	
}
