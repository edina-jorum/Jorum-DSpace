package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

/**
 * Create a simple Ediauth error page.
 * 
 * 
 * @author Ian Fieldhouse
 */

public class EdiauthError extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	/** language strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.EdiauthError.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail = 
        message("xmlui.ArtifactBrowser.EdiauthError.trail");
    
    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.EdiauthError.head");
    
    private static final Message T_para1 =
        message("xmlui.ArtifactBrowser.EdiauthError.para1");
	
	
	/**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() 
    {
       return "1";
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity() 
    {
        return NOPValidity.SHARED_INSTANCE;
    }
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
    WingException, UIException, SQLException, IOException,
    AuthorizeException
    {
    	pageMeta.addMetadata("title").addContent(T_title);

    	pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
    	pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException
    {
    	Division div = body.addDivision("ediauth-error","primary");

    	div.setHead(T_head);

    	String name = ConfigurationManager.getProperty("dspace.name");
    	div.addPara(T_para1.parameterize(name));
    }

}
