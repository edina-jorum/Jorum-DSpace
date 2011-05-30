/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : AnonItemExport.java
 *  Author              : Ian Fieldhouse (ianfi)
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

package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple form so that we can capture their email so that we can
 * inform them when their export is ready for download.
 * 
 * @author Ian Fieldhouse (ianfi)
 */

public class AnonItemExport extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {
	
	/** log4j category */
    private static Logger log = Logger.getLogger(AnonItemExport.class);
	
	/** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    
    private static final Message T_title = message("xmlui.ArtifactBrowser.AnonItemExport.title");
    
    private static final Message T_trail = message("xmlui.ArtifactBrowser.AnonItemExport.trail");
    
    private static final Message T_head =  message("xmlui.ArtifactBrowser.AnonItemExport.head");
    
    private static final Message T_para1 = message("xmlui.ArtifactBrowser.AnonItemExport.para1");
    
    private static final Message T_email = message("xmlui.ArtifactBrowser.AnonItemExport.email");

    private static final Message T_email_help = message("xmlui.ArtifactBrowser.AnonItemExport.email_help");
    
    private static final Message T_submit = message("xmlui.ArtifactBrowser.AnonItemExport.submit");
    
    private static final Message T_export_item_not_found = message("xmlui.ArtifactBrowser.AnonItemExport.item.not.found");
    
    private static final Message T_export_bad_item_id = message("xmlui.ArtifactBrowser.AnonItemExport.item.id.error");
    
    private static final Message T_item_export_success = message("xmlui.ArtifactBrowser.AnonItemExport.item.success");
    
    private static final Message T_error_no_email = message("xmlui.ArtifactBrowser.AnonItemExport.error.no.email");
    
    private static final Message T_privacy_policy_excerpt = message("xmlui.ArtifactBrowser.AnonItemExport.privacy_policy_excerpt");
	
	
    Request request;
    Response response;

	String itemId;
	String email;
	
	Boolean validItemId = false;
	Boolean validEmail  = false;
	
	java.util.List<Message> errors;
	Message message;
	

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src,
			Parameters parameters) throws ProcessingException, SAXException,
			IOException {
		super.setup(resolver, objectModel, src, parameters);
		
		this.objectModel = objectModel;
		this.request     = ObjectModelHelper.getRequest(objectModel);
		this.response    = ObjectModelHelper.getResponse(objectModel);
		
		this.itemId = request.getParameter("itemID");
		this.email  = request.getParameter("email");
		
		errors = new ArrayList<Message>();
		
		/* 
		 * Check whether an itemId is associated with this request
		 */
		if (itemId != null)
		{
			log.info("itemId present with a value of " + itemId);
			
			// Process item export request
			Item item = null;
			
			try 
			{
				item = Item.find(context, Integer.parseInt(itemId));
			} 
			catch (Exception e) 
			{
				errors.add(T_export_bad_item_id);
				validItemId = false;
			}
			
			if (item == null) 
			{
				errors.add(T_export_item_not_found);
				validItemId = false;
			} 
			else 
			{
				validItemId = true;
				/* 
				 * Check whether an email address has been submitted
				 * by the user
				 */
				if ((email != null) && (!email.equals("")))
				{
					log.info("email present with a value of " + email);

					try 
					{
						org.dspace.app.itemexport.AnonItemExport.createDownloadableExport(item, context, email, false);
					} 
					catch (Exception e) 
					{
						errors.add(message(e.getMessage()));
					}
					
					this.validEmail = true;
					
				}
				else
				{
					
					log.info("no email present");
					
					// Display form for submission of email
					if (email == "")
						errors.add(T_error_no_email);
					
					this.validEmail = false;
					
				}
			}
			
			if (errors.size() <= 0) {
				message = T_item_export_success;
			}
		}
		else
		{
			log.info("no itemId present");
			
			/* 
			 * No item id so set validItemId to false so that 
			 * we can display the correct page
			 */
			errors.add(T_export_bad_item_id);
			
			this.validItemId = false;
			
		}

	}
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
	public Serializable getKey() 
	{
		String itemId = parameters.getParameter("itemId","unknown");
		return HashUtil.hash(itemId);
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
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException 
	{

		if (!validItemId)
		{
			// Display error page 
			Division div = body.addDivision("export_error");
			div.setHead(T_head);
			
			addErrors(div);
		}
		else if (!validEmail)
		{
			// Display the email submission form
			Division div = body.addInteractiveDivision(
					"anon-export-form", contextPath + "/anon-export",
					Division.METHOD_POST, "primary");

			div.setHead(T_head);
			div.addPara(T_para1);
			addErrors(div);

			List form = div.addList("form", List.TYPE_FORM);
			
			Text emailElement = form.addItem().addText("email");
			emailElement.setValue(email);
			emailElement.setLabel(T_email);
			emailElement.setHelp(T_email_help);

			form.addItem().addButton("submit").setValue(T_submit);

			div.addHidden("itemID").setValue(itemId);
			
			Division ppe = div.addDivision("privacy-policy-excerpt");
			Para privacy_statement = ppe.addPara();
			privacy_statement.addFigure("/xmlui/themes/Jorum/images/warning_48.png", null, null);
			privacy_statement.addContent(T_privacy_policy_excerpt);
		}
		else 
		{
			/* 
			 * Both itemId and email were valid to display success screen
			 * or errors resulting from the export process
			 */
			
			Division div = body.addDivision("export_sucess");
			div.setHead(T_head);
			div.addPara(T_item_export_success);
		}
		
	}

	private void addErrors(Division div) throws WingException {
		if (errors.size() > 0) {
			for (Message error : this.errors) {
				div.addPara().addHighlight("error").addContent(error);
			}
		}
	}
	
	/**
	 * recycle
	 */
	public void recycle() {
		this.errors  = null;
		this.message = null;
		this.email   = null;
		this.itemId  = null;
		super.recycle();
	}

}
