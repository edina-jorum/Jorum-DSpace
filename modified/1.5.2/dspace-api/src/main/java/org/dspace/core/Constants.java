/*
 * Constants.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 18:02:24 +0100 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
package org.dspace.core;

/**
 * Class with constants and matching strings, for DSpace types. These numbers
 * must never be changed!!
 * 
 * @author David Stuve
 * @version $Revision: 3705 $
 */
public class Constants
{
    /** Type of bitstream objects */
    public static final int BITSTREAM = 0;

    /** Type of bundle objects */
    public static final int BUNDLE = 1;

    /** Type of item objects */
    public static final int ITEM = 2;

    /** Type of collection objects */
    public static final int COLLECTION = 3;

    /** Type of community objects */
    public static final int COMMUNITY = 4;

    /** DSpace site type */
    public static final int SITE = 5;

    /** Type of eperson groups */
    public static final int GROUP = 6;

    /** Type of individual eperson objects */
    public static final int EPERSON = 7;

    /**
     * lets you look up type names from the type IDs
     */
    public static final String[] typeText = { "BITSTREAM", "BUNDLE", "ITEM",
            "COLLECTION", "COMMUNITY", "SITE", "GROUP", "EPERSON", };

    /**
     * Special Bundle and Bitstream Names:
     */

    /** Magic name of item license, as bitstream in LICENSE_BUNDLE_NAME  */
    public static final String LICENSE_BITSTREAM_NAME = "license.txt";

    /** Magic name of bundle containing item license */
    public static final String LICENSE_BUNDLE_NAME = "LICENSE";

    /**
     * Default bundle name for the "original" item content;
     * "derived" content such as thumbnails goes in other bundles.
     */
    public static final String DEFAULT_BUNDLE_NAME = "ORIGINAL";

    // GWaller 23/09/09 Added String for content bundle to store archived packages
    /**
     * Bundle name for archived content packages i.e. the zip which the user uploaded
     */
    public static final String ARCHIVED_CONTENT_PACKAGE_BUNDLE = "ARCHIVED_CP";
    
    /**
     * Bundle name for archived content packages i.e. the zip which the user uploaded
     */
    public static final String BACKUP_CONTENT_PACKAGE_BUNDLE = "BACKUP_CP";
    
    // GWaller 23/09/09 Added String for content bundle to store related packages
    /**
     * Bundle name for related content packages i.e. user uploadd multiple packages and the wrapper holds references to them via this bundle
     */
    public static final String RELATED_CONTENT_PACKAGE_BUNDLE = "RELATED_CP";
    
    // GWaller 25/09/09 Added constant for URL_BUNDLE i.e. web resources
    /**
     * Bundle name for URLs  i.e. web resources
     */
    public static final String URL_BUNDLE = "URL_BUNDLE";

    // GWaller 27/1/11 IssueID #303 Added constant for thumbnail bundle
    /**
     * Bundle name for URLs  i.e. web resources
     */
    public static final String THUMBNAIL_BUNDLE = "THUMBNAIL";
    
    // GWaller 17/11/09 Added support for feed urls
    public static final String FEED_BUNDLE = "FEED_BUNDLE";
    public static final String FEED_BUNDLE_URL_BITSTREAM_NAME = "feed_url";
    public static final String FEED_BUNDLE_DEF_COL_HANDLE_NAME = "def_col_handle";
    public static final String FEED_BUNDLE_FORCE_COL_HANDLE_NAME = "force_col_handle";
    public static final String FEED_BUNDLE_CONTENTS_NAME = "feed_contents";
    
    
    // GWaller 11/11/09 Added constants for pacakge preview bundle and bitstream
    public static final String PREVIEW_PACKAGE_BUNDLE = "PREVIEW_CP";
    public static final String PREVIEW_PACKAGE_BITSTREAM = "PreviewIndexBitstream";
    public static final String PREVIEW_LEFT_FRAME_BITSTREAM = "PreviewOrgBitstream";
    public static final String PREVIEW_RIGHT_FRAME_BITSTREAM = "PreviewDefPageBitstream";
    
    
    //Added By CG - constants for adding cc licence to metadata 
    public static final String DC_SCHEMA = "dc";
    public static final String DC_RIGHTS = "rights";
    public static final String DC_RIGHTS_URI = "uri";
    // Metadata format label used by IMS disseminator
	public static final String METADATA_FORMAT_LABEL = "metadataFormat";
    
	// Only supporting DC and QDC metadata in disseminated packages for the time being
	// (Had to move from dspace-jorum as it was causing cyclical maven build problems if 
	// calling from core dspace api)
	public static enum SupportedDisseminationMetadataFormats {
		DC, QDC;
		public static String getMetadataFormat(String mdFormat) {
			String format = "";
			try {
				format = SupportedDisseminationMetadataFormats.valueOf(mdFormat).toString();
			} catch (Exception e) {
				return DC.toString();
			}
			return format;
		}
	}
	
    /**
     * Name of bundle for user-visible "content" (same as default for now).
     */
    public static final String CONTENT_BUNDLE_NAME = "ORIGINAL";

    /** Bundle name for structured metadata bitstreams. */
    public static final String METADATA_BUNDLE_NAME = "METADATA";


    /** Action of reading, viewing or downloading something */
    public static final int READ = 0;

    /** Action of modifying something */
    public static final int WRITE = 1;

    /**
     * Action of deleting something. Different from removing something from a
     * container. (DELETE is now obsolete)
     * 
     * @see #REMOVE
     */
    public static final int DELETE = 2;

    /**
     * Action of adding something to a container. For example, to add an item to
     * a collection, a user must have <code>ADD</code> permission on the
     * collection.
     */
    public static final int ADD = 3;

    /**
     * Action of removing something from a container. Different from deletion.
     * 
     * @see #DELETE
     */
    public static final int REMOVE = 4;

    /** Action of performing workflow step 1 */
    public static final int WORKFLOW_STEP_1 = 5;

    /** Action of performing workflow step 2 */
    public static final int WORKFLOW_STEP_2 = 6;

    /** Action of performing workflow step 3 */
    public static final int WORKFLOW_STEP_3 = 7;

    /** Action of performing a workflow */
    public static final int WORKFLOW_ABORT = 8;

    /** Default Read policies for Bitstreams submitted to container */
    public static final int DEFAULT_BITSTREAM_READ = 9;

    /** Default Read policies for Items submitted to container */
    public static final int DEFAULT_ITEM_READ = 10;

    /**
     * collection admin -- metadata, logo, item metadata, submitters, withdraw
     * items, etc.
     */
    public static final int COLLECTION_ADMIN = 11;

    /** Position of front page news item -- top box */
    public static final int NEWS_TOP = 0;

    /** Position of front page news item -- sidebar */
    public static final int NEWS_SIDE = 1;

    /**
     * lets you look up action names from the action IDs
     */
    public static final String[] actionText = { "READ", "WRITE",
            "OBSOLETE (DELETE)", "ADD", "REMOVE", "WORKFLOW_STEP_1",
            "WORKFLOW_STEP_2", "WORKFLOW_STEP_3", "WORKFLOW_ABORT",
            "DEFAULT_BITSTREAM_READ", "DEFAULT_ITEM_READ", "COLLECTION_ADMIN" };

    /**
     * constants for the relevance array generating dynamicallis is simple: just
     * 1 < < TYPE
     */
    public static final int RBITSTREAM = 1 << BITSTREAM;

    public static final int RBUNDLE = 1 << BUNDLE;

    public static final int RITEM = 1 << ITEM;

    public static final int RCOLLECTION = 1 << COLLECTION;

    public static final int RCOMMUNITY = 1 << COMMUNITY;

    /**
     * Array of relevances of actions to objects - used by the UI to only
     * display actions that are relevant to an object type To see if an action
     * is relevant to an object, just OR the relevance type above with the value
     * in actionTypeRelevance[] (To see if READ is relevant to community, just
     * test actionTypeRelevance[READ] | RCOMMUNITY, 0 = irrelevant
     */
    public static final int[] actionTypeRelevance = {
            RBITSTREAM | RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 0 - READ
            RBITSTREAM | RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 1 -
                                                                     // WRITE
            0, // 2 - DELETE (obsolete)
            RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 3 - ADD
            RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 4 - REMOVE
            0, // 5 - WORKFLOW_STEP_1
            0, // 6 - WORKFLOW_STEP_2
            0, // 7 - WORKFLOW_STEP_3
            0, // 8 - WORKFLOW_ABORT
            RCOLLECTION, // 9 - DEFAULT_BITSTREAM_READ
            RCOLLECTION, // 10 - DEFAULT_ITEM_READ
            RCOLLECTION // 11 - COLLECTION_ADMIN
    };

    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * If you know the type string, look up the corresponding type ID constant.
     * 
     * @param type
     *            String with the name of the type (must be exact match)
     * 
     * @return the corresponding type ID, or <code>-1</code> if the type
     *         string is unknown
     */
    public static int getTypeID(String type)
    {
        for (int i = 0; i < typeText.length; i++)
        {
            if (typeText[i].equals(type))
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * If you know the action string, look up the corresponding type ID
     * constant.
     * 
     * @param action
     *            String with the name of the action (must be exact match)
     * 
     * @return the corresponding action ID, or <code>-1</code> if the action
     *         string is unknown
     */
    public static int getActionID(String action)
    {
        for (int i = 0; i < actionText.length; i++)
        {
            if (actionText[i].equals(action))
            {
                return i;
            }
        }

        return -1;
    }
}
