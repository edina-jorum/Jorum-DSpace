/*
 * Country.java
 *
 * Copyright (c) 2007, University of Minho.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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

import com.maxmind.geoip.*;

import com.maxmind.geoip.*;
import java.io.*;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class to get country code and name from MaxMind GeoIP
 * 
 * @author Angelo Miranda
 */
public class Country {
    /**
     * Return an IP country code and name 
     *
     * @param   ip    the ip to get the country
     *
     * @return        the country code and name in the form "country code;country name"
     */
	public static String getCountry(Context context, String ip) throws SQLException 
	{
	    if (IsInstitution(context, ip))
	    {
	        String institutionName = ConfigurationManager.getProperty("stats.country.institutionname");
	        return "ZZ;" + institutionName;
	    }
	    
	    //Modified by CG to ensure connection to db is closed.  See IssueID#163
	    LookupService cl = null;
	      try {
			String dspaceDir = ConfigurationManager.getProperty("dspace.dir");
			String dbfile = dspaceDir + "/config/stats/GeoIP.dat";

			cl = new LookupService(dbfile, LookupService.GEOIP_STANDARD);
			String temp = cl.getCountry(ip).getCode() + ";" + cl.getCountry(ip).getName();
			cl.close();

			return temp;
		} catch (IOException e) {

			if (cl != null) {
				cl.close();
				cl = null;
			}
			e.printStackTrace();
			return "--;N/A";
		} catch (ArrayIndexOutOfBoundsException e) {
			if (cl != null) {
				cl.close();
				cl = null;
			}
			return "--;N/A";
		} finally {
			if (cl != null) {
				cl.close();
				cl = null;
			}
		}
   }
	
	public static boolean IsInstitution(Context context, String ip) throws SQLException
	{
        String sql = "select ip_range from stats.ip_institution";

        TableRowIterator iterator = DatabaseManager.query(context, sql);
        while (iterator.hasNext())
        {
            TableRow row = iterator.next();
            String range = row.getStringColumn("ip_range");
            if (ip.indexOf(range)==0)
                return true;
        }
        return false;
	}
}
