package org.dspace.app.stats;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class Aggregation
{
    private String sourceAggregationView;
    private String destAggregationTable; 
    private ArrayList<Column> valueColumns = new ArrayList<Column>();
    private ArrayList<Column> aggregationColumns = new ArrayList<Column>(); 
    
    public Aggregation(String sourceAggregationView, String destAggregationTable)
    {
        this.sourceAggregationView = sourceAggregationView;
        this.destAggregationTable = destAggregationTable;
    }

    public String getDestination()
    {
        return destAggregationTable;
    }

    public void addValueColumn(String name, Integer type)
    {
        valueColumns.add(new Column(name, type));
    }

    public void addAggregationColumn(String name, Integer type)
    {
        aggregationColumns.add(new Column(name, type));
    }
    
    public void clean(Context context) throws SQLException
    {
        DatabaseManager.updateQuery(context, "truncate table " + destAggregationTable);
    }
    
    public void aggregate(Context context) throws SQLException
    {
        String sql = "select * from " + sourceAggregationView;

        TableRowIterator iterator = DatabaseManager.query(context, sql);
        while (iterator.hasNext())
        {
            TableRow row = iterator.next();
            updateAggregation(context, row);        
        }   
    }

    private void updateAggregation(Context context, TableRow row) throws SQLException
    {
        // Check if exists the record on the aggregation table
        String checkSQL = buildCheckSQL();
        Object[] checkParams = buildCheckParams(row);
        TableRow rowCheck = DatabaseManager.querySingle(context, checkSQL, checkParams);
                
        if (rowCheck.getLongColumn("count") == 0)
        {
            // if dont exists insert
            String insertSQL = buildInsertSQL();
            Object[] insertParams = buildInsertParams(row);            
            DatabaseManager.updateQuery(context, insertSQL, insertParams);
        }
        else
        {
            // if exists update
            String updateSQL = buildUpdateSQL();
            Object[] updateParams = buildUpdateParams(row);            
            DatabaseManager.updateQuery(context, updateSQL, updateParams);
        }        
    }

    private String buildCheckSQL()
    {
        StringBuilder sql = new StringBuilder();
        
        sql.append("select count(*) as count from ");
        sql.append(destAggregationTable + " ");
        
        if (aggregationColumns.size() == 0)
        {
            sql.append("where yearmonth = ? ");
        }
        else
        {
            int i = 0;
            for (Column column : aggregationColumns)
            {
                sql.append(i == 0 ? "where " : "and ");
                sql.append(column.getName() + " = ? ");
                i++;
            }
            sql.append("and yearmonth = ? ");
        }
        
        return sql.toString();
    }
    
    private Object[] buildCheckParams(TableRow row)
    {        
        // month_trunc it is a date truncated to month
        // always exist on source aggregation views
        Date date = row.getDateColumn("month_trunc");
        
        // Converte the truncated date to an integer 
        Integer intYearMonth = getYearMonth(date);
        
        Object[] params = new Object[aggregationColumns.size() + 1];
        
        int i = 0;
        for (Column column : aggregationColumns)
        {
            if (column.getType() == Types.INTEGER)
            {
                params[i] = row.getIntColumn(column.getName());
            }
            else if (column.getType() == Types.VARCHAR)
            {
                params[i] = row.getStringColumn(column.getName());
            }
            i++;
        }
        params[i] = intYearMonth;
        
        return params;
    }
    
    private String buildInsertSQL()
    {
        StringBuilder sql = new StringBuilder();

        sql.append("insert into ");
        sql.append(destAggregationTable);
        sql.append(" (");
        
        for (Column column : aggregationColumns)
        {
            sql.append(column.getName());
            sql.append(", ");
        }
        
        sql.append("yearmonth, year");
                
        for (Column column : valueColumns)
        {
            sql.append(", ");
            sql.append(column.getName());
        }
        sql.append(") values (");

        Integer nColumns = aggregationColumns.size() +
                           valueColumns.size() +
                           2;
        
        for (int i = 0; i < nColumns; i++ )
            sql.append("?" + (i + 1 != nColumns ? ", " : ")"));
        
        return sql.toString();
    }
    
    private Object[] buildInsertParams(TableRow row)
    {
        Integer nColumns = aggregationColumns.size() +
                           valueColumns.size() +
                           2;
        
        Object[] params = new Object[nColumns];

        int i = 0;
        for (Column column : aggregationColumns)
        {
            if (column.getType() == Types.INTEGER)
            {
                params[i] = row.getIntColumn(column.getName());
            }
            else if (column.getType() == Types.VARCHAR)
            {
                params[i] = row.getStringColumn(column.getName());
            }
            i++;
        }

        Date date = row.getDateColumn("month_trunc");        
        Integer intYearMonth = getYearMonth(date);
        Integer intYear = getYear(date);

        params[i] = intYearMonth;
        i++;
        params[i] = intYear;
        i++;
        
        for (Column column : valueColumns)
        {
            if (column.getType() == Types.INTEGER)
            {
                params[i] = row.getIntColumn(column.getName());
            }
            else if (column.getType() == Types.BIGINT)
            {
                params[i] = row.getLongColumn(column.getName());
            }
            else if (column.getType() == Types.DOUBLE)
            {
                params[i] = row.getDoubleColumn(column.getName());
            }
            i++;                
        }
        
        return params;
    }

    private String buildUpdateSQL()
    {
        StringBuilder sql = new StringBuilder();
        
        sql.append("update ");
        sql.append(destAggregationTable);
        sql.append(" set ");
        
        int i = 0;
        for (Column column : valueColumns)
        {
            sql.append(column.getName());
            sql.append(" = ");
            sql.append(column.getName());
            sql.append(" + ?");
            i++;
            sql.append(i == valueColumns.size() ? " " : ", ");
        }
        
        if (aggregationColumns.size() == 0)
        {
            sql.append("where yearmonth = ? ");
        }
        else
        {
            i = 0;
            for (Column column : aggregationColumns)
            {
                sql.append(i == 0 ? "where " : "and ");
                sql.append(column.getName() + " = ? ");
                i++;
            }
            sql.append("and yearmonth = ? ");
        }
        
        return sql.toString();        
    }
    
    private Object[] buildUpdateParams(TableRow row)
    {

        Integer nParams =  aggregationColumns.size() +
                           valueColumns.size() +
                           1;

        Object[] params = new Object[nParams];
        
        int i = 0;
        
        for (Column column : valueColumns)
        {
            if (column.getType() == Types.INTEGER)
            {
                params[i] = row.getIntColumn(column.getName());
            }
            else if (column.getType() == Types.BIGINT)
            {
                params[i] = row.getLongColumn(column.getName());
            }
            else if (column.getType() == Types.DOUBLE)
            {
                params[i] = row.getDoubleColumn(column.getName());
            }
            i++;                            
        }
        
        Date date = row.getDateColumn("month_trunc");        
        Integer intYearMonth = getYearMonth(date);
        
        if (aggregationColumns.size() == 0)
        {
            params[i] = intYearMonth;
        }
        else
        {
            for (Column column : aggregationColumns)
            {
                if (column.getType() == Types.INTEGER)
                {
                    params[i] = row.getIntColumn(column.getName());
                }
                else if (column.getType() == Types.VARCHAR)
                {
                    params[i] = row.getStringColumn(column.getName());
                }
                i++;
            }
            params[i] = intYearMonth;
        }
        return params;
    }
    
    private Integer getYearMonth(Date date)
    {
        //Date processStart = Calendar.getInstance().getTime();
        
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        
        Integer intYear = c.get(Calendar.YEAR);
        Integer intMonth = c.get(Calendar.MONTH) + 1;
        
        String YearMonth = intYear.toString() + 
                           (intMonth < 10 ? "0" + intMonth.toString() : intMonth.toString());
                
        return Integer.parseInt(YearMonth);
    }

    private Integer getYear(Date date)
    {        
        Calendar c = new GregorianCalendar();
        c.setTime(date);
        
        return c.get(Calendar.YEAR);
    }

    private class Column
    {
        private String name;
        private Integer type;
        
        public Column(String name, Integer type)
        {
            this.name = name;
            this.type = type;
        }
        
        public String getName()
        {
            return name;
        }

        public Integer getType()
        {
            return type;
        }
    }
}
