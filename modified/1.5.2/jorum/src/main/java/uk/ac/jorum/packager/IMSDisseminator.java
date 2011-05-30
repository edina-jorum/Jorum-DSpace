/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : IMSDisseminator.java
 *  Author              : gwaller
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
package uk.ac.jorum.packager;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * @author gwaller
 * @author cgormley
 */
public class IMSDisseminator implements PackageDisseminator {

	/** log4j category */
	private static Logger log = Logger.getLogger(IMSDisseminator.class);
	/**
	 * This method copies all entries from the supplied ZipInputStream into the supplied ZipOutputStream and 
	 * potentially overrides the manifest in the source Zip. If a newManifest stream is supplied, this will
	 * be stored in the new Zip rather than the one found in the source input stream
	 * @param newManifest can be null. If non null, this manifest stream will be read and stored in the new Zip
	 * @param in the source Zip to copy from. NOTE: this must be closed by the caller!!
	 * @param out the ZipOutputStream to write to. NOTE: this must be closed by the caller!!
	 * @throws PackageException
	 */
	public static void copyPackageOverrideManifest(InputStream newManifest, ZipInputStream in, ZipOutputStream out)
			throws PackageException {
		ZipEntry inEntry;
		try {

			while ((inEntry = in.getNextEntry()) != null) {

				ZipEntry outEntry = new ZipEntry(inEntry.getName());
				out.putNextEntry(outEntry);

				if (inEntry.getName().equals(IMSIngester.MANIFEST_FILE)) {
					// Found manifest - do we copy this one or the one supplied via params
					if (newManifest != null) {
						// Now copy the bytes
						copyStream(newManifest, out);
						continue;
					}
				}

				// Now copy the bytes
				copyStream(in, out);

			}
		} catch (IOException e) {
			throw new PackageException(e);
		}
	}

	/**
	 * This copies bytes from one stream to the other.
	 * NOTE: it does not close any of the streams - that is the reposability of the caller
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];

		int read = 0;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}

		out.flush();
	}

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageDisseminator#disseminate(org.dspace.core.Context, org.dspace.content.DSpaceObject, org.dspace.content.packager.PackageParameters, java.io.OutputStream)
	 */
	public void disseminate(Context context, DSpaceObject object, PackageParameters params, OutputStream out)
			throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException {

		// We can only disseminate an item as an IMS package at this stage - need to look at seeing if we could build a
		// package containing all items in collection/community
		if (!(object instanceof Item)) {
			throw new PackageException("Error: Only a DSpace Item can be disseminated as an IMS package");
		}

		Item item = (Item) object;

		XMLManifest manifest = new IMSManifest(item);
		//Set the metdata format to use for the dissemination crosswalk
		manifest.setExportMetadataFormat(params.getProperty(Constants.METADATA_FORMAT_LABEL,
				Constants.SupportedDisseminationMetadataFormats.QDC.toString()));
		
		// Populate this XMLManifest instance with either a brand new manifest or an update of the original manifest
		manifest.populate();

		// Create the ZIP file 
		ZipOutputStream zipOut = new ZipOutputStream(out);
		InputStream in = null;

		try {
			//All we need to do is save each bitstream in the content bundle as a zipEntry, then add an imsmanifest.xml
			// Remember - do not need to look at the URL_BUNDLEs - only physical files should have a Zip entry
			Bundle[] contentBundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
			if (contentBundles.length > 0) {
				//Get the content streams and copy to ZipEntry via buffer
				Bitstream[] bitstreams = item.getBundles(Constants.CONTENT_BUNDLE_NAME)[0].getBitstreams();
				for (Bitstream bitstream : bitstreams) {
					in = bitstream.retrieve();
					if (in != null)
						// NOTE: createZipEntry closes the input stream 'in'
						createZipEntry(zipOut, in, bitstream.getName());
				}
			}

			// Add the manifest
			in = manifest.getXmlAsStream();
			if (in != null)
				// NOTE: createZipEntry closes the input stream 'in'
				createZipEntry(zipOut, in, IMSIngester.MANIFEST_FILE);

		} catch (Exception e) {
			throw new PackageException("Error writing the zip ", e);
		}

		finally {
			if (in != null)
				in.close();
			// Complete the ZIP file
			if (zipOut != null)
				zipOut.close();
		}

		log.info("Completed IMS package export");

		/*Bundle archivedPackageBundles[] = item.getBundles(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE);
		if (archivedPackageBundles != null && archivedPackageBundles.length > 0){
			// Return the first bitstream in the first archived bundle
			Bitstream streams[] = archivedPackageBundles[0].getBitstreams();
			if (streams != null && streams.length > 0){
				InputStream in = streams[0].retrieve();
				try{
					copyStream(in, out);
				} finally{
					try {in.close();} catch (Exception x){}
				}
			} else {
				throw new PackageException(Constants.ARCHIVED_CONTENT_PACKAGE_BUNDLE + " bundle found but contains no bitstreams");
			}
		} else {
			// Archived package not found - need to create the zip
			throw new PackageException("Not implemented - Complete IMS Package Generation is not implemented yet");
		}*/

	}

	/**
	 * Wries a new zip entry to the ZipOutputStream and also closes the InputStream in
	 * @param zipOut
	 * @param in the input stream to copy from. NOTE: this is closed by this method
	 * @param entryName
	 * @throws IOException
	 */
	private void createZipEntry(ZipOutputStream zipOut, InputStream in, String entryName) throws IOException {
		// Create a buffer for reading the files 
		byte[] buf = new byte[1024];
		try {
			log.debug("Writing zipEntry " + entryName);
			zipOut.putNextEntry(new ZipEntry(entryName));
			// Transfer bytes from the file to the ZIP entry
			int len;
			while ((len = in.read(buf)) > 0) {
				zipOut.write(buf, 0, len);
			}
		} finally {
			// Close the current entry
			zipOut.closeEntry();
			// Close the stream
			in.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.dspace.content.packager.PackageDisseminator#getMIMEType(org.dspace.content.packager.PackageParameters)
	 */
	public String getMIMEType(PackageParameters params) {
		return "application/zip";
	}

}
