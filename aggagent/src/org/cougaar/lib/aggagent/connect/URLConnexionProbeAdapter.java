/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.connect;


import java.net.*;
import java.io.*;



/**  Webservices modelled via "Probes against URLs."
  *  Can attach custom parsers (and later "gauges") to filter and monitor probes.
  *  The URL is the "proxy" to the WebService
  **/
public class URLConnexionProbeAdapter implements URLConnexionProbe
{
     private URLConnexionConfig myConfig = null;
     private URL myURL;
     private ConnexionParser myParser;
     private BufferedReader myInputDataStream;

     public URLConnexionProbeAdapter (URLConnexionConfig cfg) {
       setConfig(cfg);
       setParser(new ConnexionParserByDelimiter());
     }

     public URLConnexionProbeAdapter(URL u ){
         setURL(u);
         //
         //  DEFAULT CONNECTION PARSER
         //
         setParser(new ConnexionParserByDelimiter() ); // new ConnexionParserSAX() );
     }
     public void setURL( URL u ) {
         myURL = u;
     }

     public void setConfig (URLConnexionConfig cfg) {
       myConfig = cfg;
     }

     public URLConnexionConfig getConfig () {
       return myConfig;
     }

     public boolean isResolved () {
       return myURL != null;
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
