package org.dspace.app.stats;

import java.sql.SQLException;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class EventTable
{
    private String table;
    private ArrayList<Aggregation> aggregations = new ArrayList<Aggregation>();
    
    public EventTable(String table)
    {
        this.table = table;
    }

    public String getTable()
    {
        return table;
    }
    
    public void addAggregation(Aggregation agg)
    {
        aggregations.add(agg);
    }
    
    public ArrayList<Aggregation> getAggregations()
    {
        return aggregations;
    }
    
    public void flagAsAggregated(Context context) throws SQLException
    {
        String sql = "select today_date from stats.z_today_date";
        TableRow row = DatabaseManager.querySingle(context, sql);
        Date currentDate = row.getDateColumn("today_date");

        sql = "update " + table + " " +
                     "set aggregated=true " + 
                     "where aggregated=false and date < ?";

        DatabaseManager.updateQuery(context, sql, new java.sql.Date(currentDate.getTime()));
    }

    public void flagAsUnaggregated(Context context) throws SQLException
    {
        String sql = "update " + table + " " +
                     "set aggregated = false " + 
                     "where aggregated = true";

        DatabaseManager.updateQuery(context, sql);
    }
    
    public void flagSpiders(Context context) throws SQLException
    {
        String sql = "select closed from stats.control where control_id=1";
        TableRow row = DatabaseManager.querySingle(context, sql);
        
        if (row != null)
        {
            Date closed = row.getDateColumn("closed");
            
            // mark spider events if the IP is in spiders
            // dont change events before closed date
            sql = "update " + table + " set spider=true " +
                  "where date > ? and spider=false " + 
                  "and ip in (select ip from stats.ip_spider)";
            
            DatabaseManager.updateQuery(context, sql, new java.sql.Date(closed.getTime()));

            // unmark spider events if the IP isnt in spiders
            // dont change events before closed date
            sql = "update " + table + " set spider=false " +
                  "where date > ? and spider=true " + 
                  "and ip not in (select ip from stats.ip_spider)";
            
            DatabaseManager.updateQuery(context, sql, new java.sql.Date(closed.getTime()));
        }
    }
}
