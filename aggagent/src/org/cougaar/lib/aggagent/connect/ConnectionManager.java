/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.lib.aggagent.connect;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.w3c.dom.*;

import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.util.ConfigFileFinder;

import org.cougaar.lib.aggagent.Configs;
import org.cougaar.lib.aggagent.xml.XMLParseCommon;

//#######################################################################
public class ConnectionManager
{
    private static Map urlSpaces = Collections.synchronizedMap(new HashMap()); // set of URLs partitioned into Spaces (Flushable)

    // as URLs consumed in urlSpace (Connection opened and closed),
    // Closed Connection moved into deadConnections.. dead connections
    // managed at CM level... active Connections pooled and handled globally
    // saving dead connections allows CM to retry...
    private List deadConnections = Collections.synchronizedList(new ArrayList());
    //
    // Connections as they acquire xml documents (data) add them to the pool
    //
    private List dataPool = Collections.synchronizedList(new ArrayList());

    // Connection Timers apply to poll queries only.
    // they are created by <pollInterval/> spec
    // If not defined and Poll Query default to POLL_INTERVAL
    // If KeepAlive query and defined -- no effect.
    //
    private static Map connectionTimers = Collections.synchronizedMap(new HashMap());

    // Keep a reference to the NameService on-hand, in case the first round of
    // connection lookups isn't entirely successful.
    private NameService myNameServiceProvider = null;

    //private Vector myRawURLs = new Vector(); // Vector = synchronized  , raw URLs
    private ConnectionLogger myConnectionLogger = null;

    /**
      *  @param urlsXmlFile :
      *                    <!-- pattern 1 -->
      *                    <xml>
      *                         <source>
      *                            <!-- the url of nameServiceLookUp(name) + pspquery needs to be unique -->
      *                            <name>3ID</name>  <!--  look up in nameservice first -->
      *                            <pspquery>/alpine/demo/GENERIC.PSP?SHOW_ME</pspquery>
      *                            <PollThresh> XX </PollThresh>
      *                         </source>
      *                    </xml>
      *
      *                    <!-- pattern 2 -->
      *                    <xml>
      *                         <source>
      *                            <name>Yahoo</name> <!-- not going to find in nameservice -->
      *                            <url>http://www.yahoo.com</url>  <!-- tries this next -->
      *                            <PollThresh> XX </PollThresh>
      *                         </source>
      *                    </xml>
      *
      *  @param myNameServiceProvider
      *  @param cluster_url_resource : eg. url resource at cluster host, eg:  "/alpine/demo/GENERIC.PSP"
      *                                  NOTE - DOES NOT APPLY TO EXPLICIT URLS in config file
      **/
    public ConnectionManager(ConnectionLogger c, String configXMLFileName,
                             NameService nsp)
    {
         myConnectionLogger = c;
         myNameServiceProvider = nsp;

         try {
             Document doc = ConfigFileFinder.parseXMLConfigFile(configXMLFileName);
             Element root = doc.getDocumentElement();
             parseXMLConfigFile(doc);
         }
         catch (Exception ex) {
           ex.printStackTrace();
         }
    }

    private void parseXMLConfigFile (Document doc) {
      NodeList nl = doc.getElementsByTagName("source");
      int size = nl.getLength();
      for (int i = 0; i < size; i++) {
        Node n = (Node)nl.item(i);

        // Only process this Node if not turned off...
        // ie <source ... ignore="true">
        String ignored = XMLParseCommon.getAttributeOrChildNodeValue("ignore", n);
        if (ignored == null || ignored.toLowerCase().indexOf("true") == -1)
          createConnection(new URLConnexionConfig(n));
      }
    }

    private void createConnection (URLConnexionConfig cfg) {
      // try to ascertain the polling interval
      long interval = -1;
      if (cfg.interval != null) {
        try {
          interval = Long.parseLong(cfg.interval);
        }
        catch (NumberFormatException nfe) {
          System.out.println(
            "ConnectionManager::createConnection:  bad interval--" + cfg.name);
        }
      }

      URLConnexionProbe p = null;
      // If a URL is specified, it takes precedence over the name server.
      // Note:  the pspquery is ignored in this case
      String resolved_url = null;
      if (cfg.url != null) {
        resolved_url = cfg.url;
      }
      else if (cfg.name != null) {
        resolved_url = resolve(cfg);
        if (resolved_url == null)
          p = new URLConnexionProbeAdapter(cfg);
      }

      try {
        if (resolved_url != null)
          p = new URLConnexionProbeAdapter(new URL(resolved_url));
        if (p != null) {
          addURL(p);
          if (interval > -1)
            connectionTimers.put(p, new ConnexTimer(interval));
        }
        else
          System.out.println(
            "ConnectionManager::createConnection:  No resource specified!");
      }
      catch (MalformedURLException mfe) {
        System.out.println(
          "ConnectionManager::createConnection:  bad URL -- " + resolved_url);
      }
    }

    private String resolve (URLConnexionConfig cfg) {
      URL base = myNameServiceProvider.lookupURL(cfg.name);
      if (base == null)
        return null;
      else if (cfg.psp != null)
        return base.toExternalForm() + cfg.psp;
      else
        return base.toExternalForm();
    }

    public String getNamespace(){
        return this.toString();
    }

    public ConnectionLogger getLogger() { return myConnectionLogger; }

    public void addURL(URL u){
        myConnectionLogger.log("<FONT COLOR=RED>Adding URL: <FONT COLOR=GREEN SIZE=-1>" + u.toExternalForm()
                 + "</FONT> to space:" +  getNamespace() + "</FONT>");
        //myURLs.add(u);
        URLConnexionProbe ucg = new URLConnexionProbeAdapter(u);
        // ucg.setParser(parser);
        this.addURL(ucg);
    }
    public void addURLWithTimer(URL u, long min_time){
        myConnectionLogger.log(
                "<FONT COLOR=RED>Adding URL With <font color=blue>Timer("
                + min_time + ")</font>: <FONT COLOR=GREEN SIZE=-1>"
                + u.toExternalForm()
                + "</FONT> to space:" +  getNamespace() + "</FONT>");
        //myURLs.add(u);
        URLConnexionProbe ucg = new URLConnexionProbeAdapter(u);
        ConnexTimer timer = new ConnexTimer(min_time);
        connectionTimers.put(ucg,timer);
        this.addURL(ucg);
    }

    public void flush() {
        int outstanding_conns = ConnectionListener.spaceSize(getNamespace());
        myConnectionLogger.log("<FONT COLOR=RED>Flush command received</FONT>"
                               + ", URLs flushed in space=" + this.sizeURLs()
                               + ", dead connections flushed in space=" + deadConnections.size()
                               + ", (warning?) outstanding live connections not flushed=" + outstanding_conns);
        this.clearURLs();
        synchronized( deadConnections )
        {
             deadConnections.clear();
        }
    }


    public int numLiveConnections() {
        return ConnectionListener.spaceSize(this.getNamespace());
    }

    //
    // @return : void
    public synchronized void  connectAllAsynchronous(){

         myConnectionLogger.log("Call: connectAllAsynchronous");
         synchronized( urlSpaces )
         {
             List space_set = (List)urlSpaces.get(this);
             if( space_set == null ) {
                 System.err.println("[ConnectionManger.connectAllAsynchronous()] NO URL SPACE found!!");
                 System.err.println("[ConnectionManger.connectAllAsynchronous()] Be sure THERE ARE ACTIVE URLS TO CONNECT");
                 myConnectionLogger.log(
                     "<Font color=red>NO ACTIVE URL SPACE found in ConnectionManager.connectAllAsynchronous().</font>"
                                );
                 return; // ABORT
             }
             synchronized( space_set )
             {
                 Iterator it = space_set.iterator();
                 while(it.hasNext()){
                    URLConnexionProbe u = (URLConnexionProbe)it.next();
                    ConnectionListener con =
                         new ConnectionListener(
                             u, myConnectionLogger, getNamespace(),
                             deadConnections, dataPool);
                    if (u.isResolved()) {
                      // if the timer exists, initialize its wait interval
                      ConnexTimer timer = (ConnexTimer) connectionTimers.get(u);
                      if (timer != null)
                        timer.proceed();
                      con.start();
                    }
                    else
                      deadConnections.add(con);
                 }
             }
         }
    }

    public List cycleAllAsynchronousConnections (boolean recycle) {
      // restart dead connections, maybe
      if (recycle) {
        try {
          List copy =
            Collections.synchronizedList(new ArrayList(deadConnections));
          for (Iterator i = copy.iterator(); i.hasNext(); ) {
            ConnectionListener cl = (ConnectionListener) i.next();
            URLConnexionProbe probe = cl.getConnexionProbe();
            ConnexTimer timer = (ConnexTimer) connectionTimers.get(probe);
            if (!probe.isResolved()) {
              String resolved_url = resolve(probe.getConfig());
              if (resolved_url != null) {
                try {
                  probe.setURL(new URL(resolved_url));
                }
                catch (MalformedURLException mfe) {
                  System.out.println(
                    "ConnectionManager::cycleAllAsynchronousConnections:  " +
                    "bad URL -- " + resolved_url);
                  connectionTimers.remove(probe);
                  deadConnections.remove(cl);
                }
              }
            }
            // start the connection if it is ready; else try again later
            if (probe.isResolved() && (timer == null || timer.proceed()))
              cl.start();
          }
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }

      // return results
      List returnData = null;
      synchronized (dataPool) {
        returnData = new ArrayList(dataPool);
        dataPool.clear();
      }
      return returnData;
    }

    //
    // @param : recycle=true, relaunch any dead connections
    //                  NOTE, keep alive connecitons don't die.
    //
    // @return : data harvested from dataPool
    /*
    public List cycleAllAsynchronousConnections(boolean recycle)
    {
         List returnData = null;
         synchronized( dataPool )
         {
            returnData = new ArrayList(dataPool);
            //System.out.println("[ConnectionManager] dataPool size=" + dataPool.size() + ", " + returnData.size());
            dataPool.clear();
         }

         // CASE DO NOT RECYCLE #########
         if( false==recycle) {
             try{
                    System.out.println("Checking... Still not done..."
                                    + ConnectionListener.describeSetOfListeners(getNamespace(), deadConnections));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
        } else {
        // CASE RECYCLE ################

                try{
                    List copy = Collections.synchronizedList(new ArrayList(this.deadConnections));
                    synchronized( copy )
                    {
                       Iterator it2 = copy.iterator();
                       while(it2.hasNext() ){
                            ConnectionListener cl = (ConnectionListener)it2.next();
                            URLConnexionProbe probe = cl.getConnexionProbe();
                            ConnexTimer timer = (ConnexTimer)connectionTimers.get(probe);
                            if( timer != null ){
                                // presume Polling query with timer
                                if( timer.proceed() == true ) cl.start();
                                // else, try later!
                            } else {
                                cl.start();
                            }
                       }
                    }
               } catch (Exception ex) {
                       ex.printStackTrace();
               }
        }
        return returnData;
    }
   */

   public static void main(String[] args) {
        System.out.println("[ConnectionManager.main() called...]");
        String configXMLFileName = null;
    }

    private synchronized void addURL(URLConnexionProbe u) {
        List space_set = (List)urlSpaces.get(this);
        if( space_set == null){
            space_set = Collections.synchronizedList(new ArrayList());
            urlSpaces.put(this,space_set);
        }
        space_set.add(u);
    }
    private synchronized int sizeURLs() {
        List space_set = (List)urlSpaces.get(this);
        return space_set.size();
    }
    private synchronized void clearURLs() {
        List space_set = (List)urlSpaces.get(this);
        space_set.clear();
    }
}
