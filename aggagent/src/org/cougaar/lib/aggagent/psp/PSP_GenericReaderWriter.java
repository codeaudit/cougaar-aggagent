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



public class PSP_GenericReaderWriter extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber
{
   // null cluster_id = load all...
   //private GLDictionary myGLDictionary = null;

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
     Subscription subscription = psc.getServerPluginSupport().subscribe(this, queryObj.getPredicate());
     Collection container = ((CollectionSubscription)subscription).getCollection();   // Doesn't block
     synchronizedExecute (out, query_parameters, psc, psu, my_gl_dict, container, queryObj);
  }

  // ################################################################################
  public final void synchronizedExecute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      GLDictionary my_gl_dict,
      Collection container,
      GenericQuery queryObj
      ) throws Exception
  {
      System.out.println("[PSP_GenericReaderWriter] +++> start execution @" + psc.getServerPluginSupport().getClusterIDAsString() );

      boolean useHTML =
         (boolean)query_parameters.existsParameter("HTML");

      Vector v = query_parameters.getParameterTokens("TELLME", '=');
      if( v != null ){
           out.println("<HTML><BODY><H3>Generic Reader/Writer PSP TELL</H3>");
           out.println("Dictionary=" + my_gl_dict.toHTMLString());
           out.println("</BODY></HTML>");
           return;
      }
      /**
      v = query_parameters.getParameterTokens("XSL_ALIAS", '=');
      String myXSL_Alias = null;
      if( v != null ){
           myXSL_Alias=(String)v.get(0);
           System.out.println("XSL_ALIAS=" + myXSL_Alias);
      }
      **/

      //GenericQuery queryObj =
      //          (GenericQuery)my_gl_dict.match(query_parameters,
      //                                              my_gl_dict.MATCH_MODE_QUERY);

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

          //Subscription subscription = psc.getServerPluginSupport().subscribe(this, queryObj.getPredicate());
          //Collection container = ((CollectionSubscription)subscription).getCollection();

          queryObj.execute(container);

          queryObj.returnVal(printOut ); // my_gl_dict.getXSL(myXSL_Alias));

          if(useHTML ) {

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
 // ################################################################################
 protected StringBuffer filterXMLtoHTML(StringBuffer dataout)
  {
       //int srcend = dataout.length();
       //char csrc[] = new char[srcend];
       //dataout.getChars(0,srcend,csrc,0);

       //StringWriter sw = new StringWriter(srcend);
       StringBuffer buf = new StringBuffer();

       int depth=0;
       int i;
       int sz = dataout.length();
       char prev_c = (char)-1;
       char prev_prev_c = (char)-1;
       char c = (char)-1;
       for(i=0;i<sz;i++){
           prev_prev_c = prev_c;
           prev_c = c;
           c = dataout.charAt(i);
           if( c == '<' ) {
                buf.append("&lt"); //System.out.print(" &lt ");
                depth++;
           }
           else if( (c == '>') && (prev_c == '/') ) {
               buf.append(c);
              depth--;
           }
           else if( c == '>' ) {
                buf.append("&gt");  //System.out.print(" &gt ");
                buf.append("\n");
                for(int j=0; j<depth; j++){
                   buf.append("  " );
                }

           }
           else if( (c == '/') && (prev_c == '<') ) {
               buf.append(c);
               depth--;
               depth--;
           }
           else {
               buf.append(c); //System.out.print(c);
           }

       }
       return buf;
  }
**/


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


  public void subscriptionChanged(Subscription subscription) {
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
  /**
  private String filterXMLtoHTML(String dataout)
  {
       int srcend = dataout.length();
       char csrc[] = new char[srcend];
       dataout.getChars(0,srcend,csrc,0);

       StringWriter sw = new StringWriter(srcend);

       int i;
       int sz = dataout.length();
       for(i=0;i<sz;i++){
           char c = csrc[i];
           if( c == '<' ) sw.write("&lt");
           else if( c == '>' ) sw.write("&gt");
           else sw.write(c);
       }
       return sw.toString();
  }
  **/
   /**
   private void ___execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu ) throws Exception
  {

    String value =
      (String)query_parameters.getGETUrlString();


    System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% value="+value);

    char buf[] =    query_parameters.getPostData();

    String post = null;
    if( buf != null )
    {
        post = new String(buf);
        System.out.println("POST=" + post);
    }

    if( post.length() > 0)
    {
        /// PUBLISH TASK FOR XML DATA SOURCE
        /// TELL USER VIA HTML
        out.println("<HTML><BODY>\n");
        out.println("<P>Pubishing XML data source: <P><PRE>"
                               + GLDictionary.filterXMLtoHTML(new StringBuffer(post)).toString()
                               + "</PRE></P>");
        out.println("</BODY></HTML>");

        post = filterNonXML(post);
        PlugInDelegate pd = psc.getServerPluginSupport().getDirectDelegate();


    }
    else {
        /// MUST BE REQUEST FOR USER TO POST XML DATA

           out.println("<HTML><BODY>");
           out.println("<P>Upload XML file into PSP_ApplyXSL.</P>");
           out.println("<form  enctype=\"multipart/form-data\" method=\"post\"> ");
           out.println("<TABLE WIDTH=\"100%\">");
           out.println("<TR>");
           out.println("<TD ALIGN=\"RIGHT\" VALIGN=\"TOP\">Filename:</TD>");

           out.println("<TD ALIGN=\"LEFT\"><INPUT TYPE=\"FILE\" NAME=\"FILE1\" >");
           out.println("</TD>");
           out.println("</TR>");
           out.println("<TR>");
           out.println("<TD ALIGN=\"RIGHT\">&nbsp;</TD>");
           out.println("<TD ALIGN=\"LEFT\"><INPUT TYPE=\"SUBMIT\" NAME=\"SUB1\" VALUE=\"Upload File\"></TD>");
           out.println("</TR>");
           out.println("</TABLE></BODY></HTML>");
     }
  }
  **/

}
