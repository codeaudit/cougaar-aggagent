package org.cougaar.lib.aggagent.connect;


import java.net.*;
import java.io.*;



/** Webservices "Gauging" is modeled with respect to URLs vs. Connections.
  *  The URL is the "proxy" to the WebService
  **/
public class URLConnexionProbeAdapter implements URLConnexionProbe
{
     private URL myURL;
     private ConnexionParser myParser;
     private BufferedReader myInputDataStream;

     public URLConnexionProbeAdapter(URL u ){
         setURL(u);
         setParser(new ConnexionParserByDelimiter() ); // new ConnexionParserSAX() );
     }
     public void setURL( URL u ) {
         myURL = u;
     }

     public void setParser( ConnexionParser parser ){
           myParser  = parser;
     }

     public void init() throws java.io.IOException {
           URLConnection uc = myURL.openConnection();
           myInputDataStream = new BufferedReader(new InputStreamReader(uc.getInputStream()));
     }

     //
     // Connection Gauge Events can be thrown and are Gauge defined.
     // Caller is expected to handle.  If Gauge Event is thrown the status
     // is signified by ConnexionGaugeEvent.getStatus()
     //
     // @return : F = connexion is closed / probe shut
     //           T = assumed connexion is still open / probe still active
     //
     public boolean readConnexion(StringWriter buffer) throws ConnexionGaugeEvent
     {
          boolean connexionOpen = myParser.parse(buffer, myInputDataStream);
          return connexionOpen;
     }
     /**
     public int read(ByteArrayOutputStream buffer, String termination_read_marker) throws XionGaugeEvent {
         ByteArrayOutputStream tempbuffer = new ByteArrayOutputStream(0);
         try{
             byte[] readbuffer = new byte[512];
             int len_marker = 0;
             if( termination_read_marker != null) len_marker= termination_read_marker.length();

             int sz = 0;
             while( sz >= 0 ) {
                  sz = myInputStream.read(readbuffer,0,512-len_marker); // blocks until bytes read
                  if( sz > 0 ) {
                      tempbuffer.write(readbuffer,0,sz);
                      //System.out.println("[ConnectionListener]  incremental_result=" + temp);
                  }
                  if( sz
             }

         } catch( java.io.IOException ex ){
            ex.printStackTrace();
         }
         return buffer.toByteArray();
     }
     **/

     /**
     public URLConnection openConnection() throws java.io.IOException {
          return myURL.openConnection();
     }
     **/
     public String toExternalForm(){
          return "<URLConnexionGauge>" + myURL.toExternalForm() + "</URLConnexionGauge>";
     }
}
