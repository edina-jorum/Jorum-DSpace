<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- <xsl:output method="xml" encoding="ISO-8859-1"/> -->

   <xsl:template match="/">
            <xsl:apply-templates/>
   </xsl:template>

    <xsl:template match="node()|@*">
      <xsl:copy>
       <xsl:apply-templates select="@*"/>
       <xsl:apply-templates/>
     </xsl:copy>
   </xsl:template>
   
   
   <xsl:template match="text()">
      <xsl:choose>
        <xsl:when test="contains(.,'&amp;')">
          <xsl:call-template name="fixampersands">
            <xsl:with-param name="data" select="."/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>  
   </xsl:template>


<xsl:template name="fixampersands">
  <xsl:param name="data"/>
  <xsl:variable name="after" select="substring-after($data,'&amp;')"/>

  <xsl:value-of select="substring-before($data,'&amp;')"/>
  <xsl:choose>
    <xsl:when test="contains($after,';')">
      <xsl:choose>
        <xsl:when test="contains(substring-before($after,';'), ' ') or contains(substring-before($after,';'), '&#10;') or contains(substring-before($after,';'), '&#09;')"> 
          <xsl:text>&amp;</xsl:text> 
        </xsl:when>
        <xsl:when test="contains(substring-before($after,';'), '&#38;')">
          <!-- Coincidently, a semicolon appears in the same text as an ampersand -->
          <xsl:text>&amp;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <!-- There is only a Name between the ampersand and semicolon -->
          <xsl:text disable-output-escaping="yes">&amp;</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>&amp;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
    <xsl:when test="contains($after,'&amp;')">
      <xsl:call-template name="fixampersands">
        <xsl:with-param name="data" select="$after"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$after"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
