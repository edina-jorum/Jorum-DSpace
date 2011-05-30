/*
 * Help.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 18:02:24 +0100 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

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
 * This class represents help instructions for a single field.
 * 
 * @author Scott Phillips
 */
public class HelpLink extends Container implements StructuralElement
{
	class HelpText extends TextContainer implements StructuralElement{

		private static final String ELEMENT_NAME = "helpText";
	
		public HelpText(WingContext context, String text) throws WingException
	    {
	        super(context);
	        this.addContent(text);
	    }
		
		public HelpText(WingContext context, Message text) throws WingException
	    {
	        super(context);
	        this.addContent(text);
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
	
	class Link extends TextContainer implements StructuralElement{

		private static final String ELEMENT_NAME = "link";
	    public static final String A_LINK = "href";
	    
		private String link;
		
		public Link(WingContext context, String altText, String link) throws WingException
	    {
	        super(context);
	        this.addContent(altText);
	        this.link = link;
	    }
		
		public Link(WingContext context, Message altText, String link) throws WingException
	    {
	        super(context);
	        this.addContent(altText);
	        this.link = link;
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
	        attributes.put(A_LINK, this.link);
	    	
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
	
	
	
	
    /** The name of the help element */
    public static final String E_HELP_LINK = "helpLink";
    

    /**
     * Construct a new field help.
     * 
     * @param context
     *            (Required) The context this element is contained in
     */
    protected HelpLink(WingContext context) throws WingException
    {
        super(context);
    }

    public void addWingElement(AbstractWingElement e){
    	contents.add(e);
    }
    
    
    public void addText(String text) throws WingException{
    	HelpText helpText = new HelpText(this.context, text);
    	contents.add(helpText);
    }
    
    public void addText(Message text) throws WingException{
    	HelpText helpText = new HelpText(this.context, text);
    	contents.add(helpText);
    }
    
    public void addLink(String altText, String link) throws WingException{
    	Link linkElem = new Link(this.context, altText, link);
    	contents.add(linkElem);
    }
    
    public void addLink(Message altText, String link) throws WingException{
    	Link linkElem = new Link(this.context, altText, link);
    	contents.add(linkElem);
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
        startElement(contentHandler, namespaces, E_HELP_LINK, null);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_HELP_LINK);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        super.dispose();
    }

}
