/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.lib.aggagent.psp;




import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;


import org.cougaar.core.plugin.PlugInDelegate;
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
import org.cougaar.lib.planserver.KeepAlive;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.lib.planserver.UseDirectSocketOutputStream;

import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import java.net.*;
import java.util.*;


import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;
import org.apache.xalan.xslt.XSLTProcessorFactory;

import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;
import org.apache.xalan.xpath.xml.FormatterToHTML;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;

import org.cougaar.lib.aggagent.dictionary.GLDictionary;
import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;


public class PSP_GenericReaderWriter_KeepAlive extends PSP_GenericReaderWriter
               implements  KeepAlive, UseDirectSocketOutputStream
{
  private Vector myIncomingItems = new Vector();


  public PSP_GenericReaderWriter_KeepAlive()
  {
      super();
      //
      // SET CONNECTION ACK MESSAGE TO NULL, IF NULL, NO ACK MESSAGE WILL BE
      // RETURNED TO CLIENT.  THIS WAY WE OVERRIDE DEFAULT BEHAVIOR FOR CONNECTION
      // MANAGER WHICH PERIODICALLY ACKS CLIENTS TO TEST FOR ORPHAHS.
      //
      this.setConnectionACKMessage(null);
  }

  // ################################################################################
  public void _execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      GLDictionary my_gl_dict,
      GenericQuery queryObj
      ) throws Exception
  {
      System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] ENTER!!!");

      Subscription subscription = psc.getServerPluginSupport().subscribe(this, queryObj.getPredicate());
      Collection container = ((CollectionSubscription)subscription).getCollection();   // Doesn't block

      while(true) {
         if( container.size() > 0 ) {
             synchronizedExecute (out, query_parameters, psc, psu, my_gl_dict, container, queryObj);
             System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] SYNCHEXECUTE!!! container.size=" + container.size());
             container.clear(); // remove all objects - they have been handled
             out.println("&&&");
             out.flush();
             System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] FLUSH!!!");
         }
         Object obj = nextItem(true);  // nextItem() => waits if nothing to process...
         container.add(obj);
         System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] CONTAINER ADD!!!");
      } // end - while()
  }

  /**
   * addAlert - Adds the given alert to the alert list. Calls notifyAll()
   * to wake up blocked nextElement().
   *
   * @param alert Alert to add to list
   *
   */
  synchronized public void addItem(Object obj) {
      myIncomingItems.addElement(obj);
      notifyAll();
  }

  /**
   * nextAlert - returns the next alert on the list. If no alerts and wait is true,
   * suspends thread until an alert is added. Otherwise returns null.
   *
   * @param wait boolean controls whether call blocks
   *
   * @return alert Alert next alert on the list.
   */
  synchronized public Object nextItem(boolean wait) {
    while (myIncomingItems.size() == 0) {
      if (wait) {
        try {
          wait();
        } catch (InterruptedException ie) {
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        return null;
      }
    }

    Object obj = myIncomingItems.firstElement();
    myIncomingItems.remove(0);
    return obj;
  }

  /**
   * subscriptionChanged - adds new subscriptions to myIncomingAlerts.
   *
   * @param subscription Subscription
   */
  public void subscriptionChanged(Subscription subscription) {
      Enumeration e = ((IncrementalSubscription)subscription).getAddedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addItem(obj);
      }
  }

  /**
  // ################################################################################
  public void synchronizedExecute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      GLDictionary my_gl_dict
      ) throws Exception
  {
 
      boolean useHTML =
         (boolean)query_parameters.existsParameter("HTML");


      System.out.println("[PSP_GenericReaderWriter_KeepAlive] +++> start execution @" + psc.getServerPluginSupport().getClusterIDAsString() );
      GLDictionary myGLDictionary = getGLDictionary(psc, psu);

      GenericQuery queryObj =
                (GenericQuery)myGLDictionary.match(query_parameters,
                                                    myGLDictionary.MATCH_MODE_QUERY);

      if( queryObj != null )
      {
          ByteArrayOutputStream bufOut = null;
          PrintStream printOut = null;
          if( useHTML ) {
              bufOut = new ByteArrayOutputStream(512);
              printOut = new PrintStream(bufOut);
          }
          else printOut = out;

          System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> myGLDictionary returned: " + queryObj);

          Subscription subscription = psc.getServerPluginSupport().subscribe(this, queryObj.getPredicate());
          Collection container = ((CollectionSubscription)subscription).getCollection();

          queryObj.execute(container);
             //   false:   write results to printOut as single document
             //   for keep-alive contexts, should dribble documents back individually
             //                    as objects come in (use true).
          queryObj.returnVal(printOut, null, false);

          if(useHTML ) {

           StringBuffer sb = filterXMLtoHTML(new StringBuffer(new String(bufOut.toByteArray())));

           out.print("<HTML><BODY><PRE><BLOCKQUOTE>" +
                       sb.toString()
                       + "</BLOCKQUOTE></PRE></BODY></HTML>"
                       );
         }
      }
      else {
           out.println("<HTML><BODY><H3>Cataloged GenericLogic Entries in Dictionary @"
                         + psc.getServerPluginSupport().getClusterIDAsString() + "</H3>");
           out.println("<P>Dictionary size=" + myGLDictionary.getNumGLEntries() + "</P>");
           out.println("<BR>" + myGLDictionary.toHTMLString());
           out.println("</HTML>");
      }
      out.flush();

      System.out.println("[PSP_GenericReaderWriter] <+++ leave execution @" + psc.getServerPluginSupport().getClusterIDAsString() );
  }
  **/


}
