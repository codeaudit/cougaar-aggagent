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
import java.util.ArrayList;
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
  // private Vector myIncomingItems = new Vector();
  private List myAddContainer=  new ArrayList();
  private List myRemoveContainer=  new ArrayList();
  private List myChangeContainer=  new ArrayList();


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

  private static String delimiter = "&&&";

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
      // System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] ENTER!!!");

      IncrementalSubscription subscription =
            (IncrementalSubscription)psc.getServerPluginSupport().getDirectDelegate().subscribe(queryObj.getPredicate()); //.subscribe(this, queryObj.getPredicate());
      //Collection subContainer = ((CollectionSubscription)subscription).getCollection();   // Doesn't block

      //#######################################################
      psc.getServerPlugInSupport().openLogPlanTransaction();
      this.subscriptionChanged(subscription);
      psc.getServerPluginSupport().closeLogPlanTransaction();
      //#######################################################


      while(true) {
         if( (myAddContainer.size() > 0) || (myRemoveContainer.size()>0) || (myChangeContainer.size()>0) ) {
             synchronizedExecute (out, query_parameters, psc, psu, my_gl_dict,
                           myAddContainer,
                           myChangeContainer,
                           myRemoveContainer,
                           queryObj);

             // System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] SYNCHEXECUTE!!!" +
             //                    " myAddContainer.size=" + myAddContainer.size() +
             //                     " myChangeContainer.size=" + myChangeContainer.size() +
             //                     " myRemoveContainer.size=" + myRemoveContainer.size()
             //                    );
            // remove all objects - they have been handled
             myAddContainer.clear();
             myChangeContainer.clear();
             myRemoveContainer.clear();

             out.println(delimiter);
             out.flush();
             // System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] FLUSH!!!");
         }
         waitUntilNextItem();  // => waits if nothing to process...
         //container.add(obj);
         //System.out.println("[PSP_GenericReaderWriter.KeepAlive._execute()] CONTAINER ADD!!!");
      } // end - while()
  }

  /**
   * addAlert - Adds the given alert to the alert list. Calls notifyAll()
   * to wake up blocked nextElement().
   *
   * @param alert Alert to add to list
   *
   */
  synchronized public void addAddItem(Object obj) {
      //myIncomingItems.addElement(obj);
      myAddContainer.add(obj);
      notifyAll();
  }
  synchronized public void addRemoveItem(Object obj) {
      myRemoveContainer.add(obj);
      notifyAll();
  }
  synchronized public void addChangeItem(Object obj) {
      myChangeContainer.add(obj);
      notifyAll();
  }

  /**
   * nextAlert - returns the next alert on the list. If no alerts and wait is true,
   * suspends thread until an alert is added. Otherwise returns null.
   *
   * @return alert Alert next alert on the list.
   */
  synchronized public void waitUntilNextItem()
  {
    boolean wait = true;
    while( (myAddContainer.size() == 0) && (myChangeContainer.size() == 0) && (myRemoveContainer.size()==0) ) {
      if (wait) {
        try {
          wait();
        } catch (InterruptedException ie) {
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        return;
      }
    }
  }


  /**
   *
   * @param subscription Subscription
   */
  public void subscriptionChanged(Subscription subscription) {
      Enumeration e = ((IncrementalSubscription)subscription).getAddedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addAddItem(obj);
      }
      e = ((IncrementalSubscription)subscription).getChangedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addChangeItem(obj);
      }
      e = ((IncrementalSubscription)subscription).getRemovedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addRemoveItem(obj);
      }
  }


}
