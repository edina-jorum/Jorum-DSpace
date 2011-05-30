/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : BasePackageDetector.java
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
package uk.ac.jorum.packager.detector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.packager.PackageDetector;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageUtils;
import org.jdom.Document;

import uk.ac.jorum.packager.XMLManifest;
import uk.ac.jorum.submit.step.PackageDetectorStep;
import uk.ac.jorum.utils.ExceptionLogger;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

/**
 * @author gwaller
 *
 */
public abstract class BasePackageDetector implements PackageDetector {

	private static Logger logger = Logger.getLogger(BasePackageDetector.class);
	
	// Initialise the MimeUtil class to use 2 detectors
	static {
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
	}
	
	// Read in 512 bytes in an attempt to detect the mime type via the magic header
	private static final int MIME_DETECTOR_HEADER_BUFFER_SIZE = 512;
	
	private Bitstream b;
	
	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.dectector.PackageDetector#isValidPackage()
	 */
	public abstract boolean isValidPackage();
	
	
	


	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.PackageDetector#setBitstream(org.dspace.content.Bitstream)
	 */
	public void setBitstream(Bitstream b) {
		this.b = b;
	}

	public Bitstream getBitstream(){
		return this.b;
	}

	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.PackageDetector#ingesterClass()
	 */
	public abstract Class<? extends PackageIngester> ingesterClass();


	private Collection<MimeType> getMimeTypes(byte[] byteArr){
		return MimeUtil.getMimeTypes(byteArr);
	}
	
	private Collection<MimeType> getMimeTypes(String filename){
		return MimeUtil.getMimeTypes(filename);
	}
	
	private boolean isZip(Collection<MimeType> types){
		boolean result = false;
		
		// Valid Zip MIME types (from MimeUtil): 
		// application/zip,application/x-zip,application/x-compressed,application/x-zip-compressed,multipart/x-zip
		for (MimeType type : types){
			logger.debug("Checking MIME type: " + type);
			
			if (type.getMediaType().compareTo("application") == 0){
				result = (type.getSubType().compareTo("zip") == 0) ||
				(type.getSubType().compareTo("x-zip") == 0) ||
				(type.getSubType().compareTo("x-compressed") == 0) ||
				(type.getSubType().compareTo("x-zip-compressed") == 0);
			} else if (type.getMediaType().compareTo("multipart") == 0){
				result = (type.getSubType().compareTo("x-zip") == 0);
			}
		}
		
		return result;
	}
	
	
	private boolean isZip(byte[] byteArr){
		boolean result = false;
		try{
			// Get mime types
			Collection<MimeType> mimeTypes = getMimeTypes(byteArr);
			result = isZip(mimeTypes);
		} catch (Exception e){
			ExceptionLogger.logException(logger, e);
		}
		
		return result;
	}
	
	private boolean isZip(String filename){
		boolean result = false;
		try{
			// Get mime types
			Collection<MimeType> mimeTypes = getMimeTypes(filename);
			result = isZip(mimeTypes);
		} catch (Exception e){
			ExceptionLogger.logException(logger, e);
		}
		
		return result;
	}
	
	private byte[] fillBuffer(InputStream is){
		byte[] result = new byte[MIME_DETECTOR_HEADER_BUFFER_SIZE];
		
		// Read in as much as we can
		int offset = 0;
		while (true){
			try{
				int read = is.read(result, offset, result.length - offset);
				
				if (read == -1){
					// Got to end of stream
					break;
				}
				
				// Update offset
				offset += read;
				
				if (offset >= result.length){
					// filled buffer!
					break;
				}
				
			} catch (IOException e){
				break;
			}
		}
		
		// Must check how many bytes we have actually read - if we have read less than the buffer size, must copy this to a new array to 
		// ensure the array returned only contains data read from the stream - not random data in memory
		if (offset != result.length){
			byte[] resultTrimmed = new byte[offset];
			for (int i = 0; i < offset; i++){
				resultTrimmed[i] = result[i]; 
			}
			
			result = resultTrimmed;
		}
		
		return result;
	}

	
	/**
	 * This method retiieves the bitstream inputstream, check to see if it is a Zip file (by reading the first 
	 * MIME_DETECTOR_HEADER_BUFFER_SIZE number of bytes and using MimeUtil) and if a zip is found, attempts to 
	 * find the manifest file specified by manifestName and parses it using JDom. (Schema validation is based
	 * on the dspace config - see PackageDetectorStep)
	 * @param manifestName the name of the manifest file to look for in the zip
	 * @return Document - a JDOM Document instance representing the parsed manifest or null if none found, or error occurred
	 */
	protected Document containsManifest(String manifestName){
		Document result = null;
		ZipInputStream zip = null;
		try{
			
			// Get an input stream to the object to read some bytes
			/* 
			 * NOTE: The input stream returned is a GeneralFileInputStream i.e. either SRBFileInputStream or LocalFileInputStream
			 *       Neither of these streams extend FileInputStream and do not support mar or reset methods and as such directly
			 *       passing the stream to MIMEUtil will result in the exception:
			 *       
			 *       Caught Exception: eu.medsea.mimeutil.MimeException: InputStream must support the mark() and reset() methods.
			 *       eu.medsea.mimeutil.MimeUtil2.getMimeTypes(MimeUtil2.java:478)
			 *		 eu.medsea.mimeutil.MimeUtil2.getMimeTypes(MimeUtil2.java:455)
			 *
			 *		Instead supply a byte array of the intial bytes in the stream, enough so that the magic mime detector can make 
			 *		a good guess but not too much as to use a lot of memory - cannot read the whole file as it may be huge! 
			 *
			 *		A good compromise is to pass the byte array through the magic mime detector and see if a match is found, if not
			 *		pass the file name through the extension detector as a last resort and see if a match can be found that way.
			 */
			
			InputStream is = this.b.retrieve();
		
			
			boolean foundZip = false;
			if (is != null){
				// Read some bytes and close the stream
				byte[] byteArr = fillBuffer(is);
				try {is.close();} catch (Exception e){}
				
				logger.debug("Detecting file type using byte array approach ...");
				foundZip = isZip(byteArr); 
				logger.debug("foundZip = " + foundZip);
			} else {
				logger.warn("Could not retrieve InputStream for bitstream " + this.b.getID());
			}
			
			if (!foundZip){
				logger.debug("Detecting file type using file name approach ... filename = " + this.b.getName());
				// Try the extension detector now
				foundZip = isZip(this.b.getName());
				logger.debug("foundZip = " + foundZip);
			}
			
			// First of all check if it is a Zip file
			if (foundZip){
				// Now we need to iterate through the zip entries and find a manifest file
				
				// NOTE: Must get the InputStream again - the call to isZip will have read from the stream
				InputStream contentsStream = this.b.retrieve();
				
				// Shouldn't be null if we got here but check anyway just in case!
				if (contentsStream != null){
					zip = new ZipInputStream(contentsStream);
					ZipEntry ze;
					while ((ze = zip.getNextEntry()) != null) {
						String fname = ze.getName();
						// Manifest must be at top level directory - therefore shouldn't have a "/" in name
						if (!fname.contains("/") && fname.compareTo(manifestName) == 0){
							// found manifest - now parse the manifest and return as a Document
							// NOTE: validation set based on configuration - already read in PackageDetectorStep class
							// Don't want the stream closed - we will do that in the finally block, hence use of UnclosableInputStream
							result = XMLManifest.parseManifest(new PackageUtils.UnclosableInputStream(zip), PackageDetectorStep.validate);
							break;
						}
					}
					
				} else {
					logger.warn("Could not retrieve InputStream for bitstream " + this.b.getID());
				}
				
			}
			
		} catch (Exception e){
			ExceptionLogger.logException(logger, e);
		} finally {
			try {zip.close();} catch (Exception e){} 
		}
		
		return result;
	}
	
}
