/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : WithdrawAllItems.java
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
public class WithdrawAllItems extends CommandLineDelete{

	public void usage(){
		System.out.println("Usage: " + WithdrawAllItems.class.getCanonicalName() + " <admin email>");
		System.exit(1);
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		WithdrawAllItems instance = new WithdrawAllItems();
		
		if (args.length != 1){
			instance.usage();
		}
		
		try{
			
			System.out.println();
			System.out.println();
			System.out.println("**************************************");
			System.out.println("*");
			System.out.println("*  !!!!  WARNING  !!!!");
			System.out.println("*");
			System.out.println("* This utility will withdraw ALL items from DSpace");
			System.out.println("*");
			System.out.println("**************************************");
			System.out.println();
			System.out.println();
			
			instance.performDelete(true, true, args[0], null);
		} catch (Exception e){
			e.printStackTrace();
		}
		


	}

}
