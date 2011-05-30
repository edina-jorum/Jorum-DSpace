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
						<xsl:choose>
                    			<xsl:when test="$viewAuthorised='true'">
								<!-- BEGIN IF 04/10/09: IssueID #99 Add download link for items created by uploaded content package -->
								<xsl:choose>
									<xsl:when test="$user_authenticated = 'yes'">
									 <a>
										<xsl:variable name="webappcontext" select="substring-before(ancestor::mets:METS/@OBJEDIT, '/admin/')" />
										<xsl:attribute name="href">
										<xsl:variable name="itemId" select="substring-after(ancestor::mets:METS/@OBJEDIT, '/item')"/>
										<xsl:variable name="downloadStart" select="concat($webappcontext, '/admin/export')"/>
										<xsl:variable name="downloadHref" select="concat($downloadStart, $itemId)"/>
										<xsl:value-of select="$downloadHref"/>
										</xsl:attribute>
										<!-- TODO: This png should be changed to Download Content Package, rather than Export Resource -->
										<img src="{$webappcontext}/themes/Jorum/images/export-resource.png" />
									
									</a>
									</xsl:when>
									<xsl:otherwise>
									 <a>
										<xsl:variable name="webappcontext" select="substring-before(ancestor::mets:METS/@OBJEDIT, '/admin/')" />
										<xsl:attribute name="href">
										<xsl:variable name="itemId" select="substring-after(ancestor::mets:METS/@OBJEDIT, '/item')"/>
										<xsl:variable name="downloadStart" select="concat($webappcontext, '/anon-export')"/>
										<xsl:variable name="downloadHref" select="concat($downloadStart, $itemId)"/>
										<xsl:value-of select="$downloadHref"/>
										</xsl:attribute>
										<!-- TODO: This png should be changed to Download Content Package, rather than Export Resource -->
										<img src="{$webappcontext}/themes/Jorum/images/export-resource.png" />
									
									</a>
									</xsl:otherwise>
								</xsl:choose>
								<!-- START GWaller 12/11/09 IssueID #73 Added preview link for content packages -->
								<xsl:if test="/mets:METS/mets:fileSec/mets:fileGrp[@USE='PREVIEW_CP']/mets:file">
								<xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='PREVIEW_CP']/mets:file/mets:FLocat[@xlink:title='PreviewIndexBitstream']">
									<a>
										<xsl:attribute name="href">
											<xsl:value-of select="@xlink:href"/>
										</xsl:attribute>
										<xsl:variable name="webappcontext" select="substring-before(@xlink:href, '/bitstream/')"/>
										<img src="{$webappcontext}/themes/Jorum/images/preview-content-package.png" />
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
										<img src="{$webappcontext}/themes/Jorum/images/download-content-package.png" />
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
		                <i18n:text>xmlui.Submission.submit.CCLicenseStep.note</i18n:text>
	                </td>
	            </tr>
			
        </table>
    </xsl:template>
  
    <!-- 
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor. 
    --> 
    <xsl:template match="dri:trail">
        <li> 
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
    		<!-- put in a little arrow '>' if this is not the last item in the trail -->
    		<xsl:if test="not(position()=last())">
    			<span style="color: white; font-size: 110%;">&#8594;</span>
    		</xsl:if>
    		<!--**** End addition by IF ****-->
        </li>
    </xsl:template>
    
    <!--**** New addition by IF ****-->
    <!-- Like the header, the footer contains various miscellanious text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <!-- footer links-->
		<div id="footer_links">
			<ul>
				<li><a href="http://www.jorum.ac.uk/termsofservice.html">Terms of Service</a></li>
				<li><a href="http://www.jorum.ac.uk/website_help.html">Website Help</a></li>
				<li><a href="http://www.jorum.ac.uk/support/feedback.php">Feedback</a></li>
				<li><a href="http://www.jorum.ac.uk/policies.html">Policies</a></li>
				<!--<li><a href="http://www.jorum.ac.uk/copyright.html">Copyright</a></li>-->
				<li class="sitemap"><a href="http://www.jorum.ac.uk/sitemap.html">Site Map</a></li>
			</ul>
		</div>

		<!-- footer -->
		<div id="ds-footer">
			<div class="jisclogo"><img src="/xmlui/themes/Jorum/images/jisc_logo.gif" alt="JISC Logo"/></div>
			<div class="licenceholder">
				<div class="licencelogo">
					<a href="http://creativecommons.org/licenses/by-nc-nd/2.0/uk/"><img src="/xmlui/themes/Jorum/images/by-nc-nd.png" alt="Creative Commons Attribution-NonCommercial-No Derivative 2.0 Licence" width="100" height="35" align="middle"/></a>
				</div>
				<div class="licencetext">
					<p class="acknowledgement">
						Except for third party materials and otherwise stated, content on this site is made available under a 
						<a href="http://creativecommons.org/licenses/by-nc-nd/2.0/uk/">Attribution-Non-Commercial-No Derivative Works 2.0 UK: England &amp; Wales Licence</a>
					</p>
				</div>
			</div>
			<div class="jointlogo"><img src="/xmlui/themes/Jorum/images/Mimas_edina.png" alt="EDINA and MIMAS: JISC-designated national data centres" width="260" height="69"/></div>
			<!--Invisible link to HTML sitemap (for search engines) -->
            <!--<a>
                <xsl:attribute name="href">
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    <xsl:text>/htmlmap</xsl:text>
                </xsl:attribute>
            </a>-->
		</div>
    </xsl:template>
    <!--**** End addition by IF ****-->
         
	<!--Added by CG - Overrides code in General-Handler - adds PrettyPhoto functionality  -->
    <!-- Generate the license information from the file section -->
    <xsl:template match="mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE' or @USE='ITEM-LICENSE']">
        <div class="license-info">
            
                <xsl:if test="@USE='CC-LICENSE' or @USE='ITEM-LICENSE'">            
                <h2><a name="itemLicence"><i18n:text>xmlui.dri2xhtml.structural.link_cc_heading</i18n:text></a></h2>         
                    <a href="{mets:file/mets:FLocat[@xlink:title='license_url']/@xlink:href}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Licence"><xsl:value-of select="$cc_licence_name"/></a>

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
                	  <a href="http://www.jorum.ac.uk/termsofservice.html?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto" title="Terms of Service"><i18n:text>xmlui.dri2xhtml.structural.link_original_license</i18n:text></a>
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
           
           					<a href="{dri:field/dri:help}?iframe=true&amp;width=75%&amp;height=75%" rel="prettyPhoto[iframes]" title="{dri:field/dri:option}">
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
        			<img src="/xmlui/themes/Jorum/images/warning_16.png" alt="CC Note"/>
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
            <xsl:choose>
                <xsl:when test="$font-sizing &lt; 120">
                    <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="style">font-size: <xsl:value-of select="$font-sizing"/>%;</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
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
                    			<img src="/xmlui/themes/Jorum/images/feed-icon-20x20.png" alt="RSS feed icon" />
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
				<ul class="ds-artifact-list">
					<xsl:apply-templates select="*[not(name()='head')]"
						mode="summaryList" />
				</ul>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
    <!-- END CG 11/11/2009--> 
    
  
    
    
    
         
</xsl:stylesheet>
