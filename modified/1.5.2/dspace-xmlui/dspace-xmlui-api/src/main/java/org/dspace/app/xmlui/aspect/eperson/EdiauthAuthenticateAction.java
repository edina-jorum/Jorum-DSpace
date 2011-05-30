package org.dspace.app.xmlui.aspect.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Attempt to authenticate the user based upon their presented credentials. This
 * action assumes that requests from the Ediauth server come from the localhost
 *  and uses the http parameter of ea_edinaUserId as credentials.
 * 
 * If the request doesn't come from the localhost then it is assumed that this
 * is a request from a user so a http parameter of sid is expected if a use session
 * exists. Otherwise it is assumed that the use is attempting to login.
 * 
 * If the authentication attempt is successful then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the system
 * before authenticated or if none is supplied, back to the DSpace homepage. The
 * action will also return true, thus contents of the action will be executed.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:act name="EdiauthAuthenticateAction"> <map:serialize type="xml"/> </map:act>
 * <map:transform type="EdiauthLogin">
 * 
 * @author Ian Fieldhouse
 */

public class EdiauthAuthenticateAction extends AbstractAction {

	/** log4j category */
    private static Logger log = Logger.getLogger(EdiauthAuthenticateAction.class);
	
    
	/**
	 * Attempt to authenticate the user.
	 */
	public Map act(Redirector redirector, SourceResolver resolver,
			Map objectModel, String source, Parameters parameters)
		throws Exception 
	{
		Request request = ObjectModelHelper.getRequest(objectModel);

		String ea_edinaUserId = request.getParameter("ea_edinaUserId");
		String ea_personal    = request.getParameter("ea_personal");
		String sid            = request.getParameter("sid");
		final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
		
		/**
		 *  First check if this is a request by an end user or 
		 *  the Ediauth server on the localhost
		 */
		// Determine where this request is coming from
		final String localIpv4 = "127.0.0.1";
		final String localIpv6 = "0:0:0:0:0:0:0:1%0";
		
		String remoteIp = request.getRemoteAddr();
		log.info("Remote IP = " + remoteIp);
		
		if (remoteIp.equals(localIpv4) || remoteIp.equals(localIpv6))
		{   // request from Ediauth server
			
			// Skip out if no ea_edinaUserId given
			if (ea_edinaUserId == null){
				log.info("No valid ea_edinaUserId supplied");
				ediauthRedirect(httpResponse, null);
			}
			
			try {
				if (ea_personal.equals("0")){
					log.info("ea_personal supplied is 0");
					ediauthRedirect(httpResponse, null);
				}
			} catch (Exception e) {
				log.info("No valid ea_personal supplied");
				ediauthRedirect(httpResponse, null);
			}
			
			log.info("Request from localhost so attempt to authenticate.");
			log.info("ea_edinaUserId supplied is: " + ea_edinaUserId);
			log.info("ea_personal supplied is 1");
			
			try {
				Context context = AuthenticationUtil.Authenticate(objectModel,ea_edinaUserId, null, null);

				EPerson eperson = context.getCurrentUser();

				if (eperson != null) {
					// The user has successfully logged in
					
					// Now determine the users session ID and send this to the EdiauthRedirect servlet
					String sessionId = request.getCocoonSession().getId();
					ediauthRedirect(httpResponse, sessionId);
						
					// log the user out for the rest of this current request,
					// however they will be re-authenticated
					// fully when they come back from the redirect. This prevents
					// caching problems where part of the
					// request is performed fore the user was authenticated and the
					// other half after it succeeded. This way the user is fully 
					// authenticated from the start of the request.
					context.setCurrentUser(null);
					//context.commit();
				}
			} catch (SQLException sqle) {
				throw new PatternException("Unable to preform authentication", sqle);
			}
		}
		else 
		{   // request not from Ediauth server
			
			// Fail if no session id (sid) present.
			if (sid == null){
				log.info("No sid supplied.");
				return null;
			}
			
			log.info("Request not from localhost.");
			
			Cookie cookie = new Cookie("JSESSIONID",sid);
			cookie.setPath(request.getContextPath());
			httpResponse.addCookie(cookie);
			log.info("Set cookie with the JSESSIONID value set to sid ("+ sid +").");
			
			// START GWaller 6/10/10 IssueID #303 Support for multiple licence options
        	String redirectURL = request.getContextPath();
        	
        	if (AuthenticationUtil.isInterupptedRequest(objectModel))
        	{
        		// Resume the request and set the redirect target URL to
        		// that of the originaly interrupted request.
        		redirectURL += AuthenticationUtil.resumeInterruptedRequest(objectModel);
        	} 
			// END GWaller 6/10/10 IssueID #303 Support for multiple licence options
			
			httpResponse.sendRedirect(redirectURL);
			
			return new HashMap();	
		}

		return null;
	}

	private void ediauthRedirect(final HttpServletResponse httpResponse, String session_id)
			throws IOException {
		if (session_id == null)
			httpResponse.sendRedirect("ediauth-login-redirect");
		else
			httpResponse.sendRedirect("ediauth-login-redirect?sid=" + session_id);
	}

}
