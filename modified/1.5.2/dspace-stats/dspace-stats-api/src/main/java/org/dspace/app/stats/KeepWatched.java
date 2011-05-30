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

class KeepWatched
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
              insertKeepWatched(context, ip, agent);
          if (remove)
              removeKeepWatched(context, ip);
          
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

   private static void insertKeepWatched(Context context, String ip, String agent) throws SQLException
   {
       String sql = "select ip from stats.ip_keepwatched where ip = ?";
       TableRow row = DatabaseManager.querySingle(context, sql, ip);
 
       if (row == null)
       {
           sql = "insert into stats.ip_keepwatched (ip, last_agent, robotstxt, robottraps, manuals) " +
                 "values (?, ?, 0, 0, 0)";
           DatabaseManager.updateQuery(context, sql, ip, agent);         
       }
   }

   private static void removeKeepWatched(Context context, String ip) throws SQLException
   {
       String sql = "delete from stats.ip_keepwatched where ip = ?";
       DatabaseManager.updateQuery(context, sql, ip);
   }

   private static Options setCommandLineOptions()
   {
       
       // create an options object and populate it
       Options options = new Options();
            
       OptionBuilder.withLongOpt("add");
       OptionBuilder.withDescription(
                      "Add a keep watched IP.\n" +
                      "args format: \"IP\", \"agentname\" (example: KeepWatched -a \"127.0.0.1\",\"Googlebot...\" )\n");
       
       Option add = OptionBuilder.create('a');
       add.setArgs(1);
       options.addOption(add);    

       OptionBuilder.withLongOpt("remove");
       OptionBuilder.withDescription(
                      "Remove a keep watched IP.\n" +
                      "args format: IP (example: KeepWatched -r \"127.0.0.1\")\n");
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
           new HelpFormatter().printHelp("KeepWatchedDetector\n", options);
           System.exit(1);
       }          
       catch (ParseException e)
       {
           System.out.println("ERROR: " + e.getMessage());
           new HelpFormatter().printHelp("KeepWatched\n", options);
           System.exit(1);
       }

       if (line.hasOption('h'))
       {
           new HelpFormatter().printHelp("KeepWatched\n", options);
           System.exit(0);
       }

       if (line.hasOption('v'))
       {
           isVerbose = true;
       }
       
       if (!line.hasOption('a') && !line.hasOption('r'))
       {
           System.out.println("You have to specify an operation: add or remove.");
           new HelpFormatter().printHelp("KeepWatched\n", options);
           System.exit(1);
       }

       if (line.hasOption('a') && line.hasOption('r'))
       {
           System.out.println("You have to specify only one operation: add or remove.");
           new HelpFormatter().printHelp("KeepWatched\n", options);
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
                new HelpFormatter().printHelp("KeepWatched\n", options);
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
               new HelpFormatter().printHelp("KeepWatched\n", options);
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
