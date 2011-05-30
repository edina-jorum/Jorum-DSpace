/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : AnonItemExportDownloadReader.java
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpResponse;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.cocoon.util.ByteRange;
import org.dspace.app.itemexport.AnonItemExport;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

/**
 * @author Ian Fieldhouse (ianfi)
 */

public class AnonItemExportDownloadReader extends AbstractReader implements
		Recyclable {

	/**
     * How big of a buffer should we use when reading from the bitstream before
     * writing to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a download expire in milliseconds. This should be set to
     * some low value just to prevent someone hitting DSpace repeatedly from
     * killing the server. Note: 60000 milliseconds are in a second.
     * 
     * Format: minutes * seconds * milliseconds
     */
    protected static final int expires = 60 * 60 * 60000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    /** The bitstream file */
    protected InputStream compressedExportInputStream;
    
    /** The compressed export's reported size */
    protected long compressedExportSize;
    
    protected String compressedExportName;
    /**
     * Set up the export reader.
     * 
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
            this.request    = ObjectModelHelper.getRequest(objectModel);
            this.response   = ObjectModelHelper.getResponse(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);

            /*
             *  Get our parameters that identify the bitstream
             */
            String fileName = par.getParameter("fileName", null);
            
                
            /*
             *  Success, bitstream found.
             *  Store these for later retrieval.
             */
            this.compressedExportInputStream = AnonItemExport.getExportDownloadInputStream(fileName);
            this.compressedExportSize        = AnonItemExport.getExportFileSize(fileName);
            this.compressedExportName        = fileName;
        }
        catch (Exception e)
        {
            throw new ProcessingException("Unable to read bitstream.",e);
        } 
    }

    
    /**
	 * Write the actual data out to the response.
	 * 
	 * Some implementation notes,
	 * 
	 * 1) We set a short expires time just in the hopes of preventing someone
	 * from overloading the server by clicking reload a bunch of times. I
	 * realize that this is nowhere near 100% effective but it may help in some
	 * cases and shouldn't hurt anything.
	 * 
	 */
    public void generate() throws IOException, SAXException, ProcessingException
    {
    	if (this.compressedExportInputStream == null)
	    	return;
    	
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = -1;

        response.setDateHeader("Expires", System.currentTimeMillis() + expires);
        response.setHeader("Content-disposition","attachement; filename=" + this.compressedExportName );
        
        /*
         *  Turn off partial downloads, they cause problems and are only rarely used. 
         *  Specifically some windows PDF viewers are incapable of handling this request. 
         *  By uncommenting the following two lines you will turn this feature back on.
         */
        // response.setHeader("Accept-Ranges", "bytes");
        // String ranges = request.getHeader("Range");
        
        String ranges = null;
        
        ByteRange byteRange = null;
        if (ranges != null)
        {
            try
            {
                ranges = ranges.substring(ranges.indexOf('=') + 1);
                byteRange = new ByteRange(ranges);
            }
            catch (NumberFormatException e)
            {
                byteRange = null;
                if (response instanceof HttpResponse)
                {
                    /*
                     *  Respond with status 416 (Request range not satisfiable)
                     */
                    ((HttpResponse) response).setStatus(416);
                }
            }
        }

        if (byteRange != null)
        {
            String entityLength;
            String entityRange;
            if (this.compressedExportSize != -1)
            {
                entityLength = "" + this.compressedExportSize;
                entityRange = byteRange.intersection(new ByteRange(0, this.compressedExportSize)).toString();
            }
            else
            {
                entityLength = "*";
                entityRange = byteRange.toString();
            }

            response.setHeader("Content-Range", entityRange + "/" + entityLength);
            if (response instanceof HttpResponse)
            {
                /*
                 *  Response with status 206 (Partial content)
                 */
                ((HttpResponse) response).setStatus(206);
            }

            int pos = 0;
            int posEnd;
            while ((length = this.compressedExportInputStream.read(buffer)) > -1)
            {
                posEnd = pos + length - 1;
                ByteRange intersection = byteRange.intersection(new ByteRange(pos, posEnd));
                if (intersection != null)
                {
                    out.write(buffer, (int) intersection.getStart() - pos, (int) intersection.length());
                }
                pos += length;
            }
        }
        else
        {
            response.setHeader("Content-Length", String.valueOf(this.compressedExportSize));

            while ((length = this.compressedExportInputStream.read(buffer)) > -1)
            {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    /**
     * Returns the mime-type of the bitstream.
     */
    public String getMimeType()
    {
    	return AnonItemExport.COMPRESSED_EXPORT_MIME_TYPE;
    }
    
    /**
	 * Recycle
	 */
    public void recycle() {        
        this.response = null;
        this.request = null;
        this.compressedExportInputStream = null;
        this.compressedExportSize = 0;
        
    }

}
