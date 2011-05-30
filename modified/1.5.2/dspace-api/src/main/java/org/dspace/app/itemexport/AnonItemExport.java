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

package org.dspace.app.itemexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.FutureTask;

import javax.mail.MessagingException;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;

public class AnonItemExport extends ItemExport {
	/**
     * used for export download
     */
    public static final String COMPRESSED_EXPORT_MIME_TYPE = "application/zip";
    
    /** log4j logger */
     private static Logger log = Logger.getLogger(AnonItemExport.class);

    
     /**
      * Convenience methot to create export a single Community, Collection, or
      * Item
      * 
      * @param dso
      *            - the dspace object to export
      * @param context
      *            - the dspace context
      * @param email
      *            - email to use
      * @throws Exception
      */
     public static void createDownloadableExport(DSpaceObject dso,
             Context context, String email, boolean migrate) throws Exception
     {
         ArrayList<DSpaceObject> list = new ArrayList<DSpaceObject>(1);
         list.add(dso);
         processDownloadableExport(list, context, email, migrate);
     }
     
     /**
     * Does the work creating a List with all the Items in the Community or
     * Collection It then kicks off a new Thread to export the items, zip the
     * export directory and send confirmation email
     * 
     * @param dsObjects
     *            - List of dspace objects to process
     * @param context
     *            - the dspace context
     * @param email
     *            - email address send confirmation email to
     * @throws Exception
     */
    private static void processDownloadableExport(List<DSpaceObject> dsObjects,
            Context context, final String userEmail, boolean toMigrate) throws Exception
    {
        final String email = userEmail;
        final boolean migrate = toMigrate;

        /*
         *  before we create a new export archive lets delete the 'expired' archives
         */
        deleteOldExportArchives();

        /*
         *  keep track of the cumulative size of all bitstreams in each of the
         *  items it will be checked against the config file entry
         */
        float size = 0;
        final ArrayList<Integer> items = new ArrayList<Integer>();
        for (DSpaceObject dso : dsObjects)
        {
            if (dso.getType() == Constants.ITEM)
            {
                Item item = (Item) dso;
                // get all the bundles in the item
                Bundle[] bundles = item.getBundles();
                for (Bundle bundle : bundles)
                {
                	// GWaller 11/02/09 IssueID #192 Ignore bundles storing archived packages
                    String bundleName = bundle.getName();
                	if (bundleName.startsWith(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE) || bundleName.startsWith(Constants.BACKUP_CONTENT_PACKAGE_BUNDLE)){
                    	// simply go to the next bundle
                    	continue;
                    }
                	
                    // get all the bitstreams in the bundle
                    Bitstream[] bitstreams = bundle.getBitstreams();
                    for (Bitstream bit : bitstreams)
                    {
                        // add up the size
                        size += bit.getSize();
                    }
                }
                items.add(item.getID());
            }
            else
            {
                // nothing to do just ignore this type of DSPaceObject
            }
        }

        /*
         *  check the size of all the bitstreams against the configuration file entry if it exists
         */
        String megaBytes = ConfigurationManager.getProperty("org.dspace.app.itemexport.max.size");
        if (megaBytes != null)
        {
            float maxSize = 0;
            try
            {
                maxSize = Float.parseFloat(megaBytes);
            }
            catch (Exception e)
            {
                // ignore...configuration entry may not be present
            }

            if (maxSize > 0)
            {
                if (maxSize < (size / 1048576.00))
                { // a megabyte
                    throw new ItemExportException(ItemExportException.EXPORT_TOO_LARGE,
                    	"The overall size of this export is too large.  Please contact your administrator for more information.");
                }
            }
        }

        /*
         *  if we have any items to process then create a task and use the 
         *  executor service to submit it to the thread pool
         */
        if (items.size() > 0)
        {
        	FutureTask<Integer> task = new FutureTask<Integer>(new CallableExport(email, items, migrate));
        	es.submit(task);
        }
    }

    /**
     * Create a file name based on the date
     * 
     * @param email
     *            - email address of user who requested export and will be able to download it
     * @param date
     *            - the date the export process was created
     * @return String representing the file name in the form of
     *         'export_yyy_MMM_dd_count_epersonID'
     * @throws Exception
     */
    public static String assembleFileName(String type, String email, Date date) throws Exception
    {
        // to format the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMM_dd");
        String downloadDir = getExportDownloadDirectory();
        // used to avoid name collision
        int count = 1;
        boolean exists = true;
        String fileName = null;
        while (exists)
        {
        	String myString = Thread.currentThread().toString();
        	String md5String = null;

        	try {
        	  MessageDigest digest = MessageDigest.getInstance("MD5");
        	  digest.update(myString.getBytes());
        	  md5String = new String(Hex.encodeHex(digest.digest()));
        	}
        	catch (Exception e) {
        		log.warn("Cannot create MD5 hash of thread info", e);
        	}
        	
        	fileName = type + "_export_" + sdf.format(date) + "_" + count + "_" + md5String + "_"  + "anon";
            exists = new File(downloadDir + System.getProperty("file.separator") + fileName + ".zip").exists();
            count++;
        }
        return fileName;
    }

    /**
     * Use config file entry for org.dspace.app.itemexport.download.dir and
     * string representing anonymous users to create a download directory name
     * 
     * @return String representing a directory in the form of
     *         org.dspace.app.itemexport.download.dir/anon
     * @throws Exception
     */
    public static String getExportDownloadDirectory()
            throws Exception
    {
        String downloadDir = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.download.dir");
        if (downloadDir == null)
        {
            throw new Exception(
                    "A dspace.cfg entry for 'org.dspace.app.itemexport.download.dir' does not exist.");
        }

        return downloadDir + System.getProperty("file.separator") + "anon";

    }

    /**
     * Used to read the export archived. Intended for download.
     * 
     * @param fileName
     *            the name of the file to download
     * @return an input stream of the file to be downloaded
     * @throws Exception
     */
    public static InputStream getExportDownloadInputStream(String fileName) throws Exception
    {
        File file = new File(getExportDownloadDirectory() + System.getProperty("file.separator") + fileName);
        if (file.exists())
        {
            return new FileInputStream(file);
        }
        else
            return null;
    }
    
    /**
     * Get the file size of the export archive represented by the file name
     * 
     * @param fileName
     *            name of the file to get the size
     * @return
     * @throws Exception
     */
    public static long getExportFileSize(String fileName) throws Exception
    {
        File file = new File(
                getExportDownloadDirectory()
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory()
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.length();
    }

    public static long getExportFileLastModified(String fileName)
            throws Exception
    {
        File file = new File(getExportDownloadDirectory()
                        + System.getProperty("file.separator") + fileName);
        if (!file.exists() || !file.isFile())
        {
            throw new FileNotFoundException("The file "
                    + getExportDownloadDirectory()
                    + System.getProperty("file.separator") + fileName
                    + " does not exist.");
        }

        return file.lastModified();
    }


    /**
     * A clean up method that is ran before a new export archive is created. It
     * uses the config file entry 'org.dspace.app.itemexport.life.span.hours' to
     * determine if the current exports are too old and need pruning
     * 
     * @throws Exception
     */
    public static void deleteOldExportArchives() throws Exception
    {
        int hours = ConfigurationManager.getIntProperty("org.dspace.app.itemexport.life.span.hours");
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.add(Calendar.HOUR, (-hours));
        File downloadDir = new File(getExportDownloadDirectory());
        if (downloadDir.exists())
        {
            File[] files = downloadDir.listFiles();
            for (File file : files)
            {
                if (file.lastModified() < now.getTimeInMillis())
                {
                    file.delete();
                }
            }
        }

    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplish this
     * communication with email instead. Send a success email once the export
     * archive is complete and ready for download
     * 
     * @param context
     *            - the current Context
     * @param userEmail
     *            - email address of user to send the email to
     * @param fileName
     *            - the file name to be downloaded. It is added to the url in
     *            the email
     * @throws MessagingException
     */
    public static void emailSuccessMessage(Context context, String userEmail, String fileName) throws MessagingException
    {
        try
        {
            Locale supportedLocale = I18nUtil.getDefaultLocale();
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_success"));
            email.addRecipient(userEmail);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/anonexportdownload/" + fileName);
            email.addArgument(ConfigurationManager.getProperty("org.dspace.app.itemexport.life.span.hours"));

            email.send();
        }
        catch (Exception e)
        {
            log.warn(LogManager.getHeader(context, "emailSuccessMessage", "cannot notify user of export"), e);
        }
    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplish this
     * communication with email instead. Send an error email if the export
     * archive fails
     * 
     * @param email
     *            - email address to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException
     */
    public static void emailErrorMessage(String userEmail , String error)
            throws MessagingException
    {
        log.warn("An error occured during item export, the user will be notified. " + error);
        try
        {
            Locale supportedLocale = I18nUtil.getDefaultLocale();
            // GWaller IssueID #611 26/1/11 Use the export_error template on error not export_success
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, "export_error"));
            email.addRecipient(userEmail);
            email.addArgument(error);
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/feedback");

            email.send();
        }
        catch (Exception e)
        {
            log.warn("error during item export error notification", e);
        }
    }
}
