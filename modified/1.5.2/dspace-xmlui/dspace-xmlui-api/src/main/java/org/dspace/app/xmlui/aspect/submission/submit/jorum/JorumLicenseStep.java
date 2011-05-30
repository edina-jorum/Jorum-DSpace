/*
 * LicenseStep.java
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
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Option;
import org.dspace.app.xmlui.wing.element.Xref;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.xml.sax.SAXException;

import uk.ac.jorum.licence.LicenceController;

/**
 * This is the last step of the item submission processes. During this
 * step the user must agree to the collection's standard distribution
 * license. If the user can not agree to the license they they may either
 * save the submission untill a later time or remove the submission completely.
 * 
 * This step will include the full license text inside the page using the
 * HTML fragment method.
 * 
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class JorumLicenseStep extends AbstractSubmissionStep
{
    private static final Logger log = Logger.getLogger(JorumLicenseStep.class);
    
    /** Language Strings **/
    protected static final Message T_head = 
        message("xmlui.Submission.submit.LicenseStep.head");
    protected static final Message T_info1 = 
        message("xmlui.Submission.submit.LicenseStep.info1");
    protected static final Message T_info2 = 
        message("xmlui.Submission.submit.LicenseStep.info2");
    protected static final Message T_info3 = 
        message("xmlui.Submission.submit.LicenseStep.info3");
    protected static final Message T_decision_label = 
        message("xmlui.Submission.submit.LicenseStep.decision_label");
    
    protected static final Message T_decision_error = 
        message("xmlui.Submission.submit.LicenseStep.decision_error");
    protected static final Message T_submit_remove = 
        message("xmlui.Submission.submit.LicenseStep.submit_remove");
    protected static final Message T_submit_complete = 
        message("xmlui.Submission.submit.LicenseStep.submit_complete");
    
    // GWaller 3/12/10
    protected static final Message T_decision_checkbox_pre = 
        message("xmlui.Submission.submit.LicenseStep.decision_checkbox_pre");
    protected static final Message T_decision_checkbox_post = 
        message("xmlui.Submission.submit.LicenseStep.decision_checkbox_post");
	
    /** 
     * Global reference to Creative Commons license page
     * (this is used when CC Licensing is enabled in dspace.cfg)
     **/
    private JorumCCLicenseStep ccLicenseStep = null;
    
    /**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public JorumLicenseStep()
	{
		this.requireSubmission = true;
		this.requireStep = true;
	}
    
     /**
     * Check if this is actually the creative commons license step
     */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) 
        throws ProcessingException, SAXException, IOException
    { 
        super.setup(resolver,objectModel,src,parameters);
        
        //if Creative Commons licensing is enabled, and
        //we are on the 1st page of Licensing, 
        //then this is the CC License page
        if (LicenceController.isEnabled() && this.getPage()==1)
        {
           ccLicenseStep = new JorumCCLicenseStep();
           ccLicenseStep.setup(resolver, objectModel, src, parameters);
        }
        else
           ccLicenseStep = null;
    
    }
    
	public void addBody(Body body) throws SAXException, WingException,
	UIException, SQLException, IOException, AuthorizeException
	{
        // If Creative Commons licensing is enabled,
        // and we've initialized the CC license Page
        // then load the CreativeCommons page!
        if (LicenceController.isEnabled() && ccLicenseStep!=null)
        {
           //add body for CC License page
           ccLicenseStep.addBody(body);
           return;
        }
        
        // Get the full text for the actuial licese
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		String licenseText = collection.getLicense();
		
		Division div = body.addInteractiveDivision("submit-license",actionURL, Division.METHOD_POST,"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);
		
		Division inner = div.addDivision("submit-license-inner");
		inner.setHead(T_head);
		inner.addPara(T_info1);
		inner.addPara(T_info2);
		
		
		// Add the actual text of the license:
		Division displayLicense = inner.addDivision("submit-license-standard-text","license-text");
		displayLicense.addSimpleHTMLFragment(true, licenseText);
		
		inner.addPara(T_info3);
		
		List controls = inner.addList("submit-review", List.TYPE_FORM);
		
		CheckBox decision = controls.addItem().addCheckBox("decision");
		decision.setLabel(T_decision_label);
		
		// GWaller IssueID #546 3/12/10 Jorum hack - Message beside checkbox to agree terms has a link to the licence of the item
		Option licenceAgreeOption = decision.addOption("accept");
		licenceAgreeOption.addContent(T_decision_checkbox_pre);
		// Need to use a Xref element to contain the link to the licence
		Item item = submission.getItem();
		String licenceUrl = LicenceController.getLicenseURL(item);
		Xref xref = new Xref(licenceAgreeOption.getWingContext(), licenceUrl);
		xref.addContent(T_decision_checkbox_post);
		licenceAgreeOption.addWingElement(xref);

		// If user did not check "I accept" checkbox 
		if(this.errorFlag==org.dspace.submit.step.LicenseStep.STATUS_LICENSE_REJECTED)
		{
            log.info(LogManager.getHeader(context, "reject_license", submissionInfo.getSubmissionLogInfo()));

			decision.addError(T_decision_error);
		}
		
		//add standard control/paging buttons
        addControlButtons(controls);
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
    public List addReviewSection(List reviewList) throws SAXException,
        WingException, UIException, SQLException, IOException,
        AuthorizeException
    {
        //License step doesn't require reviewing
        return null;
    }
}