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
import java.util.HashMap;

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
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;

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


import org.cougaar.lib.aggagent.xml.XMLParseCommon;
import org.cougaar.lib.aggagent.xml.HTMLize;
import org.cougaar.lib.aggagent.dictionary.GLDictionary;
import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;
import org.cougaar.lib.aggagent.dictionary.GenericLogic;



public class PSP_GenericReaderWriter extends PSP_BaseAdapter implements PlanServiceProvider //, UISubscriber
{
  // QUERY SESSION IMPLEMENTATION DEFS
  private HashMap myQuerySessions = new HashMap();

  // Enumeration of Subscription Events
  public final int QUERY_SESSION_ADD_UPDATES    =1;
  public final int QUERY_SESSION_CHANGE_UPDATES =2;
  public final int QUERY_SESSION_REMOVE_UPDATES =3;


   public UnaryPredicate getAllPredicate() {
         UnaryPredicate pred = new UnaryPredicate() {
                 public boolean execute(Object o) {
                     return true;
                }
         };
         return pred;
   }

  /**
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_GenericReaderWriter()
  {
    super();
  }

  public PSP_GenericReaderWriter(String pkg, String id)
    throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

  // ################################################################################
  protected GLDictionary getGLDictionary( PlanServiceContext psc, PlanServiceUtilities psu)
      {
      Object entry = psu.getBlackBoard().getEntry("GLDICTIONARY", psc.getSessionAddress());
      GLDictionary myGLDictionary = null;
      if( entry == null ) {
          myGLDictionary = new GLDictionary( psc.getServerPluginSupport().getClusterIDAsString());
          //
          // Important - using LocalAddress() for hash of GLDictionary, dictionary should
          // be shared across all sessions.
          //
          psu.getBlackBoard().addEntry("GLDICTIONARY", psc.getLocalAddress(), myGLDictionary);
      } else {
          myGLDictionary = (GLDictionary)entry;
      }
      return myGLDictionary;
  }

  // ################################################################################
  public final void execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu ) throws Exception
  {
      GLDictionary myGLDictionary = getGLDictionary(psc, psu);

      synchronized( myGLDictionary ){
          GenericQuery g_query =
                (GenericQuery)myGLDictionary.match(query_parameters,
                                                    myGLDictionary.MATCH_MODE_QUERY);

          if( g_query == null ) {
              out.println("<HTML><BODY><H3>Cataloged GenericLogic Entries in Dictionary @"
                         + psc.getServerPluginSupport().getClusterIDAsString() + "</H3>");
              out.println("<P>Dictionary size=" + myGLDictionary.getNumGLEntries() + "</P>");
              out.println("<BR>" + myGLDictionary.toHTMLString());
              out.println("</HTML>");
          }
          else {
             /**
             Subscription subscription = psc.getServerPluginSupport().subscribe(this, g_query.getPredicate());
             Collection container = ((CollectionSubscription)subscription).getCollection();   // Doesn't block
             **/
             this._execute (out, query_parameters, psc, psu, myGLDictionary, g_query);
          }
      }

  }
  // ################################################################################
  // execute() => _execute() => synchronizedExecute()
  // execute() + synchronizedExecute() are FINAL and shared between this PSP and other
  // PSPs which subclass this one for core implementation (eg. _KeepAlive)
  // PSP_GenericReaderWriter_KeepAlive overloads _execute() for specialized behavior
  //
  public void _execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      GLDictionary my_gl_dict,
      GenericQuery queryObj
      ) throws Exception
  {
     //  If 'QUERYSESSION=ID' defined
     //  Look up QuerySessionUISubscriber by ID
     //       If it doesn't exist then create one with predicate.
     //  If have QuerySessionUISubscriber, check the grabUpdates() for updates.
     //  If so, act on 'em.
     //
     // @return List of updates from Query Session.
     //    Updates with respect to last time checked (via this method)
     //    If returns null then QuerySession is not defined.
     //
     // public List  getQuerySessionUpdates(String query_session_id);
     // public boolean instantiateQuerySession(
     //                                     String query_session_id);
     Vector v = query_parameters.getParameterTokens("QUERYSESSION", '=');
     String qsession_id=null;
     if( v != null ){
           qsession_id =(String)v.get(0);
           System.out.println("QUERYSESSION=" + qsession_id);
      }
      if( qsession_id != null ){
            //
            // Query Session query -- test for query results state
            //
            boolean isSession = existsMyQuerySession(qsession_id);

            List add_container=  new ArrayList();
            List remove_container=  new ArrayList();
            List change_container=  new ArrayList();

            if( isSession == false)
            {
                 // set it up -- we'll poll later for updates.
                  QuerySessionUISubscriber qsession_subscriber = this.instantiateQuerySession(qsession_id);
                  Subscription subscription = psc.getServerPluginSupport().subscribe(qsession_subscriber, queryObj.getPredicate());

                 //#######################################################
                 psc.getServerPlugInSupport().openLogPlanTransaction();
                 this.grabSubscriptionDeltas(subscription, add_container, change_container, remove_container);
                 psc.getServerPluginSupport().closeLogPlanTransaction();
                 //#######################################################

                  //
                  // don't process container directly here!  - because contents will
                  // show up at QuerySession container -- so we get duplicate entries.
                  // we'll be handling these objects from QuerySession instance
                  //
                  //container =((CollectionSubscription)subscription).getCollection();   // Doesn't block

            }
            else {
                 this.getQuerySessionUpdates(qsession_id, QUERY_SESSION_ADD_UPDATES, add_container);
                 this.getQuerySessionUpdates(qsession_id, QUERY_SESSION_REMOVE_UPDATES, remove_container);
                 this.getQuerySessionUpdates(qsession_id, QUERY_SESSION_CHANGE_UPDATES, change_container);
            }

            if( isSession == true ) {
                int totalchanges =   add_container.size() + remove_container.size() + change_container.size();
                if( totalchanges > 0)
                {
                     synchronizedExecute(
                           out,
                           query_parameters,
                           psc,
                           psu,
                           my_gl_dict,
                           add_container,
                           change_container,
                           remove_container,
                           queryObj);
                }
            }
      }
      else {
           //
           // normal poll query -- no state
           //
           IncrementalSubscription subscription =
               (IncrementalSubscription)psc.getServerPluginSupport().getDirectDelegate().subscribe(queryObj.getPredicate());
           //Collection container = ((CollectionSubscription)subscription).getCollection();   // Doesn't block


           List add_container = new ArrayList();
           List change_container = new ArrayList();
           List remove_container = new ArrayList();

           //#######################################################
           psc.getServerPlugInSupport().openLogPlanTransaction();
           this.grabSubscriptionDeltas(subscription, add_container, change_container, remove_container);
           psc.getServerPluginSupport().closeLogPlanTransaction();
           //#######################################################


           //#######################################################
           /**
           psc.getServerPlugInSupport().openLogPlanTransaction();

           Enumeration en = subscription.getAddedList();
           while( en.hasMoreElements() ) {
               add_container.add(en.nextElement());
           }

           en = subscription.getChangedList();
           while( en.hasMoreElements() ) {
               change_container.add(en.nextElement());
           }

           en = subscription.getRemovedList();
           while( en.hasMoreElements() ) {
               System.out.println("RECEIVED REMOVE ITEM");
               Thread.dumpStack();
               remove_container.add(en.nextElement());
           }
           psc.getServerPluginSupport().closeLogPlanTransaction();
           **/
           //#######################################################

           synchronizedExecute (
                           out, query_parameters, psc, psu, my_gl_dict,
                           add_container,
                           change_container,
                           remove_container,
                           queryObj);
      }
  }


  // ################################################################################
  public final void synchronizedExecute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      GLDictionary my_gl_dict,
      Collection add_container,
      Collection change_container,
      Collection remove_container,
      GenericQuery queryObj
      ) throws Exception
  {
      boolean useHTML =
         (boolean)query_parameters.existsParameter("HTML");

      boolean tellME =
         (boolean)query_parameters.existsParameter("TELLME");

      System.out.println("[PSP_GenericReaderWriter] +++> start execution @"
                         + psc.getServerPluginSupport().getClusterIDAsString()
                         + " useHTML=" + useHTML
                         );


      if(tellME ){
           out.println("<HTML><BODY><H3>Generic Reader/Writer PSP TELL</H3>");
           out.println("Dictionary=" + my_gl_dict.toHTMLString());
           out.println("</BODY></HTML>");
           return;
      }


      if( queryObj != null )
      {
          ByteArrayOutputStream bufOut = null;
          PrintStream printOut = null;
          if( useHTML ) {
              bufOut = new ByteArrayOutputStream(512);
              printOut = new PrintStream(bufOut);
          }
          else printOut = out;

          System.out.println("[PSP_GenericReaderWriter] using GenericQuery instance="
                             + queryObj
                             + " add_container.size=" + add_container.size()
                             + " change_container.size=" + change_container.size()
                             + " remove_container.size=" + remove_container.size()
                             );

          //Subscription subscription = psc.getServerPluginSupport().subscribe(this, queryObj.getPredicate());
          //Collection container = ((CollectionSubscription)subscription).getCollection();

          queryObj.execute(add_container, GenericLogic.collectionType_ADD);
          queryObj.returnVal(printOut );

          queryObj.execute(change_container, GenericLogic.collectionType_CHANGE);
          queryObj.returnVal(printOut );

          queryObj.execute(remove_container, GenericLogic.collectionType_REMOVE);
          queryObj.returnVal(printOut );

          if(useHTML )
          {
              StringBuffer sb = HTMLize.layoutXML(
                   new StringBuffer(new String(bufOut.toByteArray())),
                   new HashMap(),
                   true
                   );

              out.print("<HTML><BODY><PRE><BLOCKQUOTE>" +
                       sb.toString()
                       + "</BLOCKQUOTE></PRE></BODY></HTML>"
                   );
         }
      }
      out.flush();
      System.out.println("[PSP_GenericReaderWriter] <+++ leave execution @" + psc.getServerPluginSupport().getClusterIDAsString() );
  }



  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  /**  Any PlanServiceProvider must be able to provide DTD of its
   *  output IFF it is an XML PSP... ie.  returnsXML() == true;
   *  or return null
   **/
  public String getDTD()  {
    return null;
  }


  /**
   *
   * @param subscription Subscription
   */
  //public void subscriptionChanged(Subscription subscription ){
  //}

  public void grabSubscriptionDeltas(Subscription subscription,
                             List add_container, List change_container, List remove_container) {
      Enumeration e = ((IncrementalSubscription)subscription).getAddedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         add_container.add(obj);
      }
      e = ((IncrementalSubscription)subscription).getChangedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         change_container.add(obj);
      }
      e = ((IncrementalSubscription)subscription).getRemovedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         remove_container.add(obj);
      }
  }

  /**
	public  void applyXSL(String xml, String xsl)
    throws java.io.IOException,
           java.net.MalformedURLException,
           org.xml.sax.SAXException
	{
    // Create an XSLT processor.
    XSLTProcessor processor = XSLTProcessorFactory.getProcessor();

    // Create input source documents.
    XSLTInputSource xmlID = new XSLTInputSource("foo.xml");
    XSLTInputSource stylesheetID = new XSLTInputSource("foo.xsl");

    // Create a DOM Document node to attach the result nodes to.
    Document out = new org.apache.xerces.dom.DocumentImpl();
    XSLTResultTarget resultTarget = new XSLTResultTarget(out);

    // Process the source tree and produce the result tree.
    processor.process(xmlID, stylesheetID, resultTarget);

    // Use the FormatterToXML and TreeWalker to print the DOM to System.out
    // Note: Not yet sure how to get the Xerces Serializer to  handle
    // arbitrary nodes.
    FormatterToXML fl = new FormatterToXML(System.out);
    TreeWalker tw = new TreeWalker(fl);
    tw.traverse(out);
	}
  **/

   // transform '<', '>' to html
  private String filterNonXML( String dataout){
       int srcend = dataout.length();
       char csrc[] = new char[srcend];
       dataout.getChars(0,srcend,csrc,0);
       int start=0;
       int close=srcend-1;
       for(int i=0; i< srcend; i++)
       {
           char c = csrc[i];
           if( c == '<' ){
              start = i;
              break;
           }
       }
       for(int i=srcend-1; i>=0; i--)
       {
           char c = csrc[i];
           if( c == '>' ){
              close = i;
              break;
           }
       }
       return dataout.substring(start,close+1);
  }

  //##########################################################################


  public boolean existsMyQuerySession(String query_session_id) {
       return myQuerySessions.get(query_session_id) != null;
  }

  // @return Add List of updates from Query Session.
  //    Updates with respect to last time checked (via this method)
  //    If returns null then QuerySession is not defined.
  //
  public List  getQuerySessionUpdates(String query_session_id, int mode, List updates) {
       synchronized(myQuerySessions){
            QuerySessionUISubscriber subscriber = (QuerySessionUISubscriber)myQuerySessions.get(query_session_id);
            if( subscriber != null ){
                 synchronized( subscriber )
                 {
                    if( mode == QUERY_SESSION_ADD_UPDATES )
                         subscriber.grabAddUpdates(updates);
                    else if( mode == QUERY_SESSION_CHANGE_UPDATES )
                         subscriber.grabChangeUpdates(updates);
                    else if( mode == QUERY_SESSION_REMOVE_UPDATES )
                         subscriber.grabRemoveUpdates(updates);
                    return updates;
                 }
            }
       }
       return null;
  }

  public QuerySessionUISubscriber instantiateQuerySession( String query_session_id)
  {
      QuerySessionUISubscriber qsession = null;
      synchronized( myQuerySessions ) {
           qsession = new QuerySessionUISubscriber(query_session_id);
           myQuerySessions.put(query_session_id,qsession);
      }
      return qsession;
  }

}
