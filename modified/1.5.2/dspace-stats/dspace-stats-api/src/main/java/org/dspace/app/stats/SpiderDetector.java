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

class SpiderDetector
{
    private static boolean isVerbose = false;
    private static String pathToWeblog = null;
    
   private static List ipSpider = new ArrayList();
   private static List agents = new ArrayList();

   private static List insertedSpider = new ArrayList();
   private static List insertedAgent = new ArrayList();

   private static List compareStrings = new ArrayList();

   public static void main(String[] args)
   {
       readCommandLineOptions(args);
       
       
      Context context = null;

      try
      {
          context = new Context();
          findSpiders(context, pathToWeblog);
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

   private static void findSpiders(Context context, String filePath) throws SQLException
   {
      try
      {
         BufferedReader in = new BufferedReader(new FileReader(filePath));
         String lastDate = null;
         String line;
         boolean find = false;
         String expression = "^([^ ]+) ([^ ]+) ([^ ]+) \\[(.*)\\] \"(.*) (.*) (.*)\" ([0-9\\-]+) ([0-9\\-]+) \"(.*)\" \"(.*)\"$";
         String ip = null;
         String date = null;
         String request = null;
         String agent = null;
         String referer = null;

         getIpSpiders(context);
         getAgents(context);
         getCompareStrings(context);

         // get last processed date
         String myQuery = "select * from stats.control";
         TableRowIterator iterator = DatabaseManager.query(context, myQuery);
         if (iterator.hasNext())
         {
            TableRow row = iterator.next();
            lastDate = row.getStringColumn("last_line_log");
         }
         //

         if (lastDate == null) find = true;

         while ((line = in.readLine()) != null)
         {
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(line);

            if(matcher.matches())
            {
               ip = matcher.group(1);
               date =  matcher.group(4);
               request = matcher.group(6);
               agent = matcher.group(11);
               referer = matcher.group(10);

               if (!find && date.equals(lastDate)) find = true;

               if (find)
               {
                  if (!isSpider(ip))
                  {

                     if (isAgent(agent))
                     {
                        if (!insertedSpider.contains(ip))
                        {
                           insertAgentStaging(context, ip, agent, 1);
                           insertedSpider.add(ip);
                        }
                     }
                     else
                     {
                         boolean match = false;
 
                         for(int i=0;i<compareStrings.size();i++)
                         {
                            if (agent.indexOf((String)compareStrings.get(i)) >= 0)
                            {
                               match = true;
                               break;
                            }
                         }

                         if (match)
                         {
                             if (!insertedAgent.contains(agent))
                             {
                                insertAgentStaging(context, ip, agent, 2);
                                insertedAgent.add(agent);
                             }
                         }
                         else
                         {
                            if (request.indexOf("testpage.html")>=0)
                            {
                               if (referer.equals("-"))
                               {
                                  insertAgentStaging(context, ip, agent, 3);
                                  insertedAgent.add(agent);
                               }
                            }
                            else
                            {
                               if (request.indexOf("robots.txt")>=0 && !isSpider(ip))
                               {
                                  if (!insertedAgent.contains(agent))
                                  {
                                     insertAgentStaging(context, ip, agent, 4);
                                     insertedAgent.add(agent);
                                  }
                               }
                            }
                        }
                     }
                  }
               }
            }
            context.commit();
         }

         in.close();
        
         // set last processed date
         if (date != null)
         {
            String sql = "update stats.control set last_line_log='" + date + "'";
            DatabaseManager.updateQuery(context, sql);
            context.commit();
         }
         //
      }
      catch(IOException e)
      {
         System.err.println("Error: Can't read file: " + filePath);
      }
   }

   private static void insertAgentStaging(Context context, String ip, String agent, int type) 
   {
      String sql = "";
      try
      {
         sql = "select agent_id from stats.agent_staging where ip=? and name =?";
         TableRowIterator iterator = DatabaseManager.query(context, sql, ip, agent);
         if (!iterator.hasNext())
         {
            sql = "insert into stats.agent_staging values (getnextid('stats.agent_staging'),?, ?, ?)";

            DatabaseManager.updateQuery(context, sql, agent, ip, type);
         }
      }
      catch (SQLException e)
      {
          System.out.println("Cant execute sql: " + sql);
      }
   }

   private static void getIpSpiders(Context context) throws SQLException
   {
      String sql = "SELECT distinct ip FROM stats.ip_spider ;";
      TableRowIterator iterator = DatabaseManager.query(context, sql);
      while (iterator.hasNext())
      {
         TableRow r = iterator.next();
         ipSpider.add(r.getStringColumn("ip"));
      }
   }

   private static boolean isSpider(String ip) throws SQLException
   {
      return ipSpider.contains(ip);
   }

   private static void getAgents(Context context) throws SQLException
   {
      String sql = "SELECT * FROM stats.agent ;";
      TableRowIterator iterator = DatabaseManager.query(context, sql);
      while (iterator.hasNext())
      {
         TableRow r = iterator.next();
         agents.add(r.getStringColumn("name"));
      }
   }

   private static boolean isAgent(String agent)
   {
      return agents.contains(agent);
   }

   private static void getCompareStrings(Context context) throws SQLException
   {
      String sql = "SELECT * FROM stats.temp order by name;";
      TableRowIterator iterator = DatabaseManager.query(context, sql);
      while (iterator.hasNext())
      {
         TableRow r = iterator.next();
         compareStrings.add(r.getStringColumn("name"));
      }
   }

   private static Options setCommandLineOptions()
   {
       
       // create an options object and populate it
       Options options = new Options();
            
       OptionBuilder.withLongOpt("path");
       OptionBuilder.withDescription(
                      "Run the detection spiders for the specified weblog file.\n" +
                      "Must be in combined format"); 
          
       Option path = OptionBuilder.create('p');
       path.setArgs(1);
       options.addOption(path);    
    
       options.addOption("v", "verbose", false, "print detection logging to STDOUT");
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
           new HelpFormatter().printHelp("SpiderDetector\n", options);
           System.exit(1);
       }

       if (line.hasOption('h'))
       {
           new HelpFormatter().printHelp("SpiderDetector\n", options);
           System.exit(0);
       }

       if (line.hasOption('v'))
       {
           isVerbose = true;
       }
       if (!line.hasOption('p'))
       {
           System.out.println("You have to specify a path to the weblog file\n");
           
           new HelpFormatter().printHelp("SpiderDetector\n", options);
           System.exit(0);
       }

       if(line.hasOption('p'))
       {
           pathToWeblog = line.getOptionValue('p');
       }
   }

}
