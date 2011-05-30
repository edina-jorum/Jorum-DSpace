<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:include href="resultset2table.xsl"/>
	<xsl:include href="chart.xsl"/>
	
	<xsl:template match="/">
	
		<table border="0" cellpadding="0" cellspacing="0">
			<xsl:for-each select="/statistic/block">
				<tr>
					<td>
						<xsl:choose>
							<xsl:when test="@type = 'query'">
								<xsl:call-template name="resultset2table">
									<xsl:with-param name="show-title" select="true()"/>
								</xsl:call-template>
							</xsl:when>
							<xsl:when test="@type = 'chart'">
								<xsl:call-template name="chart">
									<xsl:with-param name="show-title" select="true()"/>
								</xsl:call-template>
							</xsl:when>
							<xsl:when test="@type = 'html'">
								<xsl:value-of select="." disable-output-escaping="yes"/>
							</xsl:when>
						</xsl:choose>
					</td>
				</tr>
			</xsl:for-each>
		</table>
		
	</xsl:template>
</xsl:stylesheet>
