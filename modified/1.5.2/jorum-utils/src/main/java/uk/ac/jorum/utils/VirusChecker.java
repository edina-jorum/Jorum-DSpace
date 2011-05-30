/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : VirusChecker.java
 *  Author              : George Hamilton
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
 
package uk.ac.jorum.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Check file for viruses using clamscan.
 */
public class VirusChecker
{
    private static final Logger LOG = Logger.getLogger(VirusChecker.class);
    private static final Pattern PATTERN = Pattern.compile(".*Infected files: 0.*");
    
    private File file = null;
    private String clamscanPath = null;
    
    /**
     * Initialise virus checker.
     * @param clamscanPath The full path to the clamscan process.
     * @param file The file to check.
     */
    public VirusChecker(String clamscanPath, File file)
    {
        File clamScan = new File(clamscanPath);
        if(clamScan.exists())
        {
            this.clamscanPath = clamscanPath;
        }
        else
        {
            throw new IllegalStateException(
                    "No valid clamscan executable found: " + clamScan);
        }
        
        if(!file.canRead())
        {
            throw new IllegalStateException(
                    "Can't virus check file: " + file.getName());
        }
        
        this.file = file;
    }
    
    /**
     * Is the file free of viruses?
     * @return True if the file is free of viruses.
     */
    public boolean isVirusFree()
    {
        boolean virusFree = false;
        BufferedReader stdInput = null;
        try
        {
            // spawn clamscan process and wait for result
        	Process p = Runtime.getRuntime().exec(new String[] { this.clamscanPath, file.getPath() });
            p.waitFor();
           
            if(p.exitValue() == 0)
            {
                // good status returned check if pattern is in output 
                stdInput = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));

                String s = null;
                while((s = stdInput.readLine()) != null)
                {
                    Matcher matchHandle = PATTERN.matcher(s);
                    if(matchHandle.find())
                    {                
                        virusFree = true;
                        break;
                    }
                }
            }
        }
        catch(InterruptedException ex)
        {
            ExceptionLogger.logException(LOG, ex);
        }
        catch(IOException ex)
        {
            ExceptionLogger.logException(LOG, ex);
        }finally {	    	
	    	try{stdInput.close();} catch (Exception e){ExceptionLogger.logException(LOG, e);}
	    }  


        if(!virusFree)
        {
            LOG.warn("*** File " + file + " has failed virus check.");
        }

        return virusFree;
    }
}
