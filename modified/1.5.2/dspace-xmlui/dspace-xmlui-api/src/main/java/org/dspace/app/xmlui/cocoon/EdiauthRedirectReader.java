package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.xml.sax.SAXException;

public class EdiauthRedirectReader extends AbstractReader implements Recyclable {

	private static Logger log = Logger.getLogger(EdiauthRedirectReader.class);
    
    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    /** The redirect's mime-type */
    protected String redirectMimeType = "text/plain";
    
    /** The user's session id */
    protected String session_id = null;

    /** The url of the dspace install */
    private String dspaceUrl;

	private String redirect_url;
    
    /**
     * Set up the bitstream reader.
     *
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);

        this.request = ObjectModelHelper.getRequest(objectModel);
        this.response = ObjectModelHelper.getResponse(objectModel);
        this.dspaceUrl = ConfigurationManager.getProperty("proxy.url");
           
        // Get our parameter that identifies type of sitemap (default to HTML sitemap)
        try {
			this.session_id = par.getParameter("sid");
			log.info("Session id present: " + session_id);
			this.redirect_url = dspaceUrl + "/ediauth-login?sid=" + session_id;
		} catch (ParameterException e) {
			log.info("No session id set");
			this.redirect_url = "";
		}
    }
    
    
    /**
	 * Generate the output.  
	 */
    public void generate() throws IOException, SAXException,
			ProcessingException {

    	log.info("Write out the redirect url for Ediauth server");
    	
    	this.response.setContentType("text/plain");
		this.response.setHeader("Content-Length", String.valueOf(redirect_url.length()));
		
		InputStream is = new StringBufferInputStream(redirect_url);
    	Utils.bufferedCopy(is, this.out);
        is.close();
        this.out.flush();
	}
    
    /**
	 * Recycle
	 */
    public void recycle() {
        this.response = null;
        this.request = null;
        this.session_id = null;
        this.dspaceUrl = null;
    }

}
