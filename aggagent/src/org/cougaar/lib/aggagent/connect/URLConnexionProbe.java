package org.cougaar.lib.aggagent.connect;

import java.io.StringWriter;
import java.net.*;


/**  Webservices modelled via "Probes against URLs."
  *  Can attach custom parsers (and later "gauges") to filter and monitor probes.
  *  The URL is the "proxy" to the WebService
  **/
public interface URLConnexionProbe
{
     public String toExternalForm();

     //public URLConnection openConnection() throws java.io.IOException;

     public void setURL( URL u );


     public void setParser( ConnexionParser parser );

     // implicitly calls openConnection()...
     public void init() throws java.io.IOException;

     //
     // Connection Gauge Events can be thrown and are Gauge defined.
     // Caller is expected to handle.  If Gauge Event is thrown the status
     // is signified by ConnexionGaugeEvent.getStatus()
     //
     // @return : F = connexion is closed / probe shut
     //           T = assumed connexion is still open / probe still active
     //
     // readConnexion() writes an a complete "unit" of data from
     // connexion and writes to buffer.    What a "unit" of data is
     // is defined by assigned Parser
     //
     public boolean readConnexion(StringWriter buffer) throws ConnexionGaugeEvent;
}
