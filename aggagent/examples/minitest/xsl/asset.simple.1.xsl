<?xml version="1.0"?> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="LogPlan">
    <LogPlan> 
       <xsl:for-each select="Asset">
          <!-- <xsl:value-of select="Container[@Event='ADD']"/> --> <!-- 'ADD'/'REMOVE'/'CHANGE' -->
          <Asset
             ContainerEvent="{Subscription/@Event}"
             ID="{UID}"
             class="{class}"
             ClusterPG="{ClusterPG/cluster_identifier}"
             OrganizationPG="{OrganizationPG/roles/name}"
          />
       </xsl:for-each>
    </LogPlan>
  </xsl:template>
  
</xsl:stylesheet>