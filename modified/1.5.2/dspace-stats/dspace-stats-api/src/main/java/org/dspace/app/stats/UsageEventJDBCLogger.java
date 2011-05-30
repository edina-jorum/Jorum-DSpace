/*
 * UsageEventJDBCLogger.java
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

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;

import org.apache.log4j.Logger;
import org.dspace.app.statistics.AbstractUsageEvent;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;


/**
 * Serialize AbstractUsageEvent data to a database table. 
 * 
 * @author Angelo Miranda 
 * @version $Revision: 0 $
 */
public class UsageEventJDBCLogger extends AbstractUsageEvent
{
    /** log4j category */
    private static Logger log = Logger.getLogger(UsageEventJDBCLogger.class);
    
	/** dspace context */
	private static Context context = null;

    public UsageEventJDBCLogger()
    {
        super();
        
        try
        {
           context = new Context();
        }
        catch (SQLException e)
        {
           System.out.println("Database error: " + e.getMessage());
        }
    }

    /**
     * Serialize to database
     */
    public void fire()
    {
        //String country;
        String countryCode = "--";
        //String countryName = "N/A";
        
        try
        {
           /*
            Commented out by CG - see IssueID #165
            country = Country.getCountry(context, sourceAddress);
            String[] temp = country.split(";");

	        if (temp.length == 2)
	        {
		        countryCode = temp[0];
		        countryName = temp[1];
		        checkCountry(countryCode, countryName);
	        }*/
    
	        switch (eventType)
	        {
	            case AbstractUsageEvent.VIEW:
	                switch (objectType)
	                {
	                    case Constants.ITEM:
	                        insertView(countryCode);
	                        break;                  
	                    case Constants.BITSTREAM:
	                        insertDownload(countryCode);
	                        break;
	                    // GH - use BUNDLE const for package download                      
	                    case Constants.BUNDLE:
	                        insertPackageDownload(countryCode);
	                        break;
	                    // GH -end
	                }
	                break;
                case AbstractUsageEvent.SEARCH:
                    insertSearch(countryCode);
                    break;                  	            
                case AbstractUsageEvent.LOGIN:
                    insertLogin(countryCode);
                    break;                                  
                case AbstractUsageEvent.ADVANCE_WORKFLOW:
                    insertAdvanceWorkflow(countryCode);
                    break;                                  
	        }
        	
        	
        	
        	context.commit();
        }
        catch (SQLException e)
        {
            log.error("ERROR: Cant execute sql: " + e.getMessage());
        }
    }
    
    private void insertView(String countryCode) throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        sql.append("insert into stats.view ");
        sql.append("(view_id, date, time, item_id, session_id, user_id, ip, country_code) ");
        sql.append("values ");
        sql.append("(getnextid('stats.view'), ?, ?, ?, ?, ?, ?, ?);");

        params = new Object[] { new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                new Integer(objectID),
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress,
                                countryCode
                              };
        DatabaseManager.updateQuery(context, sql.toString(), params);                           
    }

    private void insertDownload(String countryCode) throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        sql.append("insert into stats.download ");
        sql.append("(download_id, date, time, bitstream_id, item_id, session_id, user_id, ip, country_code, relative_value) ");
        sql.append("values ");
        sql.append("(getnextid('stats.download'), ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        
        Integer itemId = getItemID(objectID);
        
        // GH - Issue# 70 - a null itemId could be a content package
        if(itemId == null)
        {
            checkForPackageDownload(countryCode);
            return;
        }
        // GH - end
        
        Long numberOfBitstreams = getNumberOfBitstreams(itemId);
        Double relativeValue = 1d / numberOfBitstreams; 
        
        params = new Object[] { new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                new Integer(objectID),
                                itemId,
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress,
                                countryCode,
                                relativeValue
                              };
        if (itemId != null)
            DatabaseManager.updateQuery(context, sql.toString(), params);        
    }
    
    // GH - Issue #70 
    
    // if bitstream download failed to find a item-id check
    // to see if the bistream is a content package
    private void checkForPackageDownload(String countryCode) throws SQLException
    {
        StringBuffer query = new StringBuffer();
        query.append("SELECT i.item_id ");
        query.append("FROM item i, item2bundle ib, bundle bu, bundle2bitstream bb, bitstream bi ");
        query.append("WHERE i.item_id     = ib.item_id ");
        query.append("AND ib.bundle_id    = bu.bundle_id ");
        query.append("AND bu.name         = 'ARCHIVED_CP' ");
        query.append("AND bu.bundle_id    = bb.bundle_id ");
        query.append("AND bb.bitstream_id = bi.bitstream_id ");
        query.append("AND bi.bitstream_id = ?");
        
        TableRow row = DatabaseManager.querySingle(
                context, query.toString(), objectID);
        
        if (row != null)
        {
            objectID = row.getIntColumn("item_id");
            insertPackageDownload(countryCode);
        }
    }
    
    // insert content package download stat using item id and bitstream id of 0
    private void insertPackageDownload(String countryCode) throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        sql.append("insert into stats.download ");
        sql.append("(download_id, date, time, bitstream_id, item_id, session_id, user_id, ip, country_code) ");
        sql.append("values ");
        sql.append("(getnextid('stats.download'), ?, ?, ?, ?, ?, ?, ?, ?);");
        
        params = new Object[] { new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                0, // bitstream id for a package download is 0
                                objectID,
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress,
                                countryCode
        };
        
        DatabaseManager.updateQuery(context, sql.toString(), params);
    }
    // GH - end
    
    private void insertSearch(String countryCode) throws SQLException
    {
        String sqlID = "select getnextid('stats.search') as id";
        TableRow row = DatabaseManager.querySingle(context, sqlID);
        Integer id = row.getIntColumn("id");
        
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        sql.append("insert into stats.search ");
        sql.append("(search_id, date, time, scope, scope_id, query, session_id, user_id, ip, country_code) ");
        sql.append("values ");
        sql.append("(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

        String scope = null;
        switch (objectType)
        {
            case Constants.COLLECTION:
                scope = "collection";
                break;
            case Constants.COMMUNITY:
                scope = "community";
                break;
            case Constants.SITE:
                scope = "site";
                break;                
        }
        String scopeID = (new Integer(objectID)).toString();
        
        String query = otherInfo;
        
        params = new Object[] { id,
                                new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                scope,
                                scopeID,
                                query,
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress,
                                countryCode
                              };
        DatabaseManager.updateQuery(context, sql.toString(), params);  
        
        // process words
        query = query.replace("author:", "");
        query = query.replace("title:", "");
        query = query.replace("keyword:", "");
        query = query.replace("abstract:", "");
        query = query.replace("series:", "");
        query = query.replace("sponsor:", "");
        query = query.replace("identifier:", "");
        query = query.replace("language:", "");
        query = query.replace(" AND ", "");
        query = query.replace(" OR ", "");
        query = query.replace(" NOT ", "");
        query = query.replace("(", " ");
        query = query.replace(")", " ");
        query = query.replace('"', ' ');
        query = query.replace(',', ' ');
        query = query.replace(';', ' ');
        query = query.replace('.', ' ');
        
        String[] querySplitted = query.split(" ");
        
        for (int i = 0; i < querySplitted.length; i++)
        {
            querySplitted[i].toLowerCase().trim();
            
            if (querySplitted[i].length() > 3)
            {   
                String sqlWords = "insert into stats.search_words (search_words_id, search_id, word) " +
                                  "values (getnextid('stats.download'), ?, ?)";
                params = new Object[] { id,
                                       querySplitted[i]
                                      };
                
                DatabaseManager.updateQuery(context, sqlWords, params);  
            }
        }
    }

    private void insertLogin(String countryCode) throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        sql.append("insert into stats.login ");
        sql.append("(login_id, date, time, session_id, user_id, ip, country_code) ");
        sql.append("values ");
        sql.append("(getnextid('stats.login'), ?, ?, ?, ?, ?, ?);");
        
        params = new Object[] { new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress,
                                countryCode
                              };
        DatabaseManager.updateQuery(context, sql.toString(), params);                           
    }

    private void insertAdvanceWorkflow(String countryCode) throws SQLException
    {
        StringBuffer sql = new StringBuffer();
        Object[] params = null;

        String[] extraInfo = (new String(otherInfo)).split(":");
        
        Integer itemID = null;
        Integer collectionID = null;
        Integer oldState = null;
        
        if (extraInfo.length == 3)
        {
            itemID = Integer.parseInt(extraInfo[0].substring(extraInfo[0].indexOf("=") + 1)); 
            collectionID = Integer.parseInt(extraInfo[1].substring(extraInfo[1].indexOf("=") + 1));
            oldState = Integer.parseInt(extraInfo[2].substring(extraInfo[2].indexOf("=") + 1));
        }
        
        sql.append("insert into stats.workflow ");
        sql.append("(workflow_id, date, time, workflow_item_id, item_id, collection_id, old_state, session_id, user_id, ip) ");
        sql.append("values ");
        sql.append("(getnextid('stats.workflow'), ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        
        params = new Object[] { new Date(new java.util.Date().getTime()),
                                new Time(new java.util.Date().getTime()),
                                new Integer(objectID),
                                itemID,
                                collectionID,
                                oldState,
                                sessionID,
                                (null == eperson ? "anonymous" : eperson.getEmail()),
                                sourceAddress
                              };
        DatabaseManager.updateQuery(context, sql.toString(), params);                           
    }

    private void checkCountry(String countryCode, String countryName) throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "stats.country", "code", countryCode);

        // If the country does not exist in the table, insert it
        if (row == null)
        {
        	String sql = "insert into stats.country (code, name) values (?, ?)";
        	DatabaseManager.updateQuery(context, sql, countryCode, countryName);
        }
    }
    
    private Integer getItemID(Integer bitstreamId) throws SQLException
    {
        TableRow row = DatabaseManager.findByUnique(context, "stats.v_item2bitstream", "bitstream_id", bitstreamId);
        
        if (row == null)
            return null;
        
        return row.getIntColumn("item_id");
    }
    
    private Long getNumberOfBitstreams(Integer itemId) throws SQLException
    {
        if (itemId == null)
            return 0l;
        
        TableRow row = DatabaseManager.
                        querySingle(context, 
                                    "select count(*) as count " +
                                    "from stats.v_item2bitstream " +
                                    "where item_id = ?",
                                    itemId);
        
        if (row == null)
            return 0l;
        
        return row.getLongColumn("count");
    }
    
}
