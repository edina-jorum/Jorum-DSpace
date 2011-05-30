/*
 * JPEGPDFFilter.java
 *
 * Version: $Revision: 1.8 $
 *
 * Date: $Date: 2005/07/29 15:56:07 $
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
package org.dspace.app.mediafilter;

import java.lang.Runnable;
import java.lang.Process;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import org.dspace.core.ConfigurationManager;


public class JPEGPDFFilter extends MediaFilter
{
    public String getFilteredName(String oldFilename)
    {
        return oldFilename + ".jpg";
    }

    /**
     * @return String bundle name
     *  
     */
    public String getBundleName()
    {
        return "THUMBNAIL";
    }

    /**
     * @return String bitstreamformat
     */
    public String getFormatString()
    {
        return "jpeg";
    }

    /**
     * @return String description
     */
    public String getDescription()
    {
        return "Thumbnail";
    }

    /**
     * @param source
     *            source input stream
     * 
     * @return InputStream the resulting input stream
     */
    public InputStream getDestinationStream(InputStream source)
            throws Exception
    {
    	    int exit_value = 0;
    	    String pdfConversionTool = ConfigurationManager.getProperty("presentation.pdf.conversion.tool");
    	    String conversionToolOptions = ConfigurationManager.getProperty("presentation.pdf.conversion.tool.options");
            File infile = new File(ConfigurationManager.getProperty("upload.temp.dir") + File.separator + "infile.pdf");

    		BufferedInputStream bis = new BufferedInputStream(source);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(infile));
            int b=0;
            while((b=bis.read()) > -1)
            {
            	bos.write(b);
            }
            
            bos.close();
            bis.close();
    		
    		File outfile = new File(ConfigurationManager.getProperty("upload.temp.dir") + File.separator + "outfile.jpg");

    		String cmd = pdfConversionTool + " " + infile.getCanonicalPath() + "[0]";
    		
   			cmd = cmd + " " + conversionToolOptions + " "  + outfile.getCanonicalPath();


    		Runtime rt = Runtime.getRuntime();
    		Process p = null;

    		try
    		{
    			p = rt.exec(cmd);
    			exit_value = p.waitFor();
    		}
    		catch (IOException e)
    		{
    			System.out.println("IOException - Command failed: " + cmd);
    			throw new Exception(e.getMessage() + "\nFailed while running: " + cmd);
    		}

    		p.destroy();

    		if (exit_value != 0) // Process did not execute OK
    		{
    			System.out.println("Command failed: " + cmd + " with code " + exit_value);
    			throw new Exception("Failed while running: " + cmd);
    		}
    		
    		return new FileInputStream(outfile);
    }
}
