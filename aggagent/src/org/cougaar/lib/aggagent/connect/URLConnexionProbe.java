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

     public void setConfig (URLConnexionConfig cfg);

     public URLConnexionConfig getConfig ();

     public boolean isResolved ();

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
