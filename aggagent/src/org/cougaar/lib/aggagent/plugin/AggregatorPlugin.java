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
package org.cougaar.lib.aggagent.plugin;

import org.cougaar.core.naming.NamingService;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Map;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;

import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.PlugInDelegate;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.core.plugin.SimplifiedPlugIn;
import org.cougaar.lib.planserver.server.FDSProxy;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.lib.planserver.server.ProxyMapAdapter;

import org.cougaar.lib.aggagent.Configs;
import org.cougaar.lib.aggagent.connect.ConnectionManager;
import org.cougaar.lib.aggagent.connect.ConnectionLogger;
import org.cougaar.lib.aggagent.ldm.*;


public class AggregatorPlugin extends SimplifiedPlugIn
{
    private ConnectionManager myConnectionManager = null;

    public long INITIAL_WAKE_UP_INTERVAL = 10000;

    // DEFAULT POLL RATE IN miliseconds
    // Plan is to have poll rates settable on individual queries
    public long POLL_INTERVAL = 5000;


    /**
         NO SUBSCRIPTIONS FOR NOW

    private IncrementalSubscription mySubscription;
    private UnaryPredicate myPredicate;
    **/
    private String ConfigFileName = null;

    public void setupSubscriptions() {
        try{
           /**
               NO SUBSCRIPTIONS FOR NOW

           Class klass = Class.forName("alp.ui.aggserver.ldm.Task");
           myPredicate = this.getInstanceOfPredicate(klass);
           mySubscription = (IncrementalSubscription)subscribe(myPredicate);
           **/
           ConfigFileName = getStringParameter("file=", getParameters().elements(),
                                  Configs.AGGREGATOR_DEFAULT_CONNECTION_CONFIG_FILE_NAME);

           String def_poll_rate = getStringParameter("default_poll_rate=", getParameters().elements(),
                                  "5000");

           if( def_poll_rate != null) {
               long value = Long.parseLong(def_poll_rate);
               System.out.println("\nAggregatorPlugin setting Default Poll Rate to: " + value + "\n");
               POLL_INTERVAL = value;
           }

          //
          // Cant do this here, apparently hookup to society (nameserver) not ready
          //
          //NameService myNameServiceProvider = new ProxyMapAdapter( new FDSProxy() );
          //myConnectionManager = new ConnectionManager(
          //        new ConnectionLogger(), ConfigFileName, myNameServiceProvider);

          // force execute and initialize connection manager.
          this.wakeAfter(INITIAL_WAKE_UP_INTERVAL);

        } catch (Exception ex ) {
           ex.printStackTrace();
        }
    }

    private boolean first_time = true;
    private DOMParser myDOMParser = new DOMParser();   // reuse parser instance...



   /**
     * The Naming Service reference is bound and accessed here.
     */
    private NamingService theNamingService;

    public void setNamingService(NamingService aNamingService) {
       theNamingService = aNamingService;
    }

    public NamingService getNamingService() {
       return theNamingService;
    }


   /**
    * Execute
    */
    public void execute() {
        /**
         *  the use of first_time in execute vs. moving this logic into
         *  subscription setup is motivated by finding that can *MORE EASILY*
         *  get into race in subscption setup .... the FDS registry is not fully
         *  populated at time we query.
         *
         *  Note we're not entirely safe from race possibility down here.  Far
         *  better at some point to periodically re-check registry....
         */
        if( first_time ){
           /**
            * Obtain URLs to other Clusters, so we can query their Generic PSP
            */
           FDSProxy fds = new FDSProxy(theNamingService);
           NameService myNameServiceProvider = new ProxyMapAdapter( fds );
           myConnectionManager = new ConnectionManager(
                   new ConnectionLogger("connxion" + this.getClusterIdentifier().cleanToString() + ".html"),
                   ConfigFileName,
                   myNameServiceProvider);
           myConnectionManager.connectAllAsynchronous();
           first_time = false;
        }

        //
        // visit all connections, HARVEST DATA, and recycle any which
        // may have failed and or are down (ie. non keep-alive)
        //
        List data = myConnectionManager.cycleAllAsynchronousConnections(true);
        System.out.println("[AggregatorPlugin] total data elements from Conn Mgr=" + data.size());
        Iterator it = data.iterator();
        while (it.hasNext() ) {
            Object obj = it.next();
            StringBuffer str_buf = null;
            if( obj instanceof StringBuffer ) {
                 str_buf = ((StringBuffer)obj);
            }


            /**
            int substr_index =-1;
            if( str.length() > 6 ) {
               substr_index = str.substring(0,5).toLowerCase().indexOf(" <?xml");
            }

            if( substr_index > -1 )
            {
               System.out.println("Is XML");
            **/

            if( isHTML(str_buf) ) {
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // ASSUME HTML! - GO TO IT!
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                processDataFromConnection_asHTML(str_buf);
            }
            if( isXML(str_buf) ) {
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // XML! - GO TO IT!
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                processDataFromConnection_asXML(str_buf.toString());
            }
            else {
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                // LAST TRY ASSUME HTML - GO TO IT!
                // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                processDataFromConnection_asHTML(str_buf);
            }

            System.out.println("[AggregatorPlugin] data len=" + str_buf.length());
            //System.out.println("[AggregatorPlugin] data=" + str_buf + "\n\n");
        }

        this.wakeAfter(POLL_INTERVAL);
    }

    private boolean isHTML(StringBuffer data){
        if( data != null) {
             if( data.length() >= 10 ){
                  int dst_len = 10;
                  char[] dst = new char[dst_len];
                  data.getChars(0,dst_len,dst,0);
                  String str = new String(dst);
                  if( str.toLowerCase().trim().startsWith("<html") == true) {
                       return true;
                  }
             }
        }
        return false;
    }
    private boolean isXML(StringBuffer data){
        if( data != null) {
             if( data.length() >= 10 ){
                  int dst_len = 10;
                  char[] dst = new char[dst_len];
                  data.getChars(0,dst_len,dst,0);
                  String str = new String(dst);
                  if( str.toLowerCase().trim().indexOf("xml") >= 0)
                       return true;
             }
        }
        return false;
    }

    /**
     *  Handle a complete response from one of the connections, presuming the
     *  contents to be formatted as an HTML document.  In this implementation,
     *  an HTMLPlanObject is created to house the HTML text and is then
     *  published to the logplan.
     *
     *  @param strbuffer a StringBuffer containing the HTML text
     */
    protected void processDataFromConnection_asHTML(StringBuffer strbuffer)
    {
            HTMLPlanObject hp = new HTMLPlanObject(strbuffer);
            publishAdd(hp);
    }

    /**
     *  Handle a complete response from one of the connections, presuming the
     *  contents to be formatted as an XML document.  In this implementation,
     *  the XML is parsed to form a DOM, which is then encapsulated with a
     *  PlanObject instance and published on the logplan.
     *
     *  @param str a String containing the XML document
     */
    protected void processDataFromConnection_asXML(String str)
    {
            try{
                    //ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
                    //InputSource is = new InputSource(bais);
                    StringReader sr = new StringReader(str.trim());
                    InputSource is = new InputSource(sr);

                    //System.err.println("str=" + str);
                    //FileReader freader = new FileReader("task.xml");
                    //InputSource is = new InputSource(freader);

                    //myDOMParser = new DOMParser();

                    myDOMParser.setErrorHandler(new ErrorHandler(){
                          public void error(SAXParseException exception) {
                             System.err.println("[ErrorHandler.error]: " + exception);
                           }
                           public void fatalError(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.fatalError]: " + exception);
                           }
                           public void warning(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.warning]: " + exception);
                            }
                        }
                    );
                    //
                    // .reset() DISABLED:
                    // USE seems to occasionally corrupt document created.
                    //myDOMParser.reset();
                    myDOMParser.parse(is);
                    Document doc = myDOMParser.getDocument();

                    System.out.println("Is XML");

                    if( doc != null ) {
                       PlanObject p = new PlanObject(doc);
                       System.out.println("[Aggregator Plugin] Pub PlanObject, PlanObject Root doc elem="
                                          + doc.getDocumentElement().getNodeName());
                       publishAdd(p);
                    }
            } catch ( SAXParseException sax) {
                    //
                    // if we see this, assume this is not valid XML
                    //
                    System.out.println("Not XML");
                    System.out.println("\n[AggregatorPlugin] data=" + str + "\n");
            } catch (Exception ex ){
                    ex.printStackTrace();
            }
    }


  /**
   * Return a String parameter from the head of a list of plugin parameters
   * @param Enumeration of Plugin command line parameters
   * @param integer default value if no numeric value found
   * @return int value parsed from first numeric argument
   */
  public static String getStringParameter(String prefix, Enumeration parameters,
                                          String default_value)
  {
    while(parameters.hasMoreElements()) {
      String param = (String)parameters.nextElement();
      if (param.startsWith(prefix)) {
        String sVal = param.substring(prefix.length()).trim();
        return sVal;
      }
    }
    return default_value;
  }

    public UnaryPredicate getInstanceOfPredicate(final Class klass ) {
         UnaryPredicate pred = new UnaryPredicate() {
                 public boolean execute(Object o) {
                     if (klass.isInstance(o)) {
                        return true;
                     }
                     return false;
                }
         };
         return pred;
    }
}


