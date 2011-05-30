package org.dspace.app.xmlui.aspect.eperson;

import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

public class EdiauthLogin extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {
	
	/** language strings */
	public static final Message T_title = message("xmlui.EPerson.EdiauthLogin.title");

	public static final Message T_dspace_home = message("xmlui.general.dspace_home");

	public static final Message T_trail = message("xmlui.EPerson.EdiauthLogin.trail");

	public static final Message T_head1 = message("xmlui.EPerson.EdiauthLogin.head1");

	public static final Message T_submit = message("xmlui.EPerson.EdiauthLogin.submit");

	/**
	 * Generate the unique caching key. This key must be unique inside the space
	 * of this component.
	 */
	public Serializable getKey() {
		Request request = ObjectModelHelper.getRequest(objectModel);
		// String previous_username = request.getParameter("username");

		// Get any message parameters
		HttpSession session = request.getSession();
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		// If there is a message or previous email attempt then the page is not
		// cachable
		if (header == null && message == null && characters == null)
		//if (header == null && message == null && characters == null && previous_username == null)
			// cacheable
			return "1";
		else
			// Uncachable
			return "0";
	}

	/**
	 * Generate the cache validity object.
	 */
	public SourceValidity getValidity() {
		Request request = ObjectModelHelper.getRequest(objectModel);
		//String previous_username = request.getParameter("username");

		// Get any message parameters
		HttpSession session = request.getSession();
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		// If there is a message or previous email attempt then the page is not
		// cachable
		if (header == null && message == null && characters == null)
		//if (header == null && message == null && characters == null && previous_username == null)
			// Always valid
			return NOPValidity.SHARED_INSTANCE;
		else
			// invalid
			return null;
	}
	
	/**
	 * Set the page title and trail.
	 */
	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	/**
	 * Display the login form.
	 */
	public void addBody(Body body) throws SQLException, SAXException, WingException {
		// Check if the user has previously attempted to login.
		Request request = ObjectModelHelper.getRequest(objectModel);
		HttpSession session = request.getSession();
		//String previousUserName = request.getParameter("username");

		// Get any message parameters
		String header = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_HEADER);
		String message = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_MESSAGE);
		String characters = (String) session
				.getAttribute(AuthenticationUtil.REQUEST_INTERRUPTED_CHARACTERS);

		if (header != null || message != null || characters != null) {
			Division reason = body.addDivision("login-reason");

			if (header != null)
				reason.setHead(message(header));
			else
				// Always have a head.
				reason.setHead("Authentication Required");

			if (message != null)
				reason.addPara(message(message));

			if (characters != null)
				reason.addPara(characters);
		}

		String action = ConfigurationManager.getProperty("ediauth.login.action");
		Division login = body.addInteractiveDivision("login", action, Division.METHOD_GET, "primary");
		login.setHead(T_head1);

		List list = login.addList("ediauth-login", List.TYPE_FORM);

		list.addLabel();
		Item submit = list.addItem("login-in", null);
		/*
		submit.addHidden("service").setValue(service);
		submit.addHidden("context").setValue(context);
		submit.addButton("submit").setValue(T_submit);
		*/
		submit.addFigure("themes/Jorum_v2/images/depositor-login.gif", ConfigurationManager.getProperty("ediauth.login.url"), "Login");
		submit.addContent("via UK federation. ");
		submit.addXref("http://edina.ac.uk/access/fedaccess.shtml", "[info]");

	}

}
