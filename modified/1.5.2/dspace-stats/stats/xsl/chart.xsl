<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:template name="chart">
		<xsl:param name="show-title"/>
	
		<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td align="center">
					<xsl:if test="$show-title">
						<div class="BlockTitle"><xsl:value-of select="@title"/></div>
					</xsl:if>
				</td>
			</tr>
			<tr>
				<td>
					<xsl:for-each select="chart">
						<xsl:variable name="chart-url">
							<xsl:value-of select="@chart-url"/>
						</xsl:variable>
						<img src="{$chart-url}" border="0" />
					</xsl:for-each>
				</td>
			</tr>
		</table>
	</xsl:template>

</xsl:stylesheet>
