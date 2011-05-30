/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : NonCriticalException.java
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
package uk.ac.jorum.exceptions;

/**
 * An instance of this class denotes an exception which is not critical and can result in continuation of the thread on it's normal path. The 
 * Exception should probably be logged however.
 * @author gwaller
 *
 */
public class NonCriticalException extends Exception {

	public NonCriticalException(String message){
		super(message);
	}
	
	public NonCriticalException(String message, Throwable e){
		super(message, e);
	}
	
}
