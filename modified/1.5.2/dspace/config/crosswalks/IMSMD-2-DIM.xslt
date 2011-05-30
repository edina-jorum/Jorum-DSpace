<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:imsmd="http://www.imsglobal.org/xsd/imsmd_v1p2"
	exclude-result-prefixes="imsmd" version="1.0">
	
	
	<!-- IMSMD => DIM Stylesheet -->
	

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
	<!-- Do nothing. 
                 (That is, override the built-in rule (which would print out any otherwise not handled text), and suppress any otherwise not handled text) --></xsl:template>
	<!-- *************************** -->
	<!-- **** IMSMD  lom  [ROOT ELEMENT] **** -->
	<!-- *************************** -->

	<xsl:template match="imsmd:lom">
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
	<!-- **** IMSMD   general **** -->
	<!-- *************************** -->
	<xsl:template match="imsmd:general">
		<!-- WR_ Unsure why this next test is here. Benign, but most likely unnecessary.  There will be only one /lom:lom/lom:general  Can't hurt... -->
		<xsl:if test="not(preceding-sibling::imsmd:general)">
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>**** IMSMD GENERAL     ****</xsl:comment>
			<xsl:value-of select="$newline"/>
			<xsl:comment>***************************</xsl:comment>
			<xsl:value-of select="$newline"/>
		</xsl:if>
		
		<xsl:apply-templates/>
     
	</xsl:template>



	<!-- Get the identifier imsmd=>general=>identifier -->
	<xsl:template match="imsmd:identifier">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">identifier</xsl:attribute>
      		<xsl:attribute name="lang">
			</xsl:attribute>
		<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>	
	  </xsl:if>
	</xsl:template>



	<!-- Match the title imsmd=>general=>title=>langstring -->
	<xsl:template match="imsmd:title/imsmd:langstring">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">title</xsl:attribute>
      		<xsl:attribute name="lang">
				<xsl:value-of select="@xml:lang"/>
			</xsl:attribute>
		<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>	
	  </xsl:if>
	</xsl:template>


	<!-- Get the identifier imsmd=>general=>language -->
	<xsl:template match="imsmd:language">
		<xsl:if test="normalize-space(.)">
			<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">language</xsl:attribute>
      			<xsl:attribute name="lang">
					<xsl:value-of select="normalize-space(.)"/>
				</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>		
		</xsl:if>
	</xsl:template>


	<!-- Match the description imsmd=>general=>description=>langstring -->
	<xsl:template match="imsmd:general/imsmd:description/imsmd:langstring">
	 <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">description</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@xml:lang"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	</xsl:if>
	</xsl:template>


	<!-- Match the description imsmd=>general=>keyword=>langstring -->
	<xsl:template match="imsmd:keyword/imsmd:langstring">
	 <!--Test for content - we don't want to output empty elements -->
	 <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">subject</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@xml:lang"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>

	</xsl:template>




	<!-- *************************** -->
	<!-- **** IMSMD   lifeCycle **** -->
	<!-- *************************** -->
	
	<xsl:template match="imsmd:lifecycle">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>**** IMSMD LIFECYCLE   ****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	
	
	<!-- Match the role imsmd=>lifecycle=>contribute -->
	<xsl:template match="imsmd:contribute">
		
		<!--Read value and transform to lower case before testing -->
		<xsl:variable name="value_lc">
                 <xsl:call-template name="translate_lc">
                     <xsl:with-param name="value" select="imsmd:role/imsmd:value"/>
                 </xsl:call-template>     
         </xsl:variable>
		
		
		<!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
		<!-- CG 25/01/11 IssueID #598 Support for 'creator' added -->
		<xsl:choose>
	  	 	<xsl:when test="$value_lc='author'">
				<xsl:call-template name="author" />
			 	<xsl:call-template name="date">
				 <!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
				<xsl:with-param name="value">created</xsl:with-param>
			 	</xsl:call-template>
	 	 	</xsl:when>  
			<xsl:otherwise>
  				<xsl:if test="$value_lc='creator'"> 			
					<xsl:call-template name="author" />
				</xsl:if>
	  		</xsl:otherwise>
	  	</xsl:choose>
      	<!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
      	<xsl:if test="$value_lc='publisher'">
			 <xsl:call-template name="publisher" />
			 <xsl:call-template name="date">
			 <!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
				<xsl:with-param name="value">issued</xsl:with-param>
			 </xsl:call-template>
      	</xsl:if>
		<xsl:if test="$value_lc='content provider' and contains(.,'ORG:')">
			 <xsl:call-template name="project" />
			 <xsl:call-template name="date">
			 <!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
				<xsl:with-param name="value">issued</xsl:with-param>
			 </xsl:call-template>
      	</xsl:if>
		

	</xsl:template>
	
	
	<!--***** Helper methods for subelements of imsmd=>lifecycle=>contribute  *****--> 
	<xsl:template name="project">
	
	<xsl:if test="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))">
		<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">contributor</xsl:attribute>
					<xsl:attribute name="lang">
						<xsl:value-of select="imsmd:role/imsmd:value/imsmd:langstring/@xml:lang"/>
					</xsl:attribute>
	 			<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'ORG:'), '&#xA;' ))"/>
		</xsl:element>
	</xsl:if>
	</xsl:template>
	
	<xsl:template name="author">
	<!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
	<xsl:for-each select="imsmd:centity/imsmd:vcard">
	<xsl:choose>
	
	  <xsl:when test="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))">
		<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">contributor</xsl:attribute>
				<xsl:attribute name="qualifier">author</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))"/>
		</xsl:element>
	  </xsl:when>
	  <!-- CG 16/07/10 Fixed invalid syntax IssueID #379 -->
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
	</xsl:template>
	
	<xsl:template name="publisher">
	<!-- GWaller 10/5/10 IssueID #280 Support vcard with missing FN -->
	<xsl:for-each select="imsmd:centity/imsmd:vcard">
	<xsl:choose>
	  
	  <xsl:when test="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))">
	  	<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">publisher</xsl:attribute>
				<xsl:value-of select="normalize-space(substring-before(substring-after(. ,'FN:'), '&#xA;' ))"/>
		</xsl:element>
	  </xsl:when>
	  
	  <!-- CG 16/07/10 Fixed invalid syntax IssueID #379 -->
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
	</xsl:template>
	
	<xsl:template name="date">
	<!-- GWaller 10/5/10 IssueID #290 Use specific date qualifier depending on author/publisher etc entity -->
	  <xsl:param name="value"/>
	  <xsl:if test="normalize-space(imsmd:date/imsmd:datetime)">
		<xsl:element name="dim:field">
				<xsl:attribute name="mdschema">dc</xsl:attribute>
				<xsl:attribute name="element">date</xsl:attribute>
				<xsl:attribute name="qualifier"><xsl:value-of select="$value"/></xsl:attribute>
				<xsl:value-of select="normalize-space(imsmd:date/imsmd:datetime)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	<!--**** End  imsmd=>lifecycle=>contribute helper methods ****-->


	<!-- *************************** -->
	<!-- **** IMSMD   Technical **** -->
	<!-- *************************** -->
	
	<xsl:template match="imsmd:technical">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>**** IMSMD Technical   ****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>


	<!-- Match the technical format imsmd=>technical=>format -->
	<xsl:template match="imsmd:format">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">format</xsl:attribute>
			<xsl:attribute name="lang">
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	
	<!-- *************************** -->
	<!-- ****  IMSMD   Rights   **** -->
	<!-- *************************** -->
	
	<xsl:template match="imsmd:technical">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>****   IMSMD Rights    ****</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	
	
	<!-- Match the Rights description imsmd=>rights=>description=>langstring -->
	<xsl:template match="imsmd:rights/imsmd:description/imsmd:langstring">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">rights</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="@xml:lang"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	
	
	<!-- *************************** -->
	<!-- *** IMSMD Classification ** -->
	<!-- *************************** -->
	
	<xsl:template match="imsmd:classification">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>*** IMSMD Classification **</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	


	
	<!-- Match the taxonpath imsmd=>classification=>taxonpath -->
	<xsl:template match="imsmd:classification/imsmd:taxonpath">
			<xsl:call-template name="taxon_entry" />
	</xsl:template>
	
	<!--***** Helper methods for subelements of imsmd=>classification=>taxonpath   *****--> 
	
	<xsl:template name="taxon_entry">
	  <xsl:if test="normalize-space(imsmd:taxon/imsmd:entry/imsmd:langstring)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">subject</xsl:attribute>
			<xsl:attribute name="qualifier">classification</xsl:attribute>
			<xsl:attribute name="lang">
				<xsl:value-of select="imsmd:taxon/imsmd:entry/imsmd:langstring/@xml:lang"/>
			</xsl:attribute>
			<xsl:value-of select="normalize-space(imsmd:taxon/imsmd:entry/imsmd:langstring)"/>
		</xsl:element>
	  </xsl:if>
	</xsl:template>
	<!--**** End  imsmd=>classification=>taxonpath methods ****-->
	

	
	<!-- *************************** -->
	<!-- **** IMSMD Annotation   *** -->
	<!-- *************************** -->
	
	<xsl:template match="imsmd:annotation">
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>**** IMSMD Annotation   ***</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:comment>***************************</xsl:comment>
		<xsl:value-of select="$newline"/>
		<xsl:apply-templates/>
	</xsl:template>
	

</xsl:stylesheet>
