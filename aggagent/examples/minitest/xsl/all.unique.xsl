<?xml version="1.0"?> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="LogPlan">
    <LogPlan> 
       <xsl:for-each select="*|/">
          <UniqueObject 
             ID="{ID/UID}"
             UID="{UID}"
             uid="{uid}"
             id="{id}"
          />
       </xsl:for-each>
    </LogPlan>
  </xsl:template>

</xsl:stylesheet>