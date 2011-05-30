/*
 * StatsAggregator.java
 *
 * Version: $Revision:  $
 *
 * Date: $Date:  $
 *
 * Copyright (C) 2008, the DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of the DSpace Foundation nor the names of their
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.stats;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.List;
import java.util.ArrayList;

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

public class StatsAggregator
{

    private static boolean isVerbose = false;
    private static boolean aggregate = false;
    private static boolean clean = false;
    private static List tables = new ArrayList();
    private static String tablesLine = null;
    
    private static ArrayList<EventTable> eventTables = new ArrayList<EventTable>();
    
    public static void main(String[] argv) throws Exception
    {        
        readCommandLineOptions(argv);
        
        initAggregations();
        
        processAggregations();   
    }
    
    private static void processAggregations()
    {        
        if (isVerbose)
            System.out.println("PROCESSING tables: " + tablesLine);

        Context context = null;
        
        try
        {
            context = new Context();

            for (EventTable table : eventTables)
            {
                if (isVerbose)
                    System.out.println("Processing table " + table.getTable());

                if (clean)
                {
                    for (Aggregation a : table.getAggregations()) 
                    {
                        if (isVerbose)
                            System.out.println("      " + new Time(new java.util.Date().getTime()).toString() + 
                                               "\tCleaning " + a.getDestination());
                        a.clean(context);
                    }

                    if (isVerbose)
                        System.out.println("      " + new Time(new java.util.Date().getTime()).toString() + 
                                           "\tFlagging records as unaggregated ");
                    
                    table.flagAsUnaggregated(context);
                }

                
                if (aggregate)
                {
                    if (isVerbose)
                        System.out.println("      " + new Time(new java.util.Date().getTime()).toString() +  
                                           "\tFlagging spiders ");

                    table.flagSpiders(context);
                    
                    for (Aggregation a : table.getAggregations()) 
                    {
                        if (isVerbose)
                            System.out.println("      " + new Time(new java.util.Date().getTime()).toString() + 
                                               "\tAggregating " + a.getDestination());
                        a.aggregate(context);
                    }

                    if (isVerbose)
                        System.out.println("      " + new Time(new java.util.Date().getTime()).toString() + 
                                           "\tFlagging records as aggregated ");
                    
                    table.flagAsAggregated(context);
                }
                
                if (isVerbose)
                    System.out.println("   End Processing table " + table.getTable());

            }            
            
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
        
        if (isVerbose)
            System.out.println("END PROCESSING ");

    }
    
    private static void initAggregations()
    {
        EventTable table;
        Aggregation agg;

        for (int i = 0; i < tables.size(); i++)
        {
            if (tables.get(i).equals("view"))
            {                   
                table = new EventTable("stats.view");
                
                agg = new Aggregation("stats.z_view_unagg_month", "stats.view_month");
                agg.addValueColumn("value", Types.BIGINT);                
                table.addAggregation(agg);
                
                agg = new Aggregation("stats.z_view_unagg_country_month", "stats.view_country_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                table.addAggregation(agg);
                
                agg = new Aggregation("stats.z_view_unagg_item_month", "stats.view_item_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                table.addAggregation(agg);
                
                agg = new Aggregation("stats.z_view_unagg_comm_month", "stats.view_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_coll_month", "stats.view_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_country_comm_month", "stats.view_country_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_country_coll_month", "stats.view_country_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_item_comm_month", "stats.view_item_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_item_coll_month", "stats.view_item_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_month_1", "stats.view_metadata_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_month_2", "stats.view_metadata_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_comm_month_1", "stats.view_metadata_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_comm_month_2", "stats.view_metadata_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_coll_month_1", "stats.view_metadata_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_view_unagg_metadata_coll_month_2", "stats.view_metadata_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                eventTables.add(table);
            }
            if (tables.get(i).equals("download"))
            {
                table = new EventTable("stats.download");
                
                agg = new Aggregation("stats.z_download_unagg_month", "stats.download_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_country_month", "stats.download_country_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                table.addAggregation(agg);
                
                agg = new Aggregation("stats.z_download_unagg_item_month", "stats.download_item_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                table.addAggregation(agg);
                
                agg = new Aggregation("stats.z_download_unagg_comm_month", "stats.download_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_coll_month", "stats.download_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_country_comm_month", "stats.download_country_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_country_coll_month", "stats.download_country_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("country_code", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_item_comm_month", "stats.download_item_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_item_coll_month", "stats.download_item_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("item_id", Types.INTEGER);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_month_1", "stats.download_metadata_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_month_2", "stats.download_metadata_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_comm_month_1", "stats.download_metadata_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_comm_month_2", "stats.download_metadata_comm_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("community_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_coll_month_1", "stats.download_metadata_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_download_unagg_metadata_coll_month_2", "stats.download_metadata_coll_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addValueColumn("relative_value", Types.DOUBLE);
                agg.addAggregationColumn("field_id", Types.INTEGER);
                agg.addAggregationColumn("field_value", Types.VARCHAR);
                agg.addAggregationColumn("collection_id", Types.INTEGER);
                table.addAggregation(agg);

                eventTables.add(table);
            }
            if (tables.get(i).equals("search"))
            {
                table = new EventTable("stats.search");
                
                agg = new Aggregation("stats.z_search_unagg_month", "stats.search_month");
                agg.addValueColumn("value", Types.BIGINT);                
                table.addAggregation(agg);

                agg = new Aggregation("stats.z_search_unagg_words_month", "stats.search_words_month");
                agg.addValueColumn("value", Types.BIGINT);
                agg.addAggregationColumn("word", Types.VARCHAR);
                table.addAggregation(agg);

                eventTables.add(table);
            }            
        }        
    }
        
    private static Options setCommandLineOptions()
    {
        
        // create an options object and populate it
        Options options = new Options();
             
        OptionBuilder.withLongOpt("tables");
        OptionBuilder.withValueSeparator(',');
        OptionBuilder.withDescription(
                       "Run the clean and/or aggregation for the \n specified table(s).\n" +
                       "Possible values are:\n all, view, download, search\n" + 
                       "Separate multiple with a comma (,)\n" +
                       "Default is all");
           
        Option tables = OptionBuilder.create('t');
        tables.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(tables);    
     
        options.addOption("a", "aggregate", false, "aggregate the unaggregated tables");
        options.addOption("c", "clean", false, "clean the current aggregation");
        options.addOption("v", "verbose", false, "print aggregation logging to STDOUT");
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
            new HelpFormatter().printHelp("StatsAggregator\n", options);
            System.exit(1);
        }          
        catch (ParseException e)
        {
            System.out.println("ERROR: " + e.getMessage());
            new HelpFormatter().printHelp("StatsAggregator\n", options);
            System.exit(1);
        }

        if (line.hasOption('h'))
        {
            new HelpFormatter().printHelp("StatsAggregator\n", options);
            System.exit(0);
        }

        if (!line.hasOption('a') && !line.hasOption('c'))
        {
            System.out.println("You have to specify if you want to \n" +
                               "clean the current aggregation or \n" +
                               "aggregate the unaggregated tables (or both).");
            new HelpFormatter().printHelp("StatsAggregator\n", options);
            System.exit(0);
        }

        if (line.hasOption('v'))
        {
            isVerbose = true;
        }

        if (line.hasOption('c'))
        {
            clean = true;
        }

        if (line.hasOption('a'))
        {
            aggregate = true;
        }

        String temp[] = null;
        if(line.hasOption('t'))
        {
            temp = line.getOptionValues('t');                    
        }
        if(temp==null || temp.length==0)
        {   
            tables.add("view");
            tables.add("download");
            tables.add("search");            
        }
        else
        {
            for (int i = 0; i < temp.length; i++)
            {
                if (temp[i].equals("all"))
                {
                    tables.add("view");
                    tables.add("download");
                    tables.add("search");            
                    break;
                }
                if (temp[i].equals("view"))
                    tables.add("view");
                else 
                    if (temp[i].equals("download"))
                        tables.add("download");
                    else
                        if (temp[i].equals("search"))
                            tables.add("search");
            }
        }
        tablesLine = "";
        for (int i = 0; i < tables.size(); i++)
            tablesLine = tablesLine + tables.get(i) + (i == tables.size() - 1 ? "" : ", ");
    }
}
