/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : ExceptionLogger.java
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
package uk.ac.jorum.utils;

import org.apache.log4j.Logger;

/**
 * @author gwaller
 *
 */
public class ExceptionLogger {

	/**
	 * This method logs the full stack trace in the Exception to the supplied Log4j logger
	 * @param logger the log4j logger to use
	 * @param e the exception to trace
	 */
	public static void logException(Logger logger, Exception e){
		
		String stackTrace = getStackTrace(e);
		
		logger.error(stackTrace);
		
	}
	
	/**
	 * This method obtains the full stack trace of the exception and returns it as a String
	 * @param e the exception to obtain the stack trace from
	 * @return String containing the full stack trace
	 */
	public static String getStackTrace(Exception e){
		StackTraceElement[] frames = e.getStackTrace();
		StringBuffer sb = new StringBuffer(300);
		
		sb.append("Caught Exception: " + e.toString() + "\n");
		for (int i = 0; i < frames.length; i++){
			sb.append("\t");
			sb.append(frames[i].toString());
			sb.append("\n");
		}
		
		// Recusively dump all the causes
		dumpCauses(sb, e);
		
		return sb.toString();
	}
	
	
	
	/**
	 * This method recursively appends the stack trace of the cause of the exception to the supplied StringBuffer.
	 * @param sb the StringBuffer to append to, must not be null.
	 * @param throwable the throwable to examine and get the cause from
	 */
	private static void dumpCauses(StringBuffer sb, Throwable throwable){
		
		Throwable cause = throwable.getCause();
		
		if (cause != null){
			sb.append("Caused by: " + cause.toString() + "\n");
			StackTraceElement[] cframes = cause.getStackTrace();
			for (int i = 0; i < cframes.length; i++){
				sb.append("\t");
				sb.append(cframes[i].toString());
				sb.append("\n");
			}
			
			// recursively parse the cause of the cause
			dumpCauses(sb, cause);
		}
		
	}
	
}
