/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : Sequencer.java
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gwaller
 *
 */
public class Sequencer {

	private static Sequencer instance = new Sequencer();
	
	private AtomicInteger sequenceNumber = new AtomicInteger(0);
	
	private Sequencer(){
		
	}
	
	public static Sequencer getInstance(){
		return instance;
	}
	
	public int next() { 
		return sequenceNumber.getAndIncrement(); 
	}
	
	
}
