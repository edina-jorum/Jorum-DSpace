/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : CommandLineDelete.java
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author gwaller
 *
 */
public abstract class CommandLineDelete {

	public abstract void usage();
	
	
	public static void confirmDelete(boolean withdraw) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	    String answer = "";
	    
	    while (answer.compareToIgnoreCase("y") != 0 && answer.compareToIgnoreCase("n") != 0){
	    	System.out.println();
	    	String term = (withdraw)?"withdraw":"delete";
	    	String message = "Are you sure you wish to " + term + " items from DSpace? ";
	    	
	    	if (!withdraw){
	    		message += " This will permanently erase the content and cannot be undone!!! ";
	    	}
	    	
	    	System.out.print(message + "Y/N:");
		    answer = br.readLine().trim();
	    }
	    
	    if (answer.compareToIgnoreCase("n") == 0){
	    	System.exit(0);
	    }
	     
	}
	
	public void performDelete(boolean deleteAll, boolean withdraw, String adminEmail, String ownerEmail) throws Exception{
	
		confirmDelete(withdraw);

		Context context = new Context();
		
		EPerson adminPerson = EPerson.findByEmail(context, adminEmail);
		EPerson ownerPerson = null;
		
		if (!deleteAll){
			ownerPerson = EPerson.findByEmail(context, ownerEmail);
			if (ownerPerson == null){
	            System.out.println("Error, eperson cannot be found: " + ownerEmail);
	            usage();
	        }
		} 

        if (adminPerson == null){
            System.out.println("Error, admin cannot be found: " + adminEmail);
            usage();
        }
        context.setCurrentUser(adminPerson);
					
		String sql = "SELECT handle FROM handle";
        TableRowIterator iterator = DatabaseManager.queryTable(context, null, sql);
        ArrayList<String> results = new ArrayList<String>();

        try
        {
            while (iterator.hasNext())
            {
                TableRow row = (TableRow) iterator.next();
                results.add(row.getStringColumn("handle"));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (iterator != null)
                iterator.close();
        }
		
		if (results.size() > 0){
			for (String handle : results){
				DSpaceObject obj = HandleManager.resolveToObject(context, handle);
				if (obj != null && obj instanceof Item){
					
					try{
						Item item = (Item)obj;
						
						boolean safeToDelete = false;
						if (!deleteAll){
							// Check owner
							safeToDelete = (ownerPerson.getID() == item.getSubmitter().getID());
						} else {
							safeToDelete = true;
						}
						
						
						if (safeToDelete){
							
							if (withdraw){
								System.out.println("Withdrawing item with handle: " + handle);
								item.withdraw();
							} else {
								System.out.println("Deleting item with handle: " + handle);
								Collection[] collections = item.getCollections();
								if (collections != null){
									for (Collection c : collections){
										// Once the item is removed from the last owning collection it is deleted
										c.removeItem(item);
										
									}
								}
							}
							
							
						}
						
						
						
					} catch (Exception e2){
						System.err.println("Exception deleting item:");
						e2.printStackTrace();
					}
				}
			}
		}
        
		context.complete();
	
	}
	
	
	
	
	
}
