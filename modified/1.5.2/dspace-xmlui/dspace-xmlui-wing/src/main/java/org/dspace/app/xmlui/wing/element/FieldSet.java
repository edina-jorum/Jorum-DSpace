package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents a fileset which provides a visual group around related HTML items
 * 
 * @author GWaller
 */

public class FieldSet extends Container implements StructuralElement
{
	 class Legend extends TextContainer implements StructuralElement{

		private static final String ELEMENT_NAME = "legend";
	
		public Legend(WingContext context, String legend) throws WingException
	    {
	        super(context);
	        this.addContent(legend);
	    }
		
		public Legend(WingContext context, Message legend) throws WingException
	    {
	        super(context);
	        this.addContent(legend);
	    }
		
		
		/**
	     * Translate this element and all contained elements into SAX events. The
	     * events should be routed to the contentHandler found in the WingContext.
	     * 
	     * @param contentHandler
	     *            (Required) The registered contentHandler where SAX events
	     *            should be routed too.
	     * @param lexicalHandler
	     *            (Required) The registered lexicalHandler where lexical 
	     *            events (such as CDATA, DTD, etc) should be routed too.
	     * @param namespaces
	     *            (Required) SAX Helper class to keep track of namespaces able
	     *            to determine the correct prefix for a given namespace URI.
	     */
	    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
	            NamespaceSupport namespaces) throws SAXException
	    {
	        startElement(contentHandler, namespaces, ELEMENT_NAME, null);
	        super.toSAX(contentHandler, lexicalHandler, namespaces);
	        endElement(contentHandler, namespaces, ELEMENT_NAME);
	    }

	    /**
	     * dispose
	     */
	    public void dispose()
	    {
	        super.dispose();
	    }
		
	}
	
	
    /** The name of the head element */
    public static final String ELEMENT_NAME = "fieldset";
    private Legend legend;
    private String cssClass;
    

    /**
     * Construct a new fieldset.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param legend
     *            (May be null) a string representing the legend which should be displayed
     * @param cssClass
     *            (May be null) a string representing the css class to write as an attribute to the element
     */
    public FieldSet(WingContext context, String legend, String cssClass) throws WingException
    {
        super(context);
        this.cssClass = cssClass;
        this.legend = new Legend(context, legend);
        contents.add(this.legend);
    }
    
    /**
     * Construct a new fieldset.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param legend
     *            (May be null) a string representing the legend which should be displayed
     * @param cssClass
     *            (May be null) a string representing the css class to write as an attribute to the element
     */
    public FieldSet(WingContext context, Message legend, String cssClass) throws WingException
    {
        super(context);
        this.cssClass = cssClass;
        this.legend = new Legend(context, legend);
        contents.add(this.legend);
        
    }
    
    public void addWingElement(AbstractWingElement e){
    	contents.add(e);
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     */
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
            NamespaceSupport namespaces) throws SAXException
    {
    	
    	AttributeMap attributes = new AttributeMap();
        if (this.cssClass != null)
        {
            attributes.put("class", this.cssClass);
        }
    	
        startElement(contentHandler, namespaces, ELEMENT_NAME, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, ELEMENT_NAME);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }

}
