/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : WithdrawOwnerItems.java
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
package uk.ac.jorum.dspace.utils;



/**
 * @author gwaller
 *
 */
public class WithdrawOwnerItems extends CommandLineDelete{

	public void usage(){
		System.out.println("Usage: " + WithdrawOwnerItems.class.getCanonicalName() + " <admin email> <depositor email>");
		System.exit(1);
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WithdrawOwnerItems instance = new WithdrawOwnerItems();
		
		if (args.length != 2){
			instance.usage();
		}
		
		try{
			instance.performDelete(false, true, args[0], args[1]);
		} catch (Exception e){
			e.printStackTrace();
		}
		


	}

}
