/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : DeleteOwnerItems.java
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
public class DeleteOwnerItems extends CommandLineDelete{

	public void usage(){
		System.out.println("Usage: " + DeleteOwnerItems.class.getCanonicalName() + " <admin email> <depositor email>");
		System.exit(1);
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeleteOwnerItems instance = new DeleteOwnerItems();
		
		if (args.length != 2){
			instance.usage();
		}
		
		try{
			instance.performDelete(false, false, args[0], args[1]);
		} catch (Exception e){
			e.printStackTrace();
		}
		


	}

}
