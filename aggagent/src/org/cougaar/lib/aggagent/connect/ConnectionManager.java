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
import org.cougaar.lib.aggagent.XMLParseCommon;

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
                             NameService myNameServiceProvider)
    {
         myConnectionLogger = c;

         try{
             Document doc = ConfigFileFinder.parseXMLConfigFile(configXMLFileName);
             Element root = doc.getDocumentElement();
             parseXMLConfigFile(doc, myNameServiceProvider);
             /**
             InputSource is = new InputSource(new FileInputStream( configXMLFile ));
             DOMParser domp = new DOMParser();
             domp.setErrorHandler(new DefaultHandler());
             domp.parse(is);
             Document doc = domp.getDocument();
             **/

         } catch (Exception ex ){
             ex.printStackTrace();
         }
    }



    private void parseXMLConfigFile( Document doc, NameService myNameServiceProvider)
    {
        NodeList nl = doc.getElementsByTagName("source");

        System.out.println("Number of Source Entries=" + nl.getLength() );
        int size = nl.getLength();
        for(int i = 0; i< size; i++)
        {
            Node n = (Node)nl.item(i);

            String ignored = XMLParseCommon.getAttributeOrChildNodeValue("ignore", n);
            if( ignored == null) ignored = "";
            
            if( false==(ignored.toLowerCase().indexOf("true")> -1) )
            {
                 // Only process this Node if not turned off...
                 // ie <source ... ignore="true">
                 parseXMLConfigNode(n, myNameServiceProvider);
            }

        }
    }
    private void parseXMLConfigNode(Node n, NameService myNameServiceProvider)
    {
            String name = XMLParseCommon.getAttributeOrChildNodeValue("name", n);
            URL cluster_url = null;
            URL url = null;
            URL base_url =  null;
            base_url = myNameServiceProvider.lookupURL(name);
            if( base_url != null ){
               try{
                  cluster_url = new URL( base_url.toExternalForm() ); //  + "/$" + name.toUpperCase() + "/");
                  //System.out.println("+++++++++++++++++++++++++++++++++>" + cluster_url.toExternalForm() );
               } catch( java.net.MalformedURLException ex ){
                  ex.printStackTrace();
               }
            }

            String pspquery = XMLParseCommon.getAttributeOrChildNodeValue("pspquery", n);
            if( pspquery == null) pspquery = "";

            // if can't find name, see if <url> field to use explicitly
            if( cluster_url == null)
            {
                String urlpath = XMLParseCommon.getAttributeOrChildNodeValue("url", n);
                if( urlpath != null) {
                   try{
                      url = new URL(urlpath);
                   } catch (Exception ex){ ex.printStackTrace(); }
                }
            } else
            {
                try{
                    url = new URL(cluster_url.toExternalForm() + pspquery );
                    //System.out.println("+++++++++++++++++++++++++++++++++>>>" + url.toExternalForm() );
                } catch (Exception ex ) { ex.printStackTrace(); }
            }

            String externalform = null;
            if( url != null ) {
                 externalform = url.toExternalForm();
                 this.addURL(url);
                 /**
                 myConnectionLogger.log("<Font color=red>SOURCE from configuration file</font>: "
                                 + name + ", url="
                                 + externalform + pspquery);
                 **/
                 //
                 // url is in case of where derived (no url element in config) is derived
                 // from pspquery
                 //
                 System.out.println("\t[ConnectionManager.parseXMLConfigFile()] Soruce name="
                                 + name + ", url.externalform=["
                                 + externalform
                                 + "], pspquery=" + pspquery);
            } else {
                 myConnectionLogger.log("<Font color=red>SOURCE FAILED TO LOAD from configuration file </font>."
                                 + "<FONT SIZE=-2 COLOR=BLUE><UL><LI>name=" + name + "<LI> url="
                                 + externalform
                                 + "</font></UL>");
                 System.out.println("\t[parseXMLConfigFile] Soruce failed to load: name=" + name + ", url="
                                 + externalform + pspquery);
            }
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
    // @param : recycle=true, relaunch any dead connections
    //                  NOTE, keep alive connecitons don't die.
    // @return : void
    public synchronized void  connectAllAsynchronous(){

         myConnectionLogger.log("Call: connectAllAsynchronous");
         synchronized( urlSpaces )
         {
             List space_set = (List)urlSpaces.get(this);
             if( space_set == null ) {
                 System.err.println("[ConnectionManger.connectAllAsynchronous()] NO URL SPACE found!");
                 System.err.println("[ConnectionManger.connectAllAsynchronous()] Be sure all URLs are ACTIVEZ");
                 myConnectionLogger.log(
                     "<Font color=red>NO URL SPACE found in ConnectionManager.connectAllAsynchronous().</font>"
                                );
             }
             synchronized( space_set )
             {
                 Iterator it = space_set.iterator();
                 while(it.hasNext()){
                    URLConnexionProbe u = (URLConnexionProbe)it.next();
                    //URLConnexionProbe ucg = new URLConnexionProbeAdapter(u);
                    ConnectionListener con =
                         new ConnectionListener(
                             u, myConnectionLogger, getNamespace(),
                             deadConnections, dataPool);
                    con.start();
                    //System.out.println("[ConnectionManager.connectAllAsynchronous()] " + con.describeAll());
                 }
             }
         }
    }

    //
    // @param : recycle=true, relaunch any dead connections
    //                  NOTE, keep alive connecitons don't die.
    //
    // @return : data harvested from dataPool
    public List cycleAllAsynchronousConnections(boolean recycle)
    {
         List returnData = null;
         returnData = Collections.synchronizedList(new ArrayList(dataPool));;
         synchronized( dataPool )
         {
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
                            cl.start();
                       }
                    }
               } catch (Exception ex) {
                       ex.printStackTrace();
               }
        }
        return returnData;
    }


   public static void main(String[] args) {
        System.out.println("[ConnectionManager.main() called...]");
        String configXMLFileName = null;

        //NameService myNameServiceProvider = new ProxyMapAdapter( new FDSProxy() );
        // wont work without node

        ConnectionLogger logger = new ConnectionLogger();
        ConnectionManager cm = new ConnectionManager(logger, "aggregator.configs.xml", null);

        try{
           cm.addURL( new URL("http://www.bbn.com") );
           cm.addURL( new URL("http://www.yahoo.com") );
           cm.addURL( new URL("http://www.lycos.com") );
           cm.connectAllAsynchronous();
        } catch (Exception ex ){
           ex.printStackTrace();
        }
        while( cm.numLiveConnections() > 0)
        {
           try{
              Thread.sleep(3000);
           } catch (Exception ex )
           {
              ex.printStackTrace();
           }
           List data = cm.cycleAllAsynchronousConnections(false);
           Iterator it = data.iterator();
           while( it.hasNext() ){
               String s = (String)it.next();
               System.out.println("###############" + s.substring(0,80));
           }
        }

        cm.flush();
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
    //private synchronized Iterator iterateURLs() {
    //    List space_set = (List)urlSpaces.get(this);
    //    return space_set.iterator();
    //}
}
