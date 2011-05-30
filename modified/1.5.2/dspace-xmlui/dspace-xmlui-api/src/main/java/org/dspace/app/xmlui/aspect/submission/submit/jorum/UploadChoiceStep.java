/*
 * InitialQuestionsStep.java
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

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.submit.ReviewStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

import uk.ac.jorum.dspace.utils.BundleUtils;

/**
 * This is the first official step of the item submission processes. This
 * step will ask the user whether they wish to submit a file or a url
 * 
 * Questions:
 * File or URL
 * 
 * @author Colin Gormley
 */
public class UploadChoiceStep extends AbstractSubmissionStep {

	protected static final Message T_head = message("xmlui.Submission.submit.jorum.UploadChoiceStep.head");
	protected static final Message T_radio_label = message("xmlui.Submission.submit.jorum.UploadChoiceStep.radio_label");
	protected static final Message T_radio_help = message("xmlui.Submission.submit.jorum.UploadChoiceStep.radio_help");
	protected static final Message T_radio_file = message("xmlui.Submission.submit.jorum.UploadChoiceStep.radio_file");
	protected static final Message T_radio_url = message("xmlui.Submission.submit.jorum.UploadChoiceStep.radio_url");

	private static final String T_RADIO_TITLE = "choice_radio";
	private static final String T_LIST_DIV_TITLE = "submit-initial-questions";
	private static final String T_BODY_DIV_TITLE = "submit-initial-choice-questions";
	private static final String T_FILE = "file";
	private static final String T_URL = "url";

	//Left over - used in add review section
	protected static final Message T_multiple_titles = message("xmlui.Submission.submit.InitialQuestionsStep.multiple_titles");
	protected static final Message T_published_before = message("xmlui.Submission.submit.InitialQuestionsStep.published_before");
	
	// IF 14/09/2009 - Added language string for message to prompt user to add email to their profile
	private static final Message T_add_email = message("xmlui.Submission.submit.jorum.UploadChoiceStep.add_email");
	private static Logger log = Logger.getLogger(UploadChoiceStep.class);

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public UploadChoiceStep() {
		this.requireSubmission = true;
		this.requireStep = true;
	}

	//Generates the DRI which will be transformed into html
	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
	AuthorizeException {

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
					emailMessage.addPara(T_add_email);
				}
				else
				{
					renderPage(body);
				}
			} catch (NullPointerException e) {
				log.info("Password login, so netid is empty.");
				renderPage(body);
			}

		}
	}

	private void renderPage(Body body) throws WingException, SQLException {
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

		// Generate a form asking the user to choose between a file and url submission
		Division div = body.addInteractiveDivision(T_BODY_DIV_TITLE, actionURL, Division.METHOD_POST,
		"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		List form = div.addList(T_LIST_DIV_TITLE, List.TYPE_FORM);
		form.setHead(T_head);

		// Check if the selection had been made previously
		boolean isURL = BundleUtils.checkUrl(submissionInfo);	
		
		Radio radio = form.addItem().addRadio(T_RADIO_TITLE);
		radio.setLabel(T_radio_label);
		radio.setHelp(T_radio_help);
		radio.addOption(T_FILE, T_radio_file);
		if (isURL) {
			radio.setOptionSelected(T_URL);
		} else {
			radio.setOptionSelected(T_FILE);
		}
		radio.addOption(T_URL, T_radio_url);
		//add standard control/paging buttons
		addControlButtons(form);
	}

	/** 
	 * NOTE: Nothing altered here for Jorum Submission yet.
	 * 
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
		//Create a new section for this Initial Questions information
		List initSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
		initSection.setHead(T_head);

		//add information to review
		Message multipleTitles = ReviewStep.T_no;
		if (submission.hasMultipleTitles())
			multipleTitles = ReviewStep.T_yes;

		Message publishedBefore = ReviewStep.T_no;
		if (submission.isPublishedBefore())
			publishedBefore = ReviewStep.T_yes;

		initSection.addLabel(T_multiple_titles);
		initSection.addItem(multipleTitles);
		initSection.addLabel(T_published_before);
		initSection.addItem(publishedBefore);

		//return this new review section
		return initSection;
	}
}
