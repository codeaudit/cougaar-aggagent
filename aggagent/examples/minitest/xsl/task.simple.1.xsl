<?xml version="1.0"?> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="LogPlan">
    <LogPlan> 
       <xsl:for-each select="Task">
          <Task 
             ContainerEvent="{Subscription/@Event}"
             verb="{verb}"
             ID="{ID/UID}"
             source="{source}"
             destination="{destination/address}"
          />
       </xsl:for-each>
    </LogPlan>
  </xsl:template>
  

</xsl:stylesheet>