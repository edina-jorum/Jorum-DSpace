/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *
 *  University Of Edinburgh (EDINA) 
 *  Scotland
 *
 *
 *  File Name           : GeneratePreviews.java
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

import uk.ac.jorum.exceptions.NonCriticalException;

/**
 * @author gwaller
 *
 */
public class GeneratePreviews {

	public static void usage(){
		System.out.println("Usage: " + GeneratePreviews.class.getCanonicalName() + " <admin email> [min handle] [max handle]");
		System.out.println("");
		System.out.println("Example All Handles: " + GeneratePreviews.class.getCanonicalName() + " g.waller@ed.ac.uk");
		System.out.println("\tThe above will only examine all handles");
		System.out.println("");
		System.out.println("Example Handle Min: " + GeneratePreviews.class.getCanonicalName() + " g.waller@ed.ac.uk 123456789/870");
		System.out.println("\tThe above will only examine handles 123456789/870 and above");
		System.out.println("");
		System.out.println("Example Handle Min and Max: " + GeneratePreviews.class.getCanonicalName() + " g.waller@ed.ac.uk 123456789/870 123456789/900");
		System.out.println("\tThe above will only examine handles between 123456789/870 and 123456789/900 inclusive");
		System.out.println("");
		System.exit(1);
	}
	
	
	public static void confirmDelete() throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	    String answer = "";
	    
	    while (answer.compareToIgnoreCase("y") != 0 && answer.compareToIgnoreCase("n") != 0){
	    	System.out.println();
	    	System.out.print("Are you sure you wish to re-generate all the preview bitstreams? This will permanently erase the previous preview and cannot be undone!!! Y/N:");
		    answer = br.readLine().trim();
	    }
	    
	    if (answer.compareToIgnoreCase("n") == 0){
	    	System.exit(0);
	    }
	     
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1 || args.length > 3){
			usage();
		}
		
		if (args[0].equals("-h") || args[0].equals("--help")){
			usage();
		}
		
		try{
			confirmDelete();
			
			
			Context context = new Context();
			
			EPerson myEPerson = EPerson.findByEmail(context, args[0]);
	        if (myEPerson == null){
	            System.out.println("Error, eperson cannot be found: " + args[0]);
	            usage();
	        }
	        context.setCurrentUser(myEPerson);
						
			String sql = "SELECT handle FROM handle ";
	        
			if (args.length > 1){
				sql += " where handle >= '" +  args[1] + "' ";
			}
			
			if (args.length > 2){
				sql += " and handle <= '" + args[2] + "' ";
			}
			
			sql += "order by handle";
			
			
			System.out.println("Executing SQL: " + sql);
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
						System.out.println("Calling postInstallHook on item with handle: " + handle);
						try{
							((Item)obj).postInstallHook(context);
					
						} catch (NonCriticalException e3){
							System.err.println("Non-critical exception caught calling postInstallHook:");
							e3.printStackTrace();
						}catch (Exception e2){
							System.err.println("Exception calling postInstallHook:");
							e2.printStackTrace();
						}
					}
				}
			}
	        
			context.complete();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
		
		
		

	}

}
