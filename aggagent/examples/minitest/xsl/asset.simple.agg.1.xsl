<?xml version="1.0"?> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="LogPlan">
    <LogPlan> 
       <xsl:for-each select="Asset">
          <Asset> <class><xsl:value-of select="@class"/></class>
                 <id><xsl:value-of select="@ID"/></id>
                 <ClusterPG><xsl:value-of select="@ClusterPG"/></ClusterPG>
                 <OrganizationPG><xsl:value-of select="@OrganizationPG"/></OrganizationPG>
          </Asset>
       </xsl:for-each>
    </LogPlan>
  </xsl:template>
  

</xsl:stylesheet>