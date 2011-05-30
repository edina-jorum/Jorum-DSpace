package org.dspace.app.itemexport;


import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.eperson.EPerson;

import org.dspace.eperson.Group;
import uk.ac.jorum.licence.ItemLicence;
import uk.ac.jorum.licence.LicenceController;
import uk.ac.jorum.utils.ExceptionLogger;

class CallableExport implements Callable<Integer> {
	private EPerson eperson = null;
	private String email = null;
	private ArrayList<Integer> items;
	private boolean migrate;
	private final boolean imsDissemination = ConfigurationManager.getBooleanProperty("ims.dissemination.enable");
	private static final String ZIP_EXTENSION = ".zip";
	/** log4j category */
	private static Logger log = Logger.getLogger(CallableExport.class);
	/**
	 * Constructor
	 * 
	 * @param eperson
	 *            eperson requesting export
	 **/
	public CallableExport(EPerson eperson, ArrayList<Integer> items, Boolean migrate) {
		this.eperson = eperson;
		this.items = items;
		this.migrate = migrate;
	}

	/**
	 * Constructor
	 * 
	 * @param email
	 *            email address of user requesting export
	 **/
	public CallableExport(String email, ArrayList<Integer> items, Boolean migrate) {
		this.email = email;
		this.items = items;
		this.migrate = migrate;
	}

	public Integer call() throws Exception {

		Context context = null;
		ItemIterator iitems = null;
		try {
			// create a new dspace context
			context = new Context();
			// ignore auths
			context.setIgnoreAuthorization(true);
			iitems = new ItemIterator(context, items);


            // GWaller IssueID #579 26/1/11 Need to block export if user isn't authorized to view resource
            //NOTE: Creating a new ItemIterator so that the original iterator isn't altered (this is used later)
            ItemIterator tempIterator = new ItemIterator(context, items);
            Item itemToExport =  tempIterator.next();
            tempIterator.close();

            boolean authorised = false;
            ItemLicence itemLicence = LicenceController.getItemLicence(itemToExport);
            if (itemLicence != null){
        	    Group[] authorisedGroups = itemLicence.authorisedGroupsForViewing();
        	    if (authorisedGroups.length == 0){
        		    authorised = true;
        	    } else {
        		    // Check to see if the current user belongs to one of the groups

                    // Make sure the user is set in the context
                    context.setCurrentUser(eperson);

                    // NOTE: if the user hasn't logged in ie its an anon export, they won't match any groups
                    // Do we need to lookup the eperson based on an email?

        		    for (Group g: authorisedGroups){
					    if (Group.isMember(context, g.getID())){
						    authorised = true;
						    break;
					    }
				    }
        	    }
            } else {
                // Couldn't find a licence on the item - allow the export as no licence prohibits it
                authorised = true;
            }

            if (!authorised){
                throw new Exception("You are not authorised to export the resource due to the licence");
            }


			String fileName = null;
			String downloadDir = null;
			if (eperson != null) {
				fileName = ItemExport.assembleFileName("item", eperson, new Date());
				downloadDir = ItemExport.getExportDownloadDirectory(eperson.getID());

			}
			if (email != null) {
				fileName = AnonItemExport.assembleFileName("item", email, new Date());
				downloadDir = AnonItemExport.getExportDownloadDirectory();
			}
			String workDir = ItemExport.getExportWorkDirectory() + System.getProperty("file.separator") + fileName;

			File dnDir = new File(downloadDir);
			if (!dnDir.exists()) {
				dnDir.mkdirs();
			}

			//If this flag is true, we will attempt to export the item as an IMS CP.  Otherwise, export in the normal Dspace format.
			if (imsDissemination) {
				String imsZip = new StringBuilder(fileName).append(ZIP_EXTENSION).toString();
				File imsContentPackage = new File(dnDir, imsZip);
				if (!imsContentPackage.exists()) {
					imsContentPackage.createNewFile();
					log.debug("Created file for ims dissemination: " + imsZip);
				}

				//TODO: "IMS" shouldn't be hard coded here
				PackageDisseminator dip = (PackageDisseminator) PluginManager.getNamedPlugin(PackageDisseminator.class,
						"IMS");

				PackageParameters params = new PackageParameters();

				params.addProperty(Constants.METADATA_FORMAT_LABEL,
						Constants.SupportedDisseminationMetadataFormats.QDC.toString());

				dip.disseminate(context, iitems.next(), params, new FileOutputStream(imsContentPackage));

			} else {
				File wkDir = new File(workDir);
				if (!wkDir.exists()) {
					wkDir.mkdirs();
				}
				// export the items using normal export method
				ItemExport.exportItem(context, iitems, workDir, 1, migrate);
				// now zip up the export directory created above
				ItemExport.zip(workDir,
						new StringBuilder(downloadDir).append(File.separator).append(fileName).append(ZIP_EXTENSION)
								.toString());
			}

			// email message letting user know the file is ready for download 
			if (eperson != null)
				ItemExport.emailSuccessMessage(context, eperson, fileName + ".zip");
			if (email != null)
				AnonItemExport.emailSuccessMessage(context, email, fileName + ".zip");
			// return to enforcing auths
			context.setIgnoreAuthorization(false);
		} catch (Exception e1) {
			ExceptionLogger.logException(log, e1);
			try {
				if (eperson != null)
					ItemExport.emailErrorMessage(eperson, e1.getMessage());
				if (email != null)
					AnonItemExport.emailErrorMessage(email, e1.getMessage());
			} catch (Exception e) {
				// wont throw here
			}
			throw new RuntimeException(e1);
		} finally {
			if (iitems != null)
				iitems.close();

			// Make sure the database connection gets closed in all conditions.
			try {
				context.complete();
			} catch (SQLException sqle) {
				context.abort();
			}
		}

		return 1;
	}

}
