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

import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.io.StringReader;
import java.util.Iterator;

import org.cougaar.core.plugin.PlugInDelegate;
import org.cougaar.util.Enumerator;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;

import org.cougaar.lib.aggagent.ldm.*;
import org.cougaar.lib.aggagent.Configs;

/**
  * Converts Config or other XML sources to HTML for display.
  **/

public class PSP_AggConfigReader extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber
{

   private String indent = "...........>" ;


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
  public PSP_AggConfigReader() {
    super();
  }

  public PSP_AggConfigReader(String pkg, String id)
    throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

    public void execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu ) throws Exception
  {

      String urlspec =
         (String)query_parameters.getFirstParameterToken("CONFIGURL", '=');

      String file =
         (String)query_parameters.getFirstParameterToken("CONFIGFILE", '=');


      System.out.println("[PSP_AggConfigReader] ++++> entered Agg Config Reader, urlspec=" + urlspec + ", filespec=" + file );

      if( (urlspec != null) && ( file != null) ) {
           out.println("<HTML><BODY><H3>Ambiguous INPUT source, both 'URL=' and 'FILE=' parameters set</H3></BODY></HTML>");
           return;
      }

      if( urlspec != null ) {

           URL u = new URL(urlspec);
           URLConnection uc = u.openConnection();
           InputStream istr = uc.getInputStream();

           //BufferedInputStream bis = new BufferedInputStream(is);
           byte buf[] = new byte[512];
           StringBuffer sb_in = new StringBuffer();

           int sz = 0;
           while( sz >= 0) {
               sz = istr.read(buf,0,512);
               String sbuf = new String(buf);
               if( sz > 0 ) {
                   sb_in.append(sbuf.toCharArray(),0,sz);
               }
           }
           StringReader sr = new StringReader(sb_in.toString());
           InputSource is = new InputSource(sr);

                    //System.err.println("str=" + str);
                    //FileReader freader = new FileReader("task.xml");
                    //InputSource is = new InputSource(freader);

           DOMParser domp = new DOMParser();
           domp.setErrorHandler(new ErrorHandler(){
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

           domp.parse(is);
           Document doc = domp.getDocument();
           StringWriter sw = new StringWriter();
           FormatterToXML fxml = new FormatterToXML(sw);
           TreeWalker treew = new TreeWalker(fxml);
           treew.traverse(doc);
           StringBuffer sb_out = sw.getBuffer();
           out.print("<HTML><BODY><PRE><BLOCKQUOTE>" +
                       sb_out.toString()
                       + "</BLOCKQUOTE></PRE></BODY></HTML>"
                       );

      }    ///////////////////////////////////////////////////////////

      else if ( file != null ) {
           FileReader fr = new FileReader(file);
           char[] cbuf = new char[512];
           String result = new String();
           StringBuffer sb_in = new StringBuffer();
           int len=0;
           while( (len = fr.read(cbuf)) > 0) {
                sb_in.append(cbuf,0,len);
           }
           StringBuffer sb_out = filterXMLtoHTML(sb_in);

           out.print("<HTML><BODY><PRE><BLOCKQUOTE>" +
                       sb_out.toString()
                       + "</BLOCKQUOTE></PRE></BODY></HTML>"
                       );
      }


      /**
      PlugInDelegate pd = psc.getServerPluginSupport().getDirectDelegate();
      Subscription subscription = psc.getServerPluginSupport().subscribe(this,
          new UnaryPredicate() { public boolean execute(Object o) { return true; }
          });
      Collection container = ((CollectionSubscription)subscription).getCollection();
      synchronized( container ) {
         Iterator it = container.iterator();
         while( it.hasNext() ){
            Object obj = it.next();
       }
       **/

  }

  private StringBuffer filterXMLtoHTML(StringBuffer dataout)
  {
       //int srcend = dataout.length();
       //char csrc[] = new char[srcend];
       //dataout.getChars(0,srcend,csrc,0);

       //StringWriter sw = new StringWriter(srcend);
       StringBuffer buf = new StringBuffer();

       int i;
       int sz = dataout.length();
       for(i=0;i<sz;i++){
           char c = dataout.charAt(i);
           if( c == '<' ) {  buf.append("&lt"); //System.out.print(" &lt ");
           }
           else if( c == '>' ) { buf.append("&gt");  //System.out.print(" &gt ");
           }
           else { buf.append(c); //System.out.print(c);
           }
       }
       return buf;
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


    public void subscriptionChanged(Subscription subscription) {

    }
}
