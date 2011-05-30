/*
 * CCLicensePage.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.xmlui.aspect.submission.submit.jorum;


import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.FieldSet;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.HelpLink;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.xml.sax.SAXException;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.licence.LicenceController;
import uk.ac.jorum.licence.ItemLicence;
import uk.ac.jorum.licence.LicenceManager;

/**
 * This is an optional page of the item submission processes. The Creative 
 * Commons license may be added to an item in addition to the standard distribution 
 * license. This step will allow the user to go off to the creative commons website 
 * select a license and then when returned view what license was added to the item.
 * <P>
 * This class is called by org.dspace.app.xmlui.submission.step.LicenseStep
 * when the Creative Commons license is enabled
 * <P>
 * The form is divided into three major divisions: 1) A global div surrounds the 
 * whole page, 2) a specific interactive div displays the button that goes off to the 
 * creative commons website to select a license, and 3) a local division that displays 
 * the selected license and standard action bar.
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class JorumCCLicenseStep extends AbstractSubmissionStep {
	private static final Logger log = Logger.getLogger(JorumCCLicenseStep.class);
	/***Added by CG ****/
	protected static final Message T_decision_error = message("xmlui.Submission.submit.jorum.JorumCCLicenseStep.decision_error");
	/*******/

	// GWaller 4/11/09 IssueId #108 Added error message to indicate an item was found with an unspported licence
	protected static final Message T_unsupported_licence_warning = message("xmlui.Submission.submit.jorum.JorumCCLicenseStep.unsupported_licence_warn");
	
	/** Language Strings **/
	protected static final Message T_head = message("xmlui.Submission.submit.CCLicenseStep.head");
	protected static final Message T_info1 = message("xmlui.Submission.submit.CCLicenseStep.info1");
	protected static final Message T_submit_to_creative_commons = message("xmlui.Submission.submit.CCLicenseStep.submit_to_creative_commons");
	protected static final Message T_license = message("xmlui.Submission.submit.CCLicenseStep.license");
	protected static final Message T_submit_remove = message("xmlui.Submission.submit.CCLicenseStep.submit_remove");
	protected static final Message T_no_license = message("xmlui.Submission.submit.CCLicenseStep.no_license");

	protected static final Message T_info2 = message("xmlui.Submission.submit.CCLicenseStep.info2");
	protected static final Message T_faultyLicenceMessageDepositor = message("xmlui.Submission.submit.CCLicenseStep.faultyLicenceMessageDepositor");
	protected static final Message T_head2 = message("xmlui.Submission.submit.CCLicenseStep.head2");
	
	// GWaller 1/12/10  IssueID #539 Addition of a view licence link on the licence selection screen
	protected static final Message T_viewLicenceLinkAlt = message("xmlui.Submission.submit.CCLicenseStep.viewLicenceLinkAlt");
	
	private final boolean externalChooser = ConfigurationManager.getBooleanProperty("cc.chooser.external");

	/**
	 * The creative commons URL, where to send the user off to so that they can select a license.
	 */
	public final static String CREATIVE_COMMONS_URL = "http://creativecommons.org/license/";

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public JorumCCLicenseStep() {
		this.requireSubmission = true;
		this.requireStep = true;
	}

	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
			AuthorizeException {
		// Build the url to and from creative commons
		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

		Request request = ObjectModelHelper.getRequest(objectModel);
		boolean https = request.isSecure();
		String server = request.getServerName();
		int port = request.getServerPort();

		String exitURL = (https) ? "https://" : "http://";
		exitURL += server;
		if (!(port == 80 || port == 443))
			exitURL += ":" + port;

		exitURL += actionURL + "?submission-continue=" + knot.getId()
				+ "&license_url=[license_url]&license_button=[license_button]" + "&license_name=[license_name]";

		// Division 1:
		//  Global division
		Division div = body.addDivision("submit-cclicense", "primary submission");
		div.setHead(T_submission_head);

		// Division 2:
		//Progress bar division
		Division progressDiv = div.addInteractiveDivision("submit-cclicense-progress", actionURL, Division.METHOD_POST);
		addSubmissionProgressList(progressDiv);
		//need 'submission-continue' in order to keep current state
		progressDiv.addHidden("submission-continue").setValue(knot.getId());


		
		// Depending on whether the external chooser is selected, List form is added to:
		// a new div, onsiteDiv when externalChooser=true 
		// an existing div, ccLicenceDiv when externalChooser=false 
		List form = null;
		
		// START GWaller 4/11/09 IssueId #108 Added error message to indicate an item was found with an unspported licence
		// Only want to display the notice if the user submitted a piece of content which could already contain a licence i.e. a content package
		// or they submitted multiple items and atleast one of them could contain a licence 
		// i.e. need to check if the item has a RELATED_CP bundle or a ARCHIVED_CP bundle
		if ((BundleUtils.hasBundle(item, Constants.RELATED_CONTENT_PACKAGE_BUNDLE) ||
				BundleUtils.hasBundle(item, Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE)) &&
				LicenceController.foundUnsupportedLicenceInItemOrRelated(item, context)){
			log.info(LogManager.getHeader(context, "unsupported_licence", submissionInfo.getSubmissionLogInfo()));
		
			div.addPara().addHighlight("error").addContent(T_unsupported_licence_warning);
		} 
		// END GWaller 4/11/09 IssueId #108 Added error message to indicate an item was found with an unspported licence
	
		//Add details for link to external CC chooser 
		if (externalChooser) {
			// Division 3:
			// Creative commons offsite division
			// Bit of a horrible hack - all the info I want is added to hidden form fields that can be accessed from Jorum.xsl
			Division offsiteDiv = div.addInteractiveDivision("submit-cclicense-offsite", CREATIVE_COMMONS_URL, Division.METHOD_POST);
			offsiteDiv.setHead(T_head);
			offsiteDiv.addPara(T_info1);

			offsiteDiv.addHidden("submission-continue").setValue(knot.getId());
			offsiteDiv.addHidden("jurisdiction").setValue("uk");
			offsiteDiv.addHidden("stylesheet").setValue(ConfigurationManager.getProperty("external.cc.stylesheet"));
			offsiteDiv.addHidden("partner").setValue("dspace");
			offsiteDiv.addHidden("exit_url").setValue(exitURL);
			// Paragraph for adding link to external chooser
			Para cc_para = offsiteDiv.addPara("cc_submission", null);

			// Display licence if selected or message if not in this div 
			Division onsiteDiv = div.addInteractiveDivision("submit-cclicense-offsite", actionURL, Division.METHOD_POST);
			form = onsiteDiv.addList("submit-review", List.TYPE_FORM);
			form.addLabel(T_license);
			
			if (LicenceController.hasLicense(context, item)) {
				//Display the selected licence and icon
				String url = LicenceController.getLicenseURL(item);
				form.addItem("licence_details", null).addXref(url, LicenceController.getLicenseName(item));
				form.addItem().addHidden("cc_license_icon").setValue(LicenceController.getLicenseIconLocation(item));
				form.addItem().addButton("submit_no_cc").setValue(T_submit_remove);
				form.addItem().addHidden("license_url").setValue(url);
			} else {
				//Message saying no licence selected
				form.addItem().addHighlight("italic").addContent(T_no_license);
			}
		} else {
			// START GWaller 9/8/10 IssueID #303 Support for multiple licence options
			//Check if licence previously selected here so we can set the correct radio button
			String licenceUrl = "";
			if (LicenceController.hasLicense(context, item)) {
				log.debug("Item has a licence assigned");
				licenceUrl = LicenceController.getLicenseURL(item);
				log.debug("Licence found in item: <" + licenceUrl + ">");
			}
			
			Division mainLicenceDiv = div.addInteractiveDivision("submit-cclicense-offsite", actionURL, Division.METHOD_POST);
			List licenceList = mainLicenceDiv.addList("licences", List.TYPE_FORM);
			
			
			LicenceManager[] licenceManagers = LicenceController.getLicenceManagers();
			
			for (LicenceManager manager : licenceManagers){			
				FieldSet fieldSet = new FieldSet(body.getWingContext(), manager.getSectionName(), "ds-form-list");
				int itemsInFieldSet = 0;
				
				for (ItemLicence licence : manager.getInstalledLicencesInDisplayOrder()) {
					
					// Check to see if this licence should be displayed on deposit (or if the user is authorised to use the licence)
					boolean authorisedForDeposit = false;
					Group[] authorisedDepositGroups = licence.authorisedGroupsForDepositing();
					
					if (authorisedDepositGroups.length == 0){
						// no groups set so allow everyone
						authorisedForDeposit = true;
					} else {
						for (Group g: authorisedDepositGroups){
							if (Group.isMember(this.context, g.getID())){
								authorisedForDeposit = true;
								break;
							}
						}
					}
					
					if (licence.allowWebUIDeposit() && authorisedForDeposit){
						int id = licence.getId();
			
						org.dspace.app.xmlui.wing.element.Item listItem = new org.dspace.app.xmlui.wing.element.Item(body.getWingContext(), null, null);
						
						Radio radio = listItem.addRadio("license_url");
						radio.setLabel(licence.getProps().getProperty(ItemLicence.ICON_KEY));
						
						
						radio.addOption(id, licence.getProps().getProperty(ItemLicence.NAME_KEY));
						
						// GWaller 1/12/10  IssueID #539 Addition of a view licence link on the licence selection screen
						HelpLink helpLink = radio.createAndSetHelpLink();
						helpLink.addLink(T_viewLicenceLinkAlt, licence.getProps().getProperty(ItemLicence.URL_KEY));
						
						
						
						String licenceToCompare = licence.getProps().getProperty(ItemLicence.URL_KEY);
						log.debug("Comparing licence from licence manager <" + licenceToCompare + "> to item licence <" + licenceUrl + ">");
						
						if (licenceToCompare.equals(licenceUrl)) {
							radio.setOptionSelected(id);
						}
						
						fieldSet.addWingElement(listItem);
						itemsInFieldSet++;
					}
				}
				
				
				// We only want ot add the fieldset if there were any licences actually added to it
				if (itemsInFieldSet > 0){
					licenceList.addWingElement(fieldSet);
				}

				
			}
			
			//This allows us to display the error message below if appropriate
			form = mainLicenceDiv.addList("submit-review", List.TYPE_FORM);
			
			// END GWaller 9/8/10 IssueID #303 Support for multiple licence options
		
		
		}
		//Added by CG
		div.addDivision("cc_warning", "cc_warning").addPara(T_faultyLicenceMessageDepositor);

		/***Added by CG ****/
		// This shows an error message if the user tried to proceed without selecting a cc licence.  Same for external and radio chooser
		if (this.errorFlag == org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED) {

			log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));
			form.addItem().addHighlight("italic").addContent(T_decision_error);
		} 
		/*******/
		
		
		// add standard control/paging buttons
		addControlButtons(form);

	}

	/** 
	 * Each submission step must define its own information to be reviewed
	 * during the final Review/Verify Step in the submission process.
	 * <P>
	 * The information to review should be tacked onto the passed in 
	 * List object.
	 * <P>
	 * NOTE: To remain consistent across all Steps, you should first
	 * add a sub-List object (with this step's name as the heading),
	 * by using a call to reviewList.addList().   This sublist is
	 * the list you return from this method!
	 * 
	 * @param reviewList
	 *      The List to which all reviewable information should be added
	 * @return 
	 *      The new sub-List object created by this step, which contains
	 *      all the reviewable information.  If this step has nothing to
	 *      review, then return null!   
	 */
	public List addReviewSection(List reviewList) throws SAXException, WingException, UIException, SQLException,
			IOException, AuthorizeException {
		//nothing to review for CC License step
		return null;
	}

	/**
	 * Recycle
	 */
	public void recycle() {
		super.recycle();
	}
}
