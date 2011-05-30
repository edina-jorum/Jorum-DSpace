/*
 * InitialQuestionsStep.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
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
package org.dspace.submit.step.jorum;


import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

import uk.ac.jorum.dspace.utils.BundleUtils;

/**
 * Initial Submission servlet. Asks users whether they wish to submit a file or a url
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized 
 * by both the JSP-UI and the Manakin XML-UI
 * <P>
 * 
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.AbstractProcessingStep
 * 
 * @author cgormle1
 */
public class UploadChoiceStep extends AbstractProcessingStep {

	/***************************************************************************
     * STATUS / ERROR FLAGS (returned by doProcessing() if an error occurs or
     * additional user interaction may be required)
     * 
     * (Do NOT use status of 0, since it corresponds to STATUS_COMPLETE flag
     * defined in the AbstractProcessingStep class)
     **************************************************************************/
    // pruning of metadata needs to take place
    public static final int STATUS_CHOICE_NULL = 1;
    
	
	/** log4j logger */
	private static Logger log = Logger.getLogger(UploadChoiceStep.class);

	private static final String URL_LABEL="url";
	
	private static final String FILE_LABEL="file";
	
	/**
	 * Do any processing of the information input by the user, and/or perform
	 * step processing (if no user interaction required)
	 * <P>
	 * It is this method's job to save any data to the underlying database, as
	 * necessary, and return error messages (if any) which can then be processed
	 * by the appropriate user interface (JSP-UI or XML-UI)
	 * <P>
	 * NOTE: If this step is a non-interactive step (i.e. requires no UI), then
	 * it should perform *all* of its processing in this method!
	 * 
	 * @param context
	 *            current DSpace context
	 * @param request
	 *            current servlet request object
	 * @param response
	 *            current servlet response object
	 * @param subInfo
	 *            submission info object
	 * @return Status or error flag which will be processed by
	 *         doPostProcessing() below! (if STATUS_COMPLETE or 0 is returned,
	 *         no errors occurred!)
	 */
	public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response,
			SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
		
		//boolean url = subInfo.getIsURL();
		// Check if URL bundle exists
		// if so, either delete or resume depending on 
		// value of choice param
		boolean url = BundleUtils.checkUrl(subInfo);
		
		String choice = request.getParameter("choice_radio");

		// GWaller 13/8/09 choice can be null if the ?XML is used in the URL to look at the DRI
		if (choice == null){
			// Return an error to the user
			return STATUS_CHOICE_NULL;
		}

		if (choice.equals(FILE_LABEL) && !url) {
			//No need to clear any data - just proceed submission type is consistent.
			return STATUS_COMPLETE;
		} else if (choice.equals(FILE_LABEL) && url) {
			//file selected, but user previously selected url.  Delete all bitstreams, metadata associated with previous url submission.
			clearItemInfo(subInfo);	
			//no need to do anything further - url bundle deleted - absence of this means not treated as url submission
		} else if (choice.equals(URL_LABEL) && url) {
			//No need to clear any data - just proceed submission type is consistent.
			return STATUS_COMPLETE;
		} else if (choice.equals(URL_LABEL) && !url) {
			//url selected, but user previously selected file.  Delete all bitstreams, metadata associated with previous file submission.
			clearItemInfo(subInfo);
			// Create a blank url bundle
			// We can check for its existence in the next step to determine if this a url submission
			// Can't rely on request param (or session for that matter) if resuming an unfinished deposit, so use this to persist this info
			subInfo.getSubmissionItem().getItem().createBundle(Constants.URL_BUNDLE);
		}

		// commit all changes to DB
		subInfo.getSubmissionItem().update();
		context.commit();

		return STATUS_COMPLETE; // no errors!
	}

	
	
	
	/**
	 * Helper method to clear any details that may have been submitted for an item
	 * 
	 * @param subInfo 		The sunmissionInfo object
	 * @throws SQLException
	 * @throws AuthorizeException
	 * @throws IOException
	 */
	private static void clearItemInfo(SubmissionInfo subInfo) throws SQLException, AuthorizeException, IOException {
		Item item = subInfo.getSubmissionItem().getItem();
		Bundle[] bundles = item.getBundles();
		for (Bundle b : bundles) {
			item.removeBundle(b);
		}
		item.removeLicenses();
		//Remove all metadata
		item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		
		// as this method is called when a user starts a file submission and goes back to 
		// change to a url submission (or vice versa), as well as deleting previously submitted
		// bitstreams and metadata(above) we have to reset the stage of the submission process
		// we have reached - i.e reset to 1. This ensures that previously active buttons in the 
		// submission process are disabled.
		WorkspaceItem workspaceItem = (WorkspaceItem) subInfo.getSubmissionItem();
		if(workspaceItem.getStageReached()>1){
			workspaceItem.setStageReached(1);
		}
	}

	
	/**
	 * Retrieves the number of pages that this "step" extends over. This method
	 * is used to build the progress bar.
	 * <P>
	 * This method may just return 1 for most steps (since most steps consist of
	 * a single page). But, it should return a number greater than 1 for any
	 * "step" which spans across a number of HTML pages. For example, the
	 * configurable "Describe" step (configured using input-forms.xml) overrides
	 * this method to return the number of pages that are defined by its
	 * configuration file.
	 * <P>
	 * Steps which are non-interactive (i.e. they do not display an interface to
	 * the user) should return a value of 1, so that they are only processed
	 * once!
	 * 
	 * @param request
	 *            The HTTP Request
	 * @param subInfo
	 *            The current submission information object
	 * 
	 * @return the number of pages in this step
	 */
	public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
		// always just one page of initial questions
		return 1;
	}

}
