<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" indent="yes"/>

	<xsl:include href="resultset2table.xsl"/>
	<xsl:include href="singleresultset2form.xsl"/>
	<xsl:include href="chart.xsl"/>
	
	<xsl:template match="/">
		<table border="0" width="100%" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<xsl:for-each select="/statistic/block[@name='content']">
						<xsl:call-template name="singleresultset2form">
							<xsl:with-param name="show-title" select="true()"/>
						</xsl:call-template>
					</xsl:for-each>							
				</td>
			</tr>
			<tr>
				<td>
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='totals']">
									<xsl:call-template name="singleresultset2form">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
							<td width="50"></td>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='averages']">
									<xsl:call-template name="singleresultset2form">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td>
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td>
								<xsl:for-each select="/statistic/block[@name='chart']">
									<xsl:call-template name="chart">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
					</table>				
				</td>
			</tr>
			<tr>
				<td>
					<br/>
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='country']">
									<xsl:call-template name="resultset2table">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
							<td width="50"></td>
							<td valign="top">
								<xsl:for-each select="/statistic/block[@name='country-chart']">
									<xsl:call-template name="chart">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
					</table>				
				</td>
			</tr>
			<tr>
				<td>
					<br/>
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td>
								<xsl:for-each select="/statistic/block[@name='views-country']">
									<xsl:call-template name="resultset2table">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
							<td width="30"></td>
							<td>
								<xsl:for-each select="/statistic/block[@name='views-country-chart']">
									<xsl:call-template name="chart">
										<xsl:with-param name="show-title" select="true()"/>
									</xsl:call-template>
								</xsl:for-each>							
							</td>
						</tr>
					</table>				
				</td>
			</tr>						
		</table>
		
		
	</xsl:template>
</xsl:stylesheet>
