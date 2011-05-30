<?xml version="1.0" encoding="UTF-8"?>

<!--
    Classic.xsl
    
    Version: $Revision: 3705 $
    
    Date: $Date: 2009-04-11 19:02:24 +0200 (Sat, 11 Apr 2009) $
    
    Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
    Institute of Technology.  All rights reserved.
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
    
    - Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
    
    - Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
    
    - Neither the name of the Hewlett-Packard Company nor the name of the
    Massachusetts Institute of Technology nor the names of their
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
    HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
    BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
    OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
    TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
    USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
    DAMAGE.
-->
    

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:xalan="http://xml.apache.org/xalan" 
    exclude-result-prefixes="xalan encoder i18n dri mets xlink xsl dim xhtml mods dc">
    
    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>
    
    <!-- Added by CG - bit of a hack to get cc licence name and icon location from 'item_view_cc_image' method-->
    <xsl:variable name="cc_licence_name"><xsl:value-of select="//dri:field[@n='cc_licence_name']/dri:value"/></xsl:variable>
    <xsl:variable name="cc_licence_location"><xsl:value-of select="//dri:field[@n='cc_licence_icon_location']/dri:value"/></xsl:variable>

	<!-- Added by IF - Set a variable from the DRI to check whether user is authenticated -->
    <xsl:variable name="user_authenticated"><xsl:value-of select="/dri:document/dri:meta/dri:userMeta/@authenticated"/></xsl:variable>

    <!-- Robin - Find the login url -->
    <xsl:variable name="loginUrl"><xsl:value-of select="/dri:document/dri:meta/dri:userMeta/dri:metadata[@qualifier='loginURL']"/></xsl:variable>
    
    <!-- START GWaller 1/10/09 Show related item links -->
    <!--  Store the related elements from the DRI document so they can be used when parsing the METS item document. Need to do this to access the URL and title -->
   	<xsl:variable name="relatedXrefNodes" select="//dri:div[@n='related_div']//dri:xref"/> 
    <!-- END GWaller 1/10/09 Show related item links -->
      
    <!-- Override entry in DIM-Handler.xsl -->  
 	<!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin.-->
    <xsl:template name="itemDetailView-DIM">
        
        <!-- Output all of the metadata about the item from the metadata section-->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemDetailView-DIM"/>

        <!-- START CG - 06/10/09 Check if URL_BUNDLE exists and display if so -->
		<xsl:call-template name="url_bundle"/>
    	<!-- END CG - 06/10/09 -->
    
    	<!-- START GWaller 1/10/09 Show related item links -->
        <xsl:call-template name="addRelatedItemsAndHeader"/>
    	<!-- END GWaller 1/10/09 Show related itemlinks -->
    
		<!-- START CG - 06/10/09 If file in bitsteam, display details -->
		<xsl:call-template name="bitstream_display"/>
		<!-- END CG - 06/10/09 -->
  
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE' or @USE='ITEM-LICENSE']"/>
        
    </xsl:template>
    
<!--- Robins changes -->

    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemSummaryList-DIM"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>

    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM">
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <!-- Try sticking the tick in here -->
        <div class="artifact-authorised">
            <xsl:choose>
                <xsl:when test="dim:field[@element='accessRights']='viewAuthorised'">
                    <!--todo : remove the hardcoded context 'xmlui' -->
                    <img src="/xmlui/themes/Jorum_v2/images/green-tick-small.png" alt="You can access this resource" title="You can access this resource" />
                </xsl:when>
                <xsl:otherwise>
                    <!-- Stick in a space or the adjacent div floats left -->
                    <xsl:text>&#160;</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </div>
        <div class="artifact-description">
            <div class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$itemWithdrawn">
                                <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <span class="Z3988">
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="dim:field[@element='title']">
                                <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </xsl:element>
            </div>
            <div class="artifact-info">
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                <xsl:copy-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='creator']">
                            <xsl:for-each select="dim:field[@element='creator']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='contributor']">
                            <xsl:for-each select="dim:field[@element='contributor']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:text> </xsl:text>
                <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='publisher']">
	                <span class="publisher-date">
	                    <xsl:text>(</xsl:text>
	                    <xsl:if test="dim:field[@element='publisher']">
	                        <span class="publisher">
	                            <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
	                        </span>
	                        <xsl:text>, </xsl:text>
	                    </xsl:if>
	                    <span class="date">
	                        <xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
	                    </span>
	                    <xsl:text>)</xsl:text>
	                </span>
                </xsl:if>
            </div>
        </div>
    </xsl:template>

 <!-- End of Robins changes -->
    
    
    
    
    <!-- An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin.-->
    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>
        
        <!-- START CG - 06/10/09 Check if URL_BUNDLE exists and display if so -->
        <xsl:call-template name="url_bundle"/>
		<!-- END CG - 06/10/09 -->
        
        <!-- START GWaller 1/10/09 Show related item links -->
        <xsl:call-template name="addRelatedItemsAndHeader"/>
    	<!-- END GWaller 1/10/09 Show related itemlinks -->
        
        <!-- START CG - 06/10/09 If file in bitsteam, display details -->
        <xsl:call-template name="bitstream_display"/>   
		<!-- END CG - 06/10/09 -->
		
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE' or @USE='ITEM-LICENSE']"/>

    </xsl:template>
    
    
    
       
            <!-- CG Overridding entry in DIM-Handler and commenting out repetition of collections an 
        item belongs to in item view
        
        A collection rendered in the detailList pattern. Encountered on the item view page as 
        the "this item is part of these collections" list -->
		<xsl:template name="collectionDetailList-DIM">
			<xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
			<a href="{@OBJID}">
				<xsl:choose>
					<xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
						<xsl:value-of select="$data/dim:field[@element='title'][1]"/>
					</xsl:when>
					<xsl:otherwise>
						<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
					</xsl:otherwise>
				</xsl:choose>
			</a>
			<!--Display collection strengths (item counts) if they exist-->
			<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
				<xsl:text> [</xsl:text>
				<xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
				<xsl:text>]</xsl:text>
			</xsl:if>
			<br/>
		   <!-- <xsl:choose>
				<xsl:when test="$data/dim:field[@element='description' and @qualifier='abstract']">
					<xsl:copy-of select="$data/dim:field[@element='description' and @qualifier='abstract']/node()"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:copy-of select="$data/dim:field[@element='description'][1]/node()"/>
				</xsl:otherwise>
			</xsl:choose>-->
		</xsl:template>
    
    
    
    
    <!-- START CG - 06/10/09 Check if URL_BUNDLE exists and display if so -->
    <xsl:template name="url_bundle">
	<xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='URL_BUNDLE']/mets:file/mets:FLocat/@xlink:title">
        	<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-web-resource-title</i18n:text></h2>
        	
        	<xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='URL_BUNDLE']/mets:file/mets:FLocat">
        		<p>
        		<xsl:call-template name="url">
        			<xsl:with-param name="urlValue" select="@xlink:title"/>
        			<xsl:with-param name="urlName" select="@xlink:title"/>
        		</xsl:call-template>
        		<br/>
        		</p>
        	</xsl:for-each>
        </xsl:if>
    </xsl:template>  
    <!-- END CG - 06/10/09 -->
    
    <!-- START CG - 06/10/09 If file in bitsteam, display details -->
    <xsl:template name="bitstream_display">
	<xsl:if test="(./mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@GROUPID])">

		        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
		            <xsl:with-param name="context" select="."/>
		            <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
		        </xsl:apply-templates>   
        
        </xsl:if>  
    </xsl:template>  
    <!-- END CG - 06/10/09 -->
    
    
    
    <!-- Added by CG - handy snippet to create an html anchor for a url in a URL_BUNDLE-->
	<xsl:template name="url">
	<xsl:param name="urlValue"/>
	<xsl:param name="urlName"/>
	 		<a 
	 			href="{$urlValue}" 
	 			title="{$urlValue}">
     	    	<xsl:value-of select="$urlName" />          
     	    </a>
    </xsl:template>    
    
    
    
    <!--Override entry in DIM-Handler -->
     <!-- Generate the info about the item from the metadata section -->
        <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        
        <!-- IF: 30/10/2009 - Add in AddThis widget for social bookmarking -->
        <div class="addthis_toolbox addthis_default_style">
			<a href="http://www.addthis.com/bookmark.php?v=250&amp;pub=jorum" class="addthis_button_compact">Share</a>
			<span class="addthis_separator">|</span>
			<a class="addthis_button_delicious"><!-- delicious -->&#160;</a>
			<a class="addthis_button_twitter"><!-- twitter -->&#160;</a>
			<a class="addthis_button_email"><!-- email -->&#160;</a>
		</div>
		<!--<script type="text/javascript" src="http://s7.addthis.com/js/250/addthis_widget.js#pub=jorum"><![CDATA[/* Comment to force XSLT to render closing script element */]]></script>-->
		<!-- End AddThis widget -->
        <br />
        
        <table class="ds-includeSet-table">
            <!--
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@OBJID"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                            mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-preview</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>-->
            <tr class="ds-table-row even">
            
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>: </span></td>
                <td>
                    <span class="Z3988">
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                                <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                            	   <xsl:value-of select="./node()"/>
                            	   <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
	                                    <xsl:text>; </xsl:text><br/>
	                                </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                                <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </td>
            </tr>
            
      		<!--**** New addition by CG ****--> 
      		
     		<xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='URL_BUNDLE']/mets:file/mets:FLocat">
             <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-web-resource</i18n:text>: </span></td>
                <td>	
                    <span class="Z3988">
    			 		<xsl:call-template name="url">
        		<xsl:with-param name="urlValue" select="@xlink:title"/>
        		<xsl:with-param name="urlName" select="@xlink:title"/>
        	</xsl:call-template>
                    </span>
                </td>
            </tr> 
           </xsl:for-each>
           
           <!-- ************ -->
             
            <xsl:if test="dim:field[@element='contributor'][@qualifier='author'] or dim:field[@element='creator'] or dim:field[@element='contributor']">
	            <tr class="ds-table-row odd">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
	                <td>
	                    <xsl:choose>
	                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
	                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='creator']">
	                            <xsl:for-each select="dim:field[@element='creator']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='contributor']">
	                            <xsl:for-each select="dim:field[@element='contributor']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:otherwise>
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
	                </td>
	            </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='description' and @qualifier='abstract']">
	            <tr class="ds-table-row even">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
	                    	<hr class="metadata-seperator"/>
	                    </xsl:if>
	              	</xsl:for-each>
	              	<xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                </td>
	            </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='description' and not(@qualifier)]">
	            <tr class="ds-table-row odd">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
	                    	<hr class="metadata-seperator"/>
	                    </xsl:if>
	               	</xsl:for-each>
	               	<xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
	                	<hr class="metadata-seperator"/>
	                </xsl:if>
	                </td>
	            </tr>
            </xsl:if>
            <!-- IF: Added section to display keywords -->
            <xsl:if test="dim:field[@element='subject']">
	        <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-subject</i18n:text>: </span></td>
                <td>
                    <span>
                        <xsl:choose>
                            <xsl:when test="count(dim:field[@element='subject'][not(@qualifier)]) &gt; 1">
                                <xsl:for-each select="dim:field[@element='subject'][not(@qualifier)]">
                            	   <xsl:value-of select="./node()"/>
                            	   <xsl:if test="count(following-sibling::dim:field[@element='subject'][not(@qualifier)]) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="count(dim:field[@element='subject'][not(@qualifier)]) = 1">
                                <xsl:value-of select="dim:field[@element='subject'][not(@qualifier)][1]/node()"/>
                            </xsl:when>
                        </xsl:choose>
                    </span>
                </td>
            </tr>
            </xsl:if>
            <!-- End -->
            <xsl:if test="dim:field[@element='identifier' and @qualifier='uri']">
	            <tr class="ds-table-row even">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
	                <td>
	                	<xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
		                    <a>
		                        <xsl:attribute name="href">
		                            <xsl:copy-of select="./node()"/>
		                        </xsl:attribute>
		                        <xsl:copy-of select="./node()"/>
		                    </a>
		                    <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
		                    	<br/>
		                    </xsl:if>
	                    </xsl:for-each>
	                </td>
	            </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='date' and @qualifier='issued']">
	            <tr class="ds-table-row odd">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
	                <td>
		                <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
		                	<xsl:copy-of select="substring(./node(),1,10)"/>
		                	 <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
	                    	<br/>
	                    </xsl:if>
		                </xsl:for-each>
	                </td>
	            </tr>
            </xsl:if>
            
            <tr class="ds-table-row even">
	                <td align="center" colspan="2">
						<p>
                        <!-- START GWaller 13/9/10 IssueID #303 Support for multiple licence options -->
						<!-- BEGIN IF 04/10/09: IssueID #99 Add download link for items created by uploaded content package -->
						<xsl:choose>
                    			<xsl:when test="$viewAuthorised='true'">
                            <xsl:choose>
			                <xsl:when test="$user_authenticated = 'yes'">
				                <xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']">
				                 	<a>
			    						<xsl:variable name="webappcontext" select="substring-before(ancestor::mets:METS/@OBJEDIT, '/admin/')" />
										<xsl:attribute name="href">
			        					<xsl:variable name="itemId" select="substring-after(ancestor::mets:METS/@OBJEDIT, '/item')"/>
			        					<xsl:variable name="downloadStart" select="concat($webappcontext, '/admin/export')"/>
			        					<xsl:variable name="downloadHref" select="concat($downloadStart, $itemId)"/>
			        					<xsl:value-of select="$downloadHref"/>
			        					</xsl:attribute>
			        					<img src="{$webappcontext}/themes/Jorum_v2/images/package-download.png" />
			    					</a>
				                </xsl:if>
				                
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']">
									<a>
			    						<xsl:variable name="webappcontext" select="substring-before(ancestor::mets:METS/@OBJEDIT, '/admin/')" />
										<xsl:attribute name="href">
			        					<xsl:variable name="itemId" select="substring-after(ancestor::mets:METS/@OBJEDIT, '/item')"/>
			        					<xsl:variable name="downloadStart" select="concat($webappcontext, '/anon-export')"/>
			        					<xsl:variable name="downloadHref" select="concat($downloadStart, $itemId)"/>
			        					<xsl:value-of select="$downloadHref"/>
			        					</xsl:attribute>
			        					<img src="{$webappcontext}/themes/Jorum_v2/images/package-download.png" />
			    					</a>
		    					</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
						<!-- START GWaller 12/11/09 IssueID #73 Added preview link for content packages -->
						<xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='PREVIEW_CP']/mets:file">
						<xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='PREVIEW_CP']/mets:file/mets:FLocat[@xlink:title='PreviewIndexBitstream']">
	                		<a>
	    						<xsl:attribute name="href">
	        						<xsl:value-of select="@xlink:href"/>
	        					</xsl:attribute>
	        					<xsl:attribute name="id">
	        						<xsl:text>cp-preview</xsl:text>
	        					</xsl:attribute>
	        					<xsl:variable name="webappcontext" select="substring-before(@xlink:href, '/bitstream/')"/>
								<img src="{$webappcontext}/themes/Jorum_v2/images/package-preview.png" />
	    					</a>
						</xsl:for-each>
						</xsl:if>
						<!-- END GWaller 12/11/09 IssueID #73 Added preview link for content packages -->
						<!-- CG IssueID #401 comment out option to download original content package 
						<xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='ARCHIVED_CP']/mets:file">
				    		<xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='ARCHIVED_CP']/mets:file">
							<a>
	    						<xsl:variable name="webappcontext" select="substring-before(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '/bitstream/')"/>
								<xsl:attribute name="href">
	        					<xsl:variable name="bitstreamPath" select="substring-after(mets:FLocat[@LOCTYPE='URL']/@xlink:href, '/bitstream/')"/>
	        					<xsl:variable name="downloadStart" select="concat($webappcontext, '/download/bitstream/')"/>
	        					<xsl:variable name="downloadHref" select="concat($downloadStart, $bitstreamPath)"/>
	        					<xsl:value-of select="$downloadHref"/>
	        					</xsl:attribute>
	        					<xsl:attribute name="id">
	        						<xsl:text>cp-link</xsl:text>
	        					</xsl:attribute>
	        					<img src="{$webappcontext}/themes/Jorum_v2/images/download-content-package.png" />
	    					</a>
							</xsl:for-each>
						</xsl:if>-->
						<!-- END IF 04/10/09: IssueID #99 Add download link for items created by uploaded content package -->

                        </xsl:when>

                    	</xsl:choose>
						</p>
				</td>
			</tr>
			
			 <tr class="ds-table-row odd">
	                <td><span class="bold">Licence Note:</span></td>
	                <td>
                        <span class="bold">
		                <i18n:text>xmlui.Submission.submit.CCLicenseStep.note</i18n:text>
                        </span>
	                </td>
	            </tr>

            <!-- GWaller 22/12/10 IssueID #578 Display a warning if the user isn't authorised to view the item bitstreams -->
            <xsl:if test="$viewAuthorised!='true'">
            <tr>
                <td colspan="2">
                    <div class='ds-static-div cc_warning'>
              				<p><i18n:text>xmlui.licencecontroller.viewNotAuthorised.msg</i18n:text></p>
              			</div>
                </td>
            </tr>
            </xsl:if>
			
        </table>
    </xsl:template>
  
    <!-- 
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor. 
    --> 
    <xsl:template match="dri:trail">
       	<xsl:attribute name="class">
               <xsl:text>ds-trail-link </xsl:text>
               <xsl:if test="position()=1">
                   <xsl:text>first-link </xsl:text>
               </xsl:if>
               <xsl:if test="position()=last()">
                   <xsl:text>last-link</xsl:text>
               </xsl:if>
           </xsl:attribute>
           <!-- Determine whether we are dealing with a link or plain text trail link -->
           <xsl:choose>
               <xsl:when test="./@target">
                   <a>
                       <xsl:attribute name="href">
                           <xsl:value-of select="./@target"/>
                       </xsl:attribute>
                       <xsl:apply-templates />
                   </a>
               </xsl:when>
               <xsl:otherwise>
                   <xsl:apply-templates />
               </xsl:otherwise>
           </xsl:choose>
           <!--**** New addition by IF ****-->
   		<!-- put in a little arrow '&raquo;' if this is not the last item in the trail -->
   		<xsl:if test="not(position()=last())">
   			&#187;
   		</xsl:if>
   		<!--**** End addition by IF ****-->
    </xsl:template>
    
    <!-- The header (distinct from the HTML head element) contains the title, subtitle, login box and various 
        placeholders for header images -->
    <xsl:template name="buildHeader">
    	<div id="header">
			<h1 class="grid_6">
				<a href="http://www.jorum.ac.uk"><img src="/xmlui/themes/Jorum_v2/images/jorumMainLogo.jpg" alt="Jorum: Learning to Share" id="getstarted" /></a><img src="/xmlui/themes/Jorum_v2/images/jorumLearntoShare.jpg" alt="Jorum: Learning to Share" />
			</h1>
			<div id="main-search">
				<form id="main-search-form" method="post" action="http://resources.jorum.ac.uk/xmlui/advanced-search" accept-charset="utf-8">
					<div style="display:none;">
						<input type="hidden" name="_method" value="POST" />
						<input type="hidden" name="field2" value="licence" />
					</div>
					<div id="search-radios" class="input radio" style="margin: 0; padding: 0;">
						<div style="float: left">
							<input type="radio" name="query2" id="all-resources" value="" checked="checked" />
							<label for="all-resources" style="display: block; color: #fff; font-size: 12px; font-weight: normal; margin-bottom: 0; margin-right: 10px;">All Resources</label>
						</div>
						<div style="float: left">
							<input type="radio" name="query2" id="open-resources" value="creativecommons.org" style="float: left" />
							<label for="open-resources" style="display: block; color: #fff; font-size: 12px; font-weight: normal; margin-bottom: 0;">Open Resources</label>
						</div>
					</div>
					<div class="input text">
						<label for="SearchQuery">Query</label>
						<input class="input" name="query1" type="text" value="Search learning &amp; teaching resources" id="SearchQuery" />
					</div>
					<div class="submit">
						<input type="submit" value="Search" />
						<a href="http://resources.jorum.ac.uk/xmlui/advanced-search" id="advanced-search">Advanced Search</a>
					</div>
				</form>
			</div>
		</div>
		<br class="clear" />
		<ul class="grid_12" id="mainmenu">
			<li style="padding-left:10px;" ><a href="http://www.jorum.ac.uk/">Home</a></li>
			<li><a href="http://resources.jorum.ac.uk/xmlui" class="_active">Find</a></li>
			<li><a href="http://www.jorum.ac.uk/share">Share</a></li>
			<li><a href="http://community.jorum.ac.uk/">Discuss</a></li>
			<li><a href="http://jorumnews.blogspot.com/">News</a></li>
			<li><a href="http://www.jorum.ac.uk/help">Help</a></li>
		</ul>
		<div class="clear">&#160;</div>
		<!-- InstanceBeginEditable name="mainBody" -->
		<p id="breadcrumb" class="grid_12">
			You are here: 
        	<xsl:choose>
            	<xsl:when test="count(/dri:document/dri:meta/dri:pageMeta/dri:trail) = 0">
                	Resources Home
                </xsl:when>
                <xsl:otherwise>
                	<xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail"/>
                </xsl:otherwise>
            </xsl:choose>
        </p>
    </xsl:template>
    
    
    <!--**** New addition by IF ****-->
    <!-- Like the header, the footer contains various miscellanious text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <p>&#160;</p>
		<!-- footer links -->
		<br class="clear" />
		<div class="grid_12" id="footer1">
			<ul id="footmenu" class="grid_6 alpha">
				<li><a href="http://www.jorum.ac.uk/about-us">About us</a></li>
				<li><a href="http://www.jorum.ac.uk/terms-of-service">Terms of Service</a></li>
				<li><a href="http://www.jorum.ac.uk/Policies">Policies</a></li>
				<li><a href="http://www.jorum.ac.uk/sitemap">Site Map</a></li>
			</ul>
			<div id="mailbox" >
				<a href="http://www.jorum.ac.uk/feedback"><img src="/xmlui/themes/Jorum_v2/images/mailicon.gif" style="float:right;" alt="Email us your feedback" />Feedback</a>
			</div>
		</div><!--/footer1-->
		<br class="clear" style="margin-bottom:10px;" />
		<div class="grid_6" id="jisc">
			<p><img src="/xmlui/themes/Jorum_v2/images/jisc.gif" alt="JISC logo" /></p>
		</div><!--/jisc-->
		<!-- <div class="grid_5" id="cc_licence">
					<p>
					<img src="/xmlui/themes/Jorum_v2/images/cc.gif" alt="Creative Commons Attribution-Non-Commercial-No Derivative Works 2.0 UK: England &amp; Wales Licence logo" />
					<br />
					Except for third party materials and otherwise stated, content on this site is made available under an <a href="#">Attribution-Non-Commercial-No Derivative Works 2.0 UK: England &amp; Wales Licence</a>
					</p>
				</div> --><!--/cc_licence-->
		<div class="grid_6" id="edina-mimas">
			<p>
			<img src="/xmlui/themes/Jorum_v2/images/edina_mimas.gif" alt="Joint Edina and Mimas data centres logo" />
			</p>
		</div><!--/edina-mimas-->
    </xsl:template>
    <!--**** End addition by IF ****-->

	<!-- 
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying 
        the templates inside it. 
        
        In fact, the only bit of real work this template does is add the search box, which has to be 
        handled specially in that it is not actually included in the options div, and is instead built 
        from metadata available under pageMeta.
    -->
    <!-- TODO: figure out why i18n tags break the go button -->
    <xsl:template match="dri:options">
        <div id="side_menu" class="grid_3 pull_9">
                        
            
            <!-- Once the search box is built, the other parts of the options are added -->
            <xsl:apply-templates />
        </div>
    </xsl:template>

	<!-- 
        The template to handle the dri:body element. It simply creates the ds-body div and applies 
        templates of the body's child elements (which consists entirely of dri:div tags).
    -->    
    <xsl:template match="dri:body">
        <div id="second_level_content" class="grid_9 push_3">
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>
            <xsl:apply-templates />
        </div>
    </xsl:template>

	
         
	<!--Added by CG - Overrides code in General-Handler - adds PrettyPhoto functionality  -->
    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE' or @USE='ITEM-LICENSE']">
        <div class="license-info">
            
                <xsl:if test="@USE='CC-LICENSE' or @USE='ITEM-LICENSE'">
                <h2><a name="ccLicence"><i18n:text>xmlui.dri2xhtml.structural.link_cc_heading</i18n:text></a></h2>         
                    <p><a href="{mets:file/mets:FLocat[@xlink:title='license_url']/@xlink:href}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Licence"><xsl:value-of select="$cc_licence_name"/></a></p>

              		<xsl:call-template name="item_view_cc_image"/>
              		<!-- Display note for the 2 faulty CC licences -->
              		<xsl:if test="$cc_licence_name='Attribution-No Derivative Works 2.0 UK: England &amp; Wales'">  
              			<div class='ds-static-div cc_warning'>
              				<p><i18n:text>xmlui.Submission.submit.CCLicenseStep.faultyLicenceMessageUserND</i18n:text></p>
              			</div>
              		</xsl:if>
              		<xsl:if test="$cc_licence_name='Attribution-Noncommercial-No Derivative Works 2.0 UK: England &amp; Wales'">
              			<div class='ds-static-div cc_warning'>
              				<p><i18n:text>xmlui.Submission.submit.CCLicenseStep.faultyLicenceMessageUserNDNC</i18n:text></p>
              			</div>
              		</xsl:if>
                </xsl:if>
                <xsl:if test="@USE='LICENSE'">
                <h2><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></h2>
                	  <p><a href="http://www.jorum.ac.uk/termsofservice.html?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Terms of Service"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a></p>
                	<!-- <a href="{mets:file/mets:FLocat[@xlink:title='license.txt']/@xlink:href}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Terms of Service"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a>-->
                </xsl:if>
            
        </div>
    </xsl:template>




       <!--**** End addition by CG ****-->  
         
    <!--Added by CG - Replaces Form submission for external cc licence chooser  -->
    <xsl:template match="dri:p[@n='cc_submission']" >
    <p>
    <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
    </xsl:call-template>
        <i18n:text>xmlui.Submission.submit.jorum.JorumCCLicenseStep.external_cc_1</i18n:text> 
		<a>
            <xsl:attribute name="href">
            <xsl:text>http://creativecommons.org/choose/</xsl:text>
            
            <xsl:text>?jurisdiction=</xsl:text>
            <xsl:value-of select="encoder:encode(string(//dri:field[@n='jurisdiction']/dri:value[@type='raw']))"/>  
            
            <xsl:text>&amp;partner=</xsl:text>
            <xsl:value-of select="encoder:encode(string(//dri:field[@n='partner']/dri:value[@type='raw']))"/>
            
            <xsl:text>&amp;exit_url=</xsl:text>           
            <xsl:value-of select="encoder:encode(string(//dri:field[@n='exit_url']/dri:value[@type='raw']))"/>  
            
            <xsl:text>&amp;stylesheet=</xsl:text>
            <xsl:value-of select="encoder:encode(string(//dri:field[@n='stylesheet']/dri:value[@type='raw']))"/>
            
            </xsl:attribute>
          	
			
			<xsl:attribute name="title">
				<xsl:text>Choose Creative Commons Licence</xsl:text>
			</xsl:attribute>
			<i18n:text>xmlui.Submission.submit.jorum.JorumCCLicenseStep.external_cc_2</i18n:text>
        </a>
	</p>
    </xsl:template>  
    <!--**** End addition by CG ****-->  
       
       
    <xsl:template name="item_view_cc_image" >
        	<div class="ds-cc">       	
        		<a href="{mets:file/mets:FLocat[@xlink:title='license_url']/@xlink:href}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Licence">
        			<img src="{$cc_licence_location}" alt="{$cc_licence_name}"/>
        		</a>
        	</div>
    </xsl:template>

    <!--Added by CG - Adds appropriate CC icon in cc submission step  -->   
 	<xsl:template name="cc_image" >
        	<div class="ds-cc">
        		<a href="{//dri:item[@n='licence_details']/dri:xref/@target}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Licence">
        			<img src="{//dri:field[@n='cc_license_icon']/dri:value}" alt="{//dri:item[@n='licence_details']/dri:xref[@target]}"/>
        		</a>
        	</div>
    </xsl:template>
    
    <xsl:template match="//dri:item[@n='licence_details']/dri:xref">
    	<a>
        	<xsl:attribute name="href"><xsl:value-of select="@target"/>
        		<xsl:text>?iframe=true&amp;width=75%&amp;height=75%</xsl:text>
        	</xsl:attribute>
        	<xsl:attribute name="title">View Licence</xsl:attribute>
            <xsl:attribute name="rel">prettyPhoto</xsl:attribute>
            <xsl:apply-templates />         
        </a>
            <xsl:call-template name="cc_image"/>
   </xsl:template>
	 
	<!-- Overrides entry in structrual.xsl - makes text area for entering overview larger -->
   <xsl:template name="textAreaCols">
      <xsl:attribute name="cols">50</xsl:attribute>
   </xsl:template>
   
   <xsl:template name="textAreaRows">
      <xsl:attribute name="rows">15</xsl:attribute>
   </xsl:template>
    

    <!--**** End addition by CG ****-->   
    
    <!-- START GWaller 1/10/09 Show related item links -->
    
    <!-- This overrides the match for dri:xref in the structural.xsl file -->
    <xsl:template name="xrefMatcher" match="dri:xref">
        
        <xsl:variable name="writeAnchor">
        	<xsl:choose>
        		<xsl:when test="@rend='norender'">false</xsl:when>
        		<xsl:otherwise>true</xsl:otherwise>
        	</xsl:choose>
        </xsl:variable>
        
   
            <xsl:if test="$writeAnchor = 'true'">
                <a>
            		<xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
            		<xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            		<xsl:apply-templates />
        		</a>
        	</xsl:if>
       
        
    </xsl:template>  
    
    
    <xsl:template name="addRelatedItemsAndHeader">
    	<!--  Add the header -->
    	<xsl:if test="count($relatedXrefNodes) &gt; 0">
        	<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-related-title</i18n:text></h2>	
        	
        	<xsl:for-each select="$relatedXrefNodes">
        	<p>
        	<xsl:call-template name="url">
        		<xsl:with-param name="urlValue" select="@target"/>
        		<xsl:with-param name="urlName" select="current()"/>
        	</xsl:call-template>
        	<br/>
        	</p>
        	</xsl:for-each>
        </xsl:if>
    	
    </xsl:template>
    
    <!-- Override entry in structural.xsl -->
        <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
                <label class="ds-form-label">
                	<xsl:choose>
                		<xsl:when test="./dri:field/@id">
                			<xsl:attribute name="for">
                				<xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                			</xsl:attribute>
                		</xsl:when>
                		<xsl:otherwise></xsl:otherwise>
                	</xsl:choose>
                	<!-- New addition by CG -->
                	<xsl:choose>
                		<xsl:when test="dri:field[@n='license_url']">
           
           					<a href="{dri:field/dri:helpLink/dri:link/@href}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto[iframes]" title="{dri:field/dri:option}">
        						<img src="{dri:field/dri:label}" alt="{dri:field/dri:option}"/>
        					</a>
        					
                		</xsl:when>	
                		<xsl:otherwise>
                			<xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                			<xsl:text>:</xsl:text>
                		</xsl:otherwise>
                	</xsl:choose>
                	<!-- End New addition -->
                </label>                
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <xsl:choose>
                	<xsl:when test="./dri:field/@id">
                		<label>
		                	<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
		                    <xsl:text>:</xsl:text>
		                </label>
                	</xsl:when>
                	<xsl:otherwise>
                		<span>
		                	<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
		                    <xsl:text>:</xsl:text>
		                </span>
                	</xsl:otherwise>
                </xsl:choose>
                
            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:choose>       
	                <xsl:when test="preceding-sibling::*[1][local-name()='label']">
		                <label class="ds-form-label">
		                	<xsl:choose>
		                		<xsl:when test="./dri:field/@id">
		                			<xsl:attribute name="for">
		                				<xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
		                			</xsl:attribute>
		                		</xsl:when>
		                		<xsl:otherwise></xsl:otherwise>
		                	</xsl:choose>
		                    <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
		                </label>
		            </xsl:when>
		            <xsl:otherwise>
		            	<xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>&#160;
		            </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to 
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template> 




	<!-- Overrides entry in structural.xsl CG-->
	<xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
        <xsl:variable name="returnValue"><xsl:value-of select="@returnValue"/></xsl:variable>
        <label>
            <input>
                <xsl:attribute name="name"><xsl:value-of select="../@n"/></xsl:attribute>
                <xsl:attribute name="type"><xsl:value-of select="../@type"/></xsl:attribute>
                <xsl:attribute name="value"><xsl:value-of select="@returnValue"/></xsl:attribute>
                <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                    <xsl:attribute name="checked">checked</xsl:attribute>
                </xsl:if>
                <xsl:if test="../@disabled='yes'">
                    <xsl:attribute name="disabled">disabled</xsl:attribute>
                </xsl:if>
            </input>
            
            <xsl:apply-templates />
            
            <!-- Add asterisk next to faulty licences in the submission chooser -->
            <xsl:variable name="cc_name"><xsl:value-of select="current()"/></xsl:variable>
            <xsl:if test="$cc_name='Attribution-No Derivative Works 2.0 UK: England &amp; Wales' or $cc_name='Attribution-Noncommercial-No Derivative Works 2.0 UK: England &amp; Wales' ">  
              	<a href="#ccNote">
        			<img src="/xmlui/themes/Jorum_v2/images/warning_16.png" alt="Note"/>
        		</a>
            </xsl:if>
        </label>
    </xsl:template>




    <!--**** End addition by CG ****-->
    
    <!-- BEGIN IF 03/11/2009: Altered H1 heading to display feed icon and link where feed available -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <!-- with the help of the font-sizing variable, the font-size of our header text is made continuously variable based on the character count -->
        <xsl:variable name="font-sizing" select="365 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h{$head_count}">
            <!-- in case the chosen size is less than 120%, don't let it go below. Shrinking stops at 120% -->
            <!--< xsl:choose>
                            <xsl:when test="$font-sizing &lt; 120">
                                <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:attribute name="style">font-size: <xsl:value-of select="$font-sizing"/>%;</xsl:attribute>
                            </xsl:otherwise>
                        </xsl:choose> -->
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>            
            <xsl:apply-templates />
            <xsl:if test="$head_count=1">
        		<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
        			<xsl:if test="not(../@id='file.news.div.news')">
        				<xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
                			<xsl:if test="not(position()=1)">
                			<a>
                    			<xsl:attribute name="href">
                        			<xsl:value-of select="."/>
                    			</xsl:attribute>
                    			<xsl:attribute name="title">
                        			<xsl:text>Subscribe to RSS feed</xsl:text>
                    			</xsl:attribute>
                    			<img src="/xmlui/themes/Jorum_v2/images/feed-icon-20x20.png" alt="RSS feed icon" />
                			</a>
                			</xsl:if>
            			</xsl:for-each>
        			</xsl:if>
        		</xsl:if>
        	</xsl:if>
        </xsl:element>
        
    </xsl:template>
    <!-- END IF 03/11/2009-->    
    
<!-- Added By CG 11/11/09 Overrides entry in structural.xsl -->
	<xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">

		<xsl:apply-templates select="dri:head" />
		<!--
			Here we decide whether we have a hierarchical list or a flat one
		-->
		<xsl:choose>
		
			<!-- This one added by CG for treeview of communities -->
			<xsl:when test="descendant-or-self::dri:referenceSet/@n='community-browser' or ancestor::dri:referenceSet/@n='community-browser'">	
				<ul id="tree">
					<xsl:apply-templates select="*[not(name()='head')]"
						mode="summaryList" />
				</ul>
			</xsl:when>
			<!-- End CG Addition -->
			<xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
					<ul>
					<xsl:apply-templates select="*[not(name()='head')]"
						mode="summaryList" />
					</ul>
			</xsl:when>
			<xsl:otherwise>
                <!-- Robin - For selected lists add help box for the tick icons -->
                <xsl:choose>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='browse-by-dateissued-withTicks' or ancestor::dri:referenceSet/@n='browse-by-dateissued-withTicks'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='browse-by-author-withTicks' or ancestor::dri:referenceSet/@n='browse-by-author-withTicks'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='browse-by-title-withTicks' or ancestor::dri:referenceSet/@n='browse-by-title-withTicks'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='browse-by-subject-withTicks' or ancestor::dri:referenceSet/@n='browse-by-subject-withTicks'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='search-results-repository-withTicks' or ancestor::dri:referenceSet/@n='search-results-repository-withTicks'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                    <xsl:when test="descendant-or-self::dri:referenceSet/@n='collection-last-submitted' or ancestor::dri:referenceSet/@n='collection-last-submitted'">
                       <xsl:call-template name="itemSummary-tickBoxHelp" />
                    </xsl:when>
                </xsl:choose>
				<ul class="ds-artifact-list">
					<xsl:apply-templates select="*[not(name()='head')]"
						mode="summaryList" />
				</ul>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
    <!-- END CG 11/11/2009--> 

    <xsl:template name="itemSummary-tickBoxHelp">
        <div class="tickBoxHelp">
            <div class="tickBoxImage">
                <img src="/xmlui/themes/Jorum_v2/images/green-tick-small.png" alt="You can access this resource" title="You can access this resource" />
            </div>
            <div class="tickBoxText">
                <xsl:choose>
                    <xsl:when test="$user_authenticated = 'yes'">
                        <i18n:text>xmlui.dri2xhtml.logged-in.text-1</i18n:text><br />
                        <div class="font90"><i18n:text>xmlui.dri2xhtml.logged-in.text-2</i18n:text><br /></div>
                        <div class="font90"><i18n:text>xmlui.dri2xhtml.logged-in.text-3</i18n:text></div>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.logged-out.text-1</i18n:text><br />
                        <i18n:text>xmlui.dri2xhtml.logged-out.text-2</i18n:text>
                        <a>
        	                <xsl:attribute name="href"><xsl:value-of select="$loginUrl"/></xsl:attribute>
        		            <i18n:text>xmlui.dri2xhtml.logged-out.text-3</i18n:text>
        	            </a>
                        .
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>
    </xsl:template>

	<!-- The last thing in the structural elements section are the templates to cover the attribute calls. 
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs, 
        which creates a new div element to display pagination information. -->
    
    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination {$position}">
                    <xsl:if test="parent::node()/@previousPage">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@previousPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
                    <p class="pagination-info">
                        <i18n:translate>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                        <!--
                        <xsl:text>Now showing items </xsl:text>
                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                        <xsl:text>-</xsl:text>
                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                        <xsl:text> of </xsl:text>
                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                            -->
                    </p>
                    <xsl:if test="parent::node()/@nextPage">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@nextPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="pagination-masked {$position}">
                    <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
                    <p class="pagination-info">
                        <i18n:translate>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                    </p>
                    <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>            
        </xsl:choose>
    </xsl:template>
    
  
    <!--  START GWaller 1/12/10  IssueID #539 Addition of a view licence link on the licence selection screen -->
    <xsl:template match="dri:helpLink" mode="help">
        <span class="field-help">
            <xsl:apply-templates select="*" mode="help"/>
        </span>
    </xsl:template>
    
    <xsl:template match="dri:helpLink/dri:helpText" mode="help">    
		<xsl:value-of select="."/>   
    </xsl:template>
    
    <xsl:template match="dri:helpLink/dri:helpText">    
		<!--  Don't output anything - not in help mode -->
    </xsl:template>
    
    <xsl:template match="dri:helpLink/dri:link" mode="help">
          
            <a href="{./@href}?iframe=true&amp;width=75%&amp;height=75%" title="Licence" rel="prettyPhoto">
       
       		<!--  Do we have any child nodes? -->
       		<xsl:choose>
       		<xsl:when test="*">
       			<!--  Might have an i18n element - need to call apply templates to copy it -->
            	<xsl:apply-templates select="*"/>
       		</xsl:when>
       		<xsl:otherwise>
       			<!--  Might just have raw text - use this -->
       			<xsl:value-of select="."/> 
       		</xsl:otherwise>
       		</xsl:choose>
                 
            </a>
    </xsl:template>
    
    <xsl:template match="dri:helpLink/dri:link">
            <!--  Don't output anything - not in help mode -->
    </xsl:template>
    
    <!-- END GWaller 1/12/10  IssueID #539 Addition of a view licence link on the licence selection screen -->
    
         
</xsl:stylesheet>
