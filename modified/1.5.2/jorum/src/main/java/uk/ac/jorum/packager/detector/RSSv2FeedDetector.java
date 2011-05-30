/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : RSSv2FeedDetector.java
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.packager.PackageIngester;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import uk.ac.jorum.dspace.utils.BundleUtils;
import uk.ac.jorum.packager.RSSv2FeedIngester;
import uk.ac.jorum.submit.step.PackageDetectorStep;
import uk.ac.jorum.utils.ExceptionLogger;

/**
 * @author gwaller
 *
 */
public class RSSv2FeedDetector extends BasePackageDetector {

	private static Logger log = Logger.getLogger(RSSv2FeedDetector.class);
	
	public RSSv2FeedDetector(Bitstream b){
		this.setBitstream(b);
	}
	
	public RSSv2FeedDetector(){
		super();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#ingesterClass()
	 */
	@Override
	public Class<? extends PackageIngester> ingesterClass() {
		return RSSv2FeedIngester.class;
	}

	public static Document getRssDocument(Bitstream rssUrlBitstream){
		try{
			return getRssDocument(rssUrlBitstream.getbContext(), rssUrlBitstream.retrieve(), rssUrlBitstream.getBundles()[0]);
		}catch (Exception e){
			ExceptionLogger.logException(log, e);
		}
		
		return null;
	}
	
	public static Document getRssDocument(Context context, InputStream is, Bundle feedBundle){
		GetMethod method = null;
		BufferedReader in = null;
		
		try{
			// Download the feed
			// url is the bitstream contents
			in = new BufferedReader(new InputStreamReader(is));
			String url = in.readLine();

			// need to replace the feed prefix with the HTTP protocol ie replace feed:// with http://
			url = url.replaceAll(PackageDetectorStep.FEED_PREFIX, "http://");
			
			HttpClient client = new HttpClient();
			// Create a method instance.
			method = new GetMethod(url);

			// Provide custom retry handler is necessary
			method.getParams()
					.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

			// Execute the method.
			int statusCode = client.executeMethod(method);

			if (statusCode != HttpStatus.SC_OK) {
				throw new Exception("Method failed: " + method.getStatusLine());
			}

			// Read the response body.
			byte[] responseBody = method.getResponseBody();
		
			// Store the response in a bitstream so that the ingester can use it later
			if (feedBundle != null){
				BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "Text");
				BundleUtils.setBitstreamFromBytes(feedBundle, Constants.FEED_BUNDLE_CONTENTS_NAME, bs_format, responseBody, false);
			}
			
			// Now see if we have a RSS v2.0 document
			SAXBuilder builder = new SAXBuilder(false);
			Document xmlDoc = builder.build(new ByteArrayInputStream(responseBody));
			//XMLOutputter outputPretty = new XMLOutputter(Format.getPrettyFormat());
            //log.debug("Got RSS DOCUMENT:");
            //log.debug(outputPretty.outputString(xmlDoc));
			
            return xmlDoc;
            

	    } catch (Exception e) {
	    	ExceptionLogger.logException(log, e);
	    } finally {
	    	// Release the connection.
	    	method.releaseConnection();
	    	try{in.close();} catch (Exception e){}
	    }  
		
	    return null;
	}
	
	
	/* (non-Javadoc)
	 * @see uk.ac.jorum.packager.detector.BasePackageDetector#isValidPackage()
	 */
	@Override
	public boolean isValidPackage() {
		boolean result = false;
		boolean isUrl = false;
		
		try{
			// Download the feed
			// url is the bitstream contents
			
			// Check that the bitstream belongs in the FEED_BUNDLE - if it isn't then we may be looking at raw content ie not a link to a feed!
			Bundle[] bundles = this.getBitstream().getBundles();
			for (Bundle b:bundles){
				if (b.getName().equals(Constants.FEED_BUNDLE)){
					isUrl = true;
					break;
				}
			}
		
			if (isUrl){
				Document xmlDoc = getRssDocument(this.getBitstream());
				Element root = xmlDoc.getRootElement();
	            
				Attribute version = root.getAttribute("version");
				if (root.getName().equals("rss") && version != null && version.getValue().equals("2.0")){
					result = true;
				}
			}


	    } catch (Exception e) {
	    	ExceptionLogger.logException(log, e);
	    } 
		
		return result;
	}

}
