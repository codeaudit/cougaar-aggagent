<?xml version="1.0"?> 
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="LogPlan">
    <LogPlan> 
       <xsl:for-each select="Task">
          <Task> <verb><xsl:value-of select="@verb"/></verb>
                 <id><xsl:value-of select="@ID"/></id>
                 <source><xsl:value-of select="@source"/></source>
                 <destination><xsl:value-of select="@destination"/></destination>
          </Task>
       </xsl:for-each>
    </LogPlan>
  </xsl:template>
</xsl:stylesheet>

