<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:lom="http://ltsc.ieee.org/xsd/LOM" xsi:schemaLocation="http://ltsc.ieee.org/xsd/LOM http://ltsc.ieee.org/xsd/lomv1.0/lomLoose.xsd"
	exclude-result-prefixes="lom" version="1.0">
	<!--
		************************************************** LOM-2-DIM ("DSpace
		Intermediate Metadata" ~ Dublin Core variant) For a DSpace INGEST
		Plug-In Crosswalk (This) XSLT - William Reilly wreilly@mit.edu
		Crosswalk - Rob Wolfe rwolfe@mit.edu DSpace Plugin - Larry Stone
		lcs@mit.edu ************************************************** (c)
		Massachusetts Institute of Technology, 2005 This work is licensed
		under the Creative Commons Attribution-ShareAlike License. To view a
		copy of this license, visit
		http://creativecommons.org/licenses/by-sa/2.5/ or send a letter to
		Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305,
		USA.
	-->

	<!--
		Dublin Core usage guide: http://dublincore.org/documents/usageguide/
		Dublin Core schema links:
		http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd
		http://dublincore.org/schemas/xmls/qdc/2003/04/02/dcterms.xsd
	-->
	<xsl:output indent="yes" method="xml"/>
	<xsl:strip-space elements="*"/>
	<xsl:variable name="newline">
		<xsl:text/>
	</xsl:variable>

	<!--Used for converting between upper and lower case in string matches -->
	<xsl:variable name="lower">abcdefghijklmnopqrstuvwxyz</xsl:variable>
	<xsl:variable name="upper">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable> 
	
	<!-- Template to convert string from upper to lower case -->
	<xsl:template name="translate_lc">
         <xsl:param name="value"/>
         <xsl:value-of select="normalize-space(translate($value, $upper, $lower))"/>
     </xsl:template>

	<!-- ****  /GLOBAL, DATA-DETERMINED  VARIABLES  *** -->
	<xsl:template match="text()">
	<!--
		Do nothing. (That is, override the built-in rule (which would print
		out any otherwise not handled text), and suppress any otherwise not
		handled text)
	-->
	</xsl:template>
	<!-- *************************** -->
	<!-- **** LOM  lom  [ROOT ELEMENT] ====> DC n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:lom">
		<xsl:element name="dim:dim">
			<xsl:value-of select="$newline"/>
			<xsl:comment>IMPORTANT NOTE:
				***************************************************************************************
				THIS "Dspace Intermediate Metadata" ('DIM') IS **NOT** TO BE USED FOR
				INTERCHANGE WITH OTHER SYSTEMS.
				***************************************************************************************
				It does NOT pretend to be a standard, interoperable representation of Dublin
				Core. It is EXPRESSLY used for transformation to and from source metadata XML
				vocabularies into and out of the DSpace object model. See
				http://wiki.dspace.org/DspaceIntermediateMetadata For more on Dublin Core
				standard schemata, see:
				http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd
				http://dublincore.org/schemas/xmls/qdc/2003/04/02/dcterms.xsd Dublin Core
				usage guide: http://dublincore.org/documents/usageguide/ Also:
				http://dublincore.org/documents/dc-rdf/</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:apply-templates/>
		</xsl:element>
	</xsl:template>
	<!-- *************************** -->
	<!-- **** LOM   general ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:general">
		<!-- WR_ Unsure why this next test is here. Benign, but most likely unnecessary.  There will be only one /lom:lom/lom:general  Can't hurt... -->
		<xsl:if test="not(preceding-sibling::lom:general)">
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>**** LOM GENERAL  *********</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
		</xsl:if>
		
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** LOM   general/title ====> DC title **** -->
	<xsl:template match="lom:title/lom:string">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">title</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@language"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>


	<!-- **** LOM  general/description ====> DC  description **** -->
	<xsl:template match="lom:general/lom:description/lom:string">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">description</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@language"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	
	<!-- **** LOM  general/identifier/entry ====> DC  identifier **** -->
	<xsl:template match="lom:lom/lom:general/lom:identifier/lom:entry">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">identifier</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	
	<!-- **** LOM  general/language ====> DC language  **** -->
	<xsl:template match="lom:language">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">language</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	<!-- **** LOM  general/keyword ====> DC subject  **** -->
	<xsl:template match="lom:keyword">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">subject</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@language"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	
	
	<!-- *************************** -->
	<!-- **** LOM   lifeCycle ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:lifeCycle">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>**** LOM LIFECYCLE    *****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>


	
	<!-- **** LOM   lifeCycle/contribute/entity ====> DC contributor.author **** -->
	<xsl:template match="lom:contribute">
	
		<xsl:variable name="value_lc">
                 <xsl:call-template name="translate_lc">
                     <xsl:with-param name="value" select="lom:role/lom:value"/>
                 </xsl:call-template>     
         </xsl:variable>
			
		<xsl:if test="$value_lc='author'">
		  <!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
		  <xsl:for-each select="lom:entity">
		  <xsl:choose>
		  <xsl:when test="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">contributor</xsl:attribute>
				<xsl:attribute name="qualifier">author</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))"/>			
			</xsl:element>
		  </xsl:when>
		  
		  <xsl:otherwise>
		  	<xsl:if test="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">contributor</xsl:attribute>
				<xsl:attribute name="qualifier">author</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))"/>			
			</xsl:element>
		  </xsl:if>
		  </xsl:otherwise>
		  
		  </xsl:choose>
		  </xsl:for-each>
		<xsl:call-template name="date">
		<!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
		<xsl:with-param name="value">created</xsl:with-param>
		</xsl:call-template>

      	</xsl:if>
      	
      	<xsl:if test="$value_lc='publisher'">
      	<!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
      	<xsl:for-each select="lom:entity">
		  <xsl:choose>
      	  <xsl:when test="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">publisher</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))"/>					
			</xsl:element>
		  </xsl:when>
		  
		  <xsl:otherwise>
		  	<xsl:if test="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">publisher</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))"/>			
			</xsl:element>
		  </xsl:if>
		  </xsl:otherwise>
		  
		  </xsl:choose>
		</xsl:for-each>
		  
		<xsl:call-template name="date">
		<!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
		<xsl:with-param name="value">issued</xsl:with-param>
		</xsl:call-template>

      	</xsl:if>
      	
      	
	</xsl:template>
	

	<!-- **** LOM   lifeCycle/contribute/date/dateTime ====> DC date.available? **** -->
	<xsl:template name="date">
	<!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
	<xsl:param name="value"/>
	  <xsl:if test="normalize-space(lom:date)">
    	<xsl:element name="dim:field">
        	<xsl:attribute name="mdschema">dc</xsl:attribute>
            <xsl:attribute name="element">date</xsl:attribute>
            <xsl:attribute name="qualifier"><xsl:value-of select="$value"/></xsl:attribute>
            <xsl:value-of select="normalize-space(lom:date)"/>
        </xsl:element>
      </xsl:if>
    </xsl:template>

	<!-- *************************** -->
	<!-- **** LOM   technical ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:technical">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***** LOM TECHNICAL   *****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** LOM  technical/format ====> DC  format **** -->
	<xsl:template match="lom:format">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">format</xsl:attribute>
			<!--<xsl:attribute name="qualifier">mimetype</xsl:attribute>-->
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	<!-- *************************** -->
	<!-- **** LOM   educational ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:educational">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>**** LOM EDUCATIONAL  *****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!--  GWaller 2/10/09 Commenting out as this throws a DSpace exception when matched! -->
	<!--  <xsl:template match="lom:context/lom:value">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">audience</xsl:attribute>
			<xsl:attribute name="qualifier">educationlevel</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	</xsl:template> -->
	
	<!-- *************************** -->
	<!-- **** LOM   rights ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:rights">
		<xsl:if test="not(preceding-sibling::lom:rights)">
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>**** LOM RIGHTS 	   ******</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
		</xsl:if>
		<xsl:apply-templates/>
	
		<xsl:call-template name="check-relation"/>
	</xsl:template>
	<!-- **** LOM  rights/description ====> DC rights  **** -->
	<xsl:template match="/lom:lom/lom:rights/lom:description/lom:string">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">rights</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@language"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	<!-- *************************** -->
	<!-- **** LOM   relation ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template name="check-relation">
		<xsl:if test="not(/lom:lom/lom:relation)">
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>**** LOM RELATION     *****</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>(Note: No "Relationship" information recorded for Course HomePage
				LOM. (There will be for Section and Resource LOM.))</xsl:comment>
			<xsl:value-of select="$newline"/>
		</xsl:if>
	</xsl:template>
	
	<!-- *************************** -->
	<!-- **** LOM   classification ====> DC  n/a **** -->
	<!-- *************************** -->
	<xsl:template match="lom:classification">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>*** LOM CLASSIFICATION ****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- GWaller 2/10/09 Commenting out - DSpace displays any dc:subject as keywords in the GUI - maybe we shoudl store this under dc:subject qualified as classification?? -->
	<!-- CG 30/11/09 - CG unncommenting and storing in dc.subject.classification-->
	<!-- **** LOM     /lom/classification/taxonPath
                 ====> DC subject.{taxon/string} e.g. lcsh; cip (Note: lower-case)  **** -->
	  <xsl:template match="lom:taxonPath">
		<xsl:if test="normalize-space(lom:taxon/lom:entry/lom:string)">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">subject</xsl:attribute>
				<xsl:attribute name="qualifier">classification</xsl:attribute>
				<xsl:attribute name="lang">
					<xsl:value-of select="lom:taxon/lom:entry/lom:string/@language"/>
				</xsl:attribute>
				<xsl:value-of select="normalize-space(lom:taxon/lom:entry/lom:string)"/>
			</xsl:element>
		</xsl:if>
	</xsl:template>
	
</xsl:stylesheet>
