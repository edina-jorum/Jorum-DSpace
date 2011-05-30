package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

public class FrontPageEmailCheck extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	private static Logger log = Logger.getLogger(FrontPageEmailCheck.class);
	
	/** Language Strings */
    
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
    private static final Message T_para1 = 
        message("xmlui.ArtifactBrowser.FrontPageEmailCheck.para1");
	
	
	public Serializable getKey() {
		// Don't bother caching this component
		return null;
	}

	public SourceValidity getValidity() {
		// Since the object isn't currently cachable set this to null also
		return null;
	}
	
	/**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	pageMeta.addMetadata("title").addContent(T_dspace_home);
    	pageMeta.addTrailLink(contextPath, T_dspace_home);
        
        // Add RSS links if available
        String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats.split(","))
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1) 
					continue;
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/site";
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }
    

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        EPerson eperson = context.getCurrentUser();
        if (eperson != null) // Is user logged in?
        {
        	String email = eperson.getEmail();
        	String netid = eperson.getNetid();
        	
        	try {
				if (email.equals(netid.toLowerCase())) // User hasn't submitted an e-mail address yet
				{
					// Render email change request message
					Division emailMessage =  body.addDivision("email-message");
					emailMessage.addPara(T_para1);
				}
			} catch (NullPointerException e) {
				log.info("Password login, so netid is empty.");
			}
        	
        }
    }

}
