<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:template name="singleresultset2form">
		<xsl:param name="show-title"/>

		<xsl:if test="$show-title">
			<div class="BlockTitle"><xsl:value-of select="@title"/></div>
		</xsl:if>

		<table border="0" align="left" class="statTable">
			<xsl:for-each select="resultset/result">
				<xsl:call-template name="form-row"/>
			</xsl:for-each>
		</table>
		<br/>

	</xsl:template>

	<xsl:template name="form-row">
		<tr>
		<xsl:for-each select="child::node()">
			<xsl:variable name="align">
				<xsl:value-of select="@align"/>
			</xsl:variable>

			<tr>
				<td>
					<xsl:value-of select="@name"/>
				</td>
				<td width="5">:</td>
				<td align="{$align}">
					<xsl:value-of select="."/>
				</td>				
			</tr>
		</xsl:for-each>
		</tr>
	</xsl:template>
	
</xsl:stylesheet>
