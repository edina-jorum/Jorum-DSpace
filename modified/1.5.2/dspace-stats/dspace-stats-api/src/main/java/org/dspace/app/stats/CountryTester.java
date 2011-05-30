package org.dspace.app.stats;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.*;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

class CountryTester
{

   public static void main(String[] args)
   {
       Context context = null;
       
       try
       {
           context = new Context();
       
           System.out.println("Country: " + Country.getCountry(context, args[0]));
           
           context.commit();
       }
       catch (SQLException e)
       {
          System.out.println("Database error: " + e.getMessage());
          System.exit(1);
       }
       finally
       {
          if ((context != null) && context.isValid())
             context.abort();
       }                
   }

}
