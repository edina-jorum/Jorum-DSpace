<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:template name="resultset2table">
		<xsl:param name="show-title"/>

		<xsl:if test="$show-title">
			<div class="BlockTitle"><xsl:value-of select="@title"/></div>
		</xsl:if>

		<table class="statTable">
			<xsl:for-each select="resultset/result[1]">
				<xsl:call-template name="table-header-row"/>
			</xsl:for-each>
			<xsl:for-each select="resultset/result">
				<xsl:call-template name="table-data-row"/>
			</xsl:for-each>
			<xsl:for-each select="resultset/total">
				<xsl:call-template name="table-total-row"/>
			</xsl:for-each>			
		</table>
		<br/>

    </xsl:template>
	
	<xsl:template name="table-header-row">
		<tr>
		<xsl:for-each select="child::node()">
			<xsl:call-template name="table-header-cell"/>
		</xsl:for-each>
		</tr>
	</xsl:template>

	<xsl:template name="table-header-cell">
		<th class="statTableHeader" align="left">
			<xsl:value-of select="@name"/>
		</th>	
	</xsl:template>
	
	<xsl:template name="table-data-row">
		<tr>
		<xsl:variable name="rowstyle">
			<xsl:choose>
				<xsl:when test="(position() mod 2) = 0">statEvenRow</xsl:when>
				<xsl:otherwise>statOddRow</xsl:otherwise>
			</xsl:choose>     
		</xsl:variable>
		<xsl:for-each select="child::node()">
			<xsl:call-template name="table-data-cell">
				<xsl:with-param name="rowstyle" select="$rowstyle"/>
			</xsl:call-template>
		</xsl:for-each>
		</tr>
	</xsl:template>

	<xsl:template name="table-data-cell">
		<xsl:param name="rowstyle"/>
		<xsl:variable name="colstyle">
			<xsl:choose>
				<xsl:when test="(position() mod 2) = 0">
					<xsl:value-of select="concat($rowstyle,'EvenCol')"/>
				</xsl:when>
				<xsl:otherwise><xsl:value-of select="concat($rowstyle,'OddCol')"/></xsl:otherwise>
			</xsl:choose>     
		</xsl:variable>

		<xsl:variable name="align">
			<xsl:value-of select="@align"/>
		</xsl:variable>

		<xsl:variable name="bandeira">
			<xsl:value-of select="."/>
		</xsl:variable>
		<xsl:variable name="pais">
			<xsl:value-of select="substring($bandeira,9)"/>
		</xsl:variable>
		<td class="{$colstyle}" align="{$align}">
		
		<xsl:choose>
			<xsl:when test="contains($bandeira,'flag:')">
				<xsl:variable name="codigo">
					<xsl:value-of select="substring($bandeira,6,2)"/>
				</xsl:variable>
				<img alt="{$pais}" src="stats/img/flags/{$codigo}.png"/>
				<xsl:value-of select="concat(' ',substring($bandeira,9))"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="."/>				
			</xsl:otherwise>
		</xsl:choose>  
		</td>
	</xsl:template>

	<xsl:template name="table-total-row">
		<tr>
		<xsl:variable name="rowstyle">
			<xsl:choose>
				<xsl:when test="(position() mod 2) = 0">statEvenRow</xsl:when>
				<xsl:otherwise>statOddRow</xsl:otherwise>
			</xsl:choose>     
		</xsl:variable>
		<xsl:for-each select="child::node()">
			<xsl:call-template name="table-total-cell">
				<xsl:with-param name="rowstyle" select="$rowstyle"/>
			</xsl:call-template>
		</xsl:for-each>
		</tr>
	</xsl:template>

	<xsl:template name="table-total-cell">
		<xsl:param name="rowstyle"/>
		<xsl:variable name="colstyle">
			<xsl:choose>
				<xsl:when test="(position() mod 2) = 0">
					<xsl:value-of select="concat($rowstyle,'EvenCol')"/>
				</xsl:when>
				<xsl:otherwise><xsl:value-of select="concat($rowstyle,'OddCol')"/></xsl:otherwise>
			</xsl:choose>     
		</xsl:variable>

		<xsl:variable name="align">
			<xsl:value-of select="@align"/>
		</xsl:variable>

		<td class="{$colstyle}" align="{$align}">
			<b><xsl:value-of select="."/></b>
		</td>
	</xsl:template>

</xsl:stylesheet>
