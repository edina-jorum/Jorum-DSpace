<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:include href="resultset2table.xsl"/>
	<xsl:include href="chart.xsl"/>
	
	<xsl:template match="/">
		<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td valign="top">
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='files']">
									<xsl:call-template name="resultset2table">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
						<tr>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='format']">
									<xsl:call-template name="resultset2table">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
					</table>
				</td>
				<td width="50"></td>
				<td valign="top">
								<xsl:for-each select="/statistic/block[@name='format-chart']">
									<xsl:call-template name="chart">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>											
				</td>
			</tr>
		</table>
		
		
	</xsl:template>
</xsl:stylesheet>

