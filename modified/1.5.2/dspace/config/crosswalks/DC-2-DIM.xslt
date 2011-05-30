<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	exclude-result-prefixes="dc" version="1.0">

 	<!--
                **************************************************
                DC-2-DIM  ("DSpace Intermediate Metadata" ~ Dublin Core variant)
                For a DSpace INGEST Plug-In Crosswalk
    -->
    <!--   Dublin Core usage guide: ˚http://dublincore.org/documents/usageguide/
                Dublin Core schema links:
                        http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd
                        http://dublincore.org/schemas/xmls/qdc/2003/04/02/dcterms.xsd  -->
	<xsl:output indent="yes" method="xml"/>
	<xsl:strip-space elements="*"/>
	<xsl:variable name="newline">
		<xsl:text/>
	</xsl:variable>
	
	<!-- ****  /GLOBAL, DATA-DETERMINED  VARIABLES  *** -->
	<xsl:template match="text()">
	<!-- Do nothing.  (That is, override the built-in rule (which would print out any otherwise not handled text), and suppress any otherwise not handled text) -->
	</xsl:template>
	
	<!-- Match the root element of the doc -->
	<xsl:template match="/">
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
	

	<!-- **** dc.title **** -->
	<xsl:template match="dc:title">
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
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.language **** -->
	<xsl:template match="dc:language">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">language</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.description **** -->
	<xsl:template match="dc:description">
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
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.creator **** -->
	<xsl:template match="dc:creator">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">creator</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.contributor **** -->
	<xsl:template match="dc:contributor">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">contributor</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.contributor.author **** -->
	<xsl:template match="dc:contributor.author">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">contributor</xsl:attribute>
			<xsl:attribute name="qualifier">author</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.publisher **** -->
	<xsl:template match="dc:publisher">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">publisher</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.date **** -->
	<xsl:template match="dc:date">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">date</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	 </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.format **** -->
	<xsl:template match="dc:format">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">format</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.identifier **** -->
	<xsl:template match="dc:identifier">
	  <xsl:if test="normalize-space(.)">
		<xsl:element name="dim:field">
			<xsl:attribute name="mdschema">dc</xsl:attribute>
			<xsl:attribute name="element">identifier</xsl:attribute>
			<xsl:value-of select="normalize-space(.)"/>
		</xsl:element>
	  </xsl:if>
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.rights **** -->
	<xsl:template match="dc:rights">
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
		<xsl:apply-templates/>
	</xsl:template>
	
	<!-- **** dc.subject **** -->
	<xsl:template match="dc:subject">
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
		<xsl:apply-templates/>
	</xsl:template>
	
</xsl:stylesheet>
