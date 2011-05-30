/*
 * UploadStep.java
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
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.aspect.submission.submit.EditFileStep;
import org.dspace.app.xmlui.aspect.submission.submit.ReviewStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.workflow.WorkflowItem;
import org.xml.sax.SAXException;

import uk.ac.jorum.dspace.utils.BundleUtils;

/**
 * This is a step of the item submission processes. The upload
 * stages allows the user to upload files into the submission. The
 * form is optimized for one file, but allows the user to upload
 * more if needed.
 * <P>
 * The form is brokenup into three sections:
 * <P>
 * Part A: Ask the user to upload a file
 * Part B: List previously uploaded files
 * Part C: The standard action bar
 * 
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 */
public class JorumUploadStep extends AbstractSubmissionStep {
	private static Logger log = Logger.getLogger(JorumUploadStep.class);

	
	/***Added by CG ****/
	protected static final Message T_url_label = message("xmlui.Submission.submit.jorum.JorumUploadStep.url_label");
	protected static final Message T_url_help = message("xmlui.Submission.submit.jorum.JorumUploadStep.url_help");
	protected static final Message T_url_error = message("xmlui.Submission.submit.jorum.JorumUploadStep.url_error");
	protected static final Message T_virus_check_failed = message("xmlui.Submission.submit.jorum.JorumUploadStep.virus_check_failed");

	private static final String T_URL_DIV_TITLE = "url_div";
	private static final String T_URL_TEXT = "url";
	private static final String SCHEMA = "dc";
	private static final String URL_IDENTIFIER = "identifier";
	
	//altered
	protected static final Message J_head = message("xmlui.Submission.submit.jorum.JorumUploadStep.head");
	/*******/
	
	
	/** Language Strings for Uploading **/
	protected static final Message T_head = message("xmlui.Submission.submit.UploadStep.head");
	protected static final Message T_file = message("xmlui.Submission.submit.UploadStep.file");
	protected static final Message T_file_help = message("xmlui.Submission.submit.UploadStep.file_help");
	protected static final Message T_file_error = message("xmlui.Submission.submit.UploadStep.file_error");
	protected static final Message T_upload_error = message("xmlui.Submission.submit.UploadStep.upload_error");
	protected static final Message T_description = message("xmlui.Submission.submit.UploadStep.description");
	protected static final Message T_description_help = message("xmlui.Submission.submit.UploadStep.description_help");
	protected static final Message T_submit_upload = message("xmlui.Submission.submit.UploadStep.submit_upload");
	protected static final Message T_head2 = message("xmlui.Submission.submit.UploadStep.head2");
	protected static final Message T_column0 = message("xmlui.Submission.submit.UploadStep.column0");
	protected static final Message T_column1 = message("xmlui.Submission.submit.UploadStep.column1");
	protected static final Message T_column2 = message("xmlui.Submission.submit.UploadStep.column2");
	protected static final Message T_column3 = message("xmlui.Submission.submit.UploadStep.column3");
	protected static final Message T_column4 = message("xmlui.Submission.submit.UploadStep.column4");
	protected static final Message T_column5 = message("xmlui.Submission.submit.UploadStep.column5");
	protected static final Message T_column6 = message("xmlui.Submission.submit.UploadStep.column6");
	protected static final Message T_unknown_name = message("xmlui.Submission.submit.UploadStep.unknown_name");
	protected static final Message T_unknown_format = message("xmlui.Submission.submit.UploadStep.unknown_format");
	protected static final Message T_supported = message("xmlui.Submission.submit.UploadStep.supported");
	protected static final Message T_known = message("xmlui.Submission.submit.UploadStep.known");
	protected static final Message T_unsupported = message("xmlui.Submission.submit.UploadStep.unsupported");
	protected static final Message T_submit_edit = message("xmlui.Submission.submit.UploadStep.submit_edit");
	protected static final Message T_checksum = message("xmlui.Submission.submit.UploadStep.checksum");
	protected static final Message T_submit_remove = message("xmlui.Submission.submit.UploadStep.submit_remove");

	
	
	/** 
	 * Global reference to edit file page
	 * (this is used when a user requests to edit a bitstream)
	 **/
	private EditFileStep editFile = null;

	/**
	 * Establish our required parameters, abstractStep will enforce these.
	 */
	public JorumUploadStep() {
		this.requireSubmission = true;
		this.requireStep = true;
	}

	/**
	 * Check if user has requested to edit information about an
	 * uploaded file
	 */
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters)
			throws ProcessingException, SAXException, IOException {
		super.setup(resolver, objectModel, src, parameters);

		//If this page for editing an uploaded file's information
		//was requested, then we need to load EditFileStep instead!
		if (this.errorFlag == org.dspace.submit.step.UploadStep.STATUS_EDIT_BITSTREAM) {
			this.editFile = new EditFileStep();
			this.editFile.setup(resolver, objectModel, src, parameters);
		} else
			this.editFile = null;
	}

	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException,
			AuthorizeException {
		//If we are actually editing an uploaded file's information,
		//then display that body instead!
		if (this.editFile != null) {
			editFile.addBody(body);
			return;
		}
		
		//Check for existence of url bundle.  If not there, it's a file submission
		boolean isUrl = BundleUtils.checkUrl(submissionInfo);

		// Get a list of all files in the original bundle
		Item item = submission.getItem();
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		boolean workflow = submission instanceof WorkflowItem;
		
		// Need to work out what bundle we're going to display
		Bundle[] bundles = BundleUtils.getBundlesForDisplay(item, context); 	
		
		// Added by CG
		Bitstream[] bitstreams = null;
		int length = bundles.length;
		if (length > 0) {
			if(length == 1){
				bitstreams = bundles[0].getBitstreams();
			}else{
				// we're displaying related cps
				bitstreams = new Bitstream[length];
				int count = 0;
				for(Bundle b : bundles){
					bitstreams[count] = b.getBitstreams()[0];
					count++;
				}
			}

		}else{
			bitstreams = new Bitstream[0];
		}


		// Part A: 
		//  First ask the user if they would like to upload a new file (may be the first one)
		Division div = body.addInteractiveDivision("submit-upload", actionURL, Division.METHOD_MULTIPART,
				"primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		List upload = null;
		if (!workflow) {
			// Only add the upload capabilities for new item submissions
			upload = div.addList("submit-upload-new", List.TYPE_FORM);
			//upload.setHead(T_head);
			upload.setHead(J_head);

			if (!isUrl) {
				File file = upload.addItem().addFile("file");
				file.setLabel(T_file);
				file.setHelp(T_file_help);
				file.setRequired();

				//if no files found error was thrown by processing class, display it!
				if (this.errorFlag == org.dspace.submit.step.jorum.JorumUploadStep.STATUS_NO_FILES_ERROR) {
					file.addError(T_file_error);
				}

				// if an upload error was thrown by processing class, display it!
				if (this.errorFlag == org.dspace.submit.step.jorum.JorumUploadStep.STATUS_UPLOAD_ERROR) {
					file.addError(T_upload_error);
				}

				// GH 
				// if an upload error was thrown by processing class, display it!
                if (this.errorFlag == org.dspace.submit.step.jorum.JorumUploadStep.VIRUS_CHECK_FAILED) {
                    Object[] adminEmail = {ConfigurationManager.getProperty("mail.admin")};
                    file.addError(T_virus_check_failed.parameterize(adminEmail));  
                }
                // GH - end
				
				Button uploadSubmit = upload.addItem().addButton("submit_upload");
				uploadSubmit.setValue(T_submit_upload);
			}
			 
			
			
			else{
				/***Added by CG ****/
				String urlValue = "http://";
				
				//It is a url
				Text url = upload.addItem(T_URL_DIV_TITLE,"").addText(T_URL_TEXT);
		        url.setLabel(T_url_label);
		        url.setHelp(T_url_help);
		        
		        //Get the metadata from the item to see if the identifier has already been set
		        DCValue[] dcVals =  item.getMetadata(SCHEMA, URL_IDENTIFIER, null, Item.ANY);
		        for(DCValue value : dcVals){
		        	urlValue = value.value;
		        }
		        url.setValue(urlValue);
		  
		        //The error flag will be set from JorumUploadStep processing class if no url entered.
		        if (this.errorFlag == org.dspace.submit.step.jorum.JorumUploadStep.STATUS_MISSING_URL) {
		        	url.addError(T_url_error);
		        	//reset the error flag
		        	this.errorFlag=0;
				}

		        //Not sure this really does anything?
		        url.setRequired(true);
			}
			/*******/
			
		}

		// Part B:
		//  If the user has allready uploaded files provide a list for the user.
		if (bitstreams.length > 0 || workflow) {
			Table summary = div.addTable("submit-upload-summary", (bitstreams.length * 2) + 2, 6);
			summary.setHead(T_head2);

			Row header = summary.addRow(Row.ROLE_HEADER);
			header.addCellContent(T_column1); // select checkbox
			header.addCellContent(T_column2); // file name	
			header.addCellContent(T_column6); // edit button

			for (Bitstream bitstream : bitstreams) {
				int id = bitstream.getID();
				String name = bitstream.getName();
				String url = contextPath + "/bitstream/item/" + item.getID() + "/" + name;
				long bytes = bitstream.getSize();
				String algorithm = bitstream.getChecksumAlgorithm();
				String checksum = bitstream.getChecksum();
				BitstreamFormat format = bitstream.getFormat();
				int support = format.getSupportLevel();

				Row row = summary.addRow();

				if (!workflow) {
					// Workflow users can not remove files.
					CheckBox remove = row.addCell().addCheckBox("remove");
					remove.setLabel("remove");
					remove.addOption(id);
				} else {
					row.addCell();
				}

				row.addCell().addXref(url, name);


				// Edited by CG
				Row size = summary.addRow();
				size.addCell();
				Cell checksumCell = size.addCell(null, null, 0, 6, null);
				checksumCell.addHighlight("bold").addContent(T_column3);
				checksumCell.addContent(" : ");
				checksumCell.addContent(bytes + " bytes");
				
				Row formatRow = summary.addRow();
				formatRow.addCell();
				Cell formatCell = formatRow.addCell(null, null, 0, 6, null);
				formatCell.addHighlight("bold").addContent(T_column5);
				formatCell.addContent(" : ");
				if (format == null) {
					formatCell.addContent(T_unknown_format);
				} else {
					formatCell.addContent(format.getMIMEType());
					switch (support) {
						case 1:
							formatCell.addContent(T_supported);
							break;
						case 2:
							formatCell.addContent(T_known);
							break;
						case 3:
							formatCell.addContent(T_unsupported);
							break;
					}
				}
				

			}

			if (!workflow) {
				// Workflow user's can not remove files.
				Row actionRow = summary.addRow();
				actionRow.addCell();
				Button removeSeleceted = actionRow.addCell(null, null, 0, 6, null).addButton("submit_remove_selected");
				removeSeleceted.setValue(T_submit_remove);
			}

			upload = div.addList("submit-upload-new-part2", List.TYPE_FORM);
		}

		// Part C:
		// add standard control/paging buttons
		addControlButtons(upload);
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
		// Create a new list section for this step (and set its heading)
		List uploadSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
		uploadSection.setHead(T_head);

		//Review all uploaded files
		Bundle[] bundles = submission.getItem().getBundles("ORIGINAL");
		Bitstream[] bitstreams = new Bitstream[0];
		if (bundles.length > 0) {
			bitstreams = bundles[0].getBitstreams();
		}

		for (Bitstream bitstream : bitstreams) {
			BitstreamFormat bitstreamFormat = bitstream.getFormat();

			int id = bitstream.getID();
			String name = bitstream.getName();
			String url = contextPath + "/retrieve/" + id + "/" + name;
			String format = bitstreamFormat.getShortDescription();
			Message support = ReviewStep.T_unknown;
			
			if (bitstreamFormat.getSupportLevel() == BitstreamFormat.KNOWN)
				support = T_known;
			else if (bitstreamFormat.getSupportLevel() == BitstreamFormat.SUPPORTED)
				support = T_supported;

			org.dspace.app.xmlui.wing.element.Item file = uploadSection.addItem();
			file.addXref(url, name);
			file.addContent(" - " + format + " ");
			file.addContent(support);

		}

		//return this new "upload" section
		return uploadSection;
	}
}
