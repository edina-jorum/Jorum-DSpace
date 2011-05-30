package org.dspace.app.stats;

import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

class Spider
{

    private static boolean isVerbose = false;
    private static boolean add = false;
    private static boolean remove = false;
    private static String ip;
    private static String agent;
    
   public static void main(String[] args)
   {
       readCommandLineOptions(args);
       
       
      Context context = null;

      try
      {
          context = new Context();

          if (add)
              insertSpider(context, ip, agent);
          if (remove)
              removeSpider(context, ip);
          
          context.commit();
      }
      catch (SQLException e)
      {
          System.out.println("Database error: " + e.getMessage());
      }
      finally
      {
          if ((context != null) && context.isValid())
              context.abort();
      }
   }

   private static void insertSpider(Context context, String ip, String agent) throws SQLException
   {
       Integer agentId = null;
       
       String sql = "SELECT agent_id FROM stats.agent WHERE name = ?";
       TableRow row = DatabaseManager.querySingle(context, sql, agent);
       if (row == null)
       {
           String sqlID = "select getnextid('stats.agent') as id";
           row = DatabaseManager.querySingle(context, sqlID);
           agentId = row.getIntColumn("id");

           sql = "insert into stats.agent (agent_id, name, count) " +
           "values (?, ?, 0)";
     
           DatabaseManager.updateQuery(context, sql, agentId, agent);
       }
       else
       {
           agentId = row.getIntColumn("agent_id");
       }
 
       sql = "select ip from stats.ip_spider where ip = ? and agent_id = ?";
       row = DatabaseManager.querySingle(context, sql, ip, agentId);
 
       if (row == null)
       {
           String country;
           String countryCode = null;
           String countryName = null;

           country = Country.getCountry(context, ip);

           String[] temp = country.split(";");

           if (temp.length == 2)
           {
               countryCode = temp[0];
               countryName = temp[1];
               checkCountry(context, countryCode, countryName);
           }

           sql = "insert into stats.ip_spider (ip, agent_id, country_code) " +
                 "values (?, ?, ?)";
           DatabaseManager.updateQuery(context, sql, ip, agentId, countryCode);
         
           sql = "delete from stats.ip_keepwatched where ip = ?";
           DatabaseManager.updateQuery(context, sql, ip);
       }
   }

   private static void removeSpider(Context context, String ip) throws SQLException
   {
       String sql = "delete from stats.ip_spider where ip = ?";
       DatabaseManager.updateQuery(context, sql, ip);
   }

   private static void checkCountry(Context context, String countryCode, String countryName) throws SQLException
   {
       TableRow row = DatabaseManager.findByUnique(context, "stats.country", "code", countryCode);

       // If the country does not exist in the table, insert it
       if (row == null)
       {
           String sql = "insert into stats.country (code, name) values (?, ?)";
           DatabaseManager.updateQuery(context, sql, countryCode, countryName);
       }
   }   

   private static Options setCommandLineOptions()
   {
       
       // create an options object and populate it
       Options options = new Options();
            
       OptionBuilder.withLongOpt("add");
       OptionBuilder.withDescription(
                      "Add a spider.\n" +
                      "args format: \"IP\", \"agentname\" (example: Spider -a \"127.0.0.1\",\"Googlebot...\" )\n" +
                      "You must clean/aggregate the aggregation to \n" + 
                      "changes take effect on aggregations.\n"); 
       
       Option add = OptionBuilder.create('a');
       add.setArgs(1);
       options.addOption(add);    

       OptionBuilder.withLongOpt("remove");
       OptionBuilder.withDescription(
                      "Remove a spider.\n" +
                      "args format: IP (example: Spider -r \"127.0.0.1\")\n" +
                      "You must clean/aggregate the aggregation to \n" + 
                      "changes take effect on aggregations.\n");    
       Option remove = OptionBuilder.create('r');
       remove.setArgs(1);
       options.addOption(remove);    

       options.addOption("v", "verbose", false, "print logging to STDOUT");
       options.addOption("h", "help", false, "help");
       
       return options;
   }

   private static void readCommandLineOptions(String[] argv)
   {
       // set up command line parser
       CommandLineParser parser = new PosixParser();
       CommandLine line = null;

       Options options = setCommandLineOptions();
       
       try
       {
           line = parser.parse(options, argv);
       }
       catch(MissingArgumentException e)
       {
           System.out.println("Missing Argument: " + e.getMessage());
           new HelpFormatter().printHelp("SpiderDetector\n", options);
           System.exit(1);
       }          
       catch (ParseException e)
       {
           System.out.println("ERROR: " + e.getMessage());
           new HelpFormatter().printHelp("Spider\n", options);
           System.exit(1);
       }

       if (line.hasOption('h'))
       {
           new HelpFormatter().printHelp("Spider\n", options);
           System.exit(0);
       }

       if (line.hasOption('v'))
       {
           isVerbose = true;
       }
       
       if (!line.hasOption('a') && !line.hasOption('r'))
       {
           System.out.println("You have to specify an operation: add or remove.");
           new HelpFormatter().printHelp("Spider\n", options);
           System.exit(1);
       }

       if (line.hasOption('a') && line.hasOption('r'))
       {
           System.out.println("You have to specify only one operation: add or remove.");
           new HelpFormatter().printHelp("Spider\n", options);
           System.exit(1);
       }

       String params = null;
       if(line.hasOption('a'))
       {
           params = line.getOptionValue('a');
           if (params.indexOf(",") == -1)
           {
               System.out.println("Specify de add params: \"IP\",\"agentname\"\n" +
                               "(example: \"127.0.0.1\",\"Googlebot+ http://www...\")");
                new HelpFormatter().printHelp("Spider\n", options);
                System.exit(1);               
           }
           else
           {
               ip = params.substring(0, params.indexOf(",")).trim();
               agent = params.substring(params.indexOf(",") + 1).trim();
               add = true;               
           }                    
       }
       
       String removeParams[] = null;
       if(line.hasOption('r'))
       {
           removeParams = line.getOptionValues('r');                    

           if(removeParams==null || removeParams.length==0)
           {   
               System.out.println("Specify de remove params: \"IP\" \n" +
                                  "(example: \"127.0.0.1\")");
               new HelpFormatter().printHelp("Spider\n", options);
               System.exit(1);
           }
           else
           {
               ip = removeParams[0].trim();
               remove = true;
           }
       }
   }   
}
