package org.cougaar.lib.aggagent.dictionary.glquery;

import java.io.Serializable;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Vector;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.core.util.XMLObjectProvider;

import com.ibm.xml.parser.TXDocument;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;
import org.cougaar.lib.aggagent.bsax.*;

//
// Example Generic Query instance which applies arbitrary XSL to
// UnaryPredicate predicate results and returns Document.
// Suited for Society Clusters only -- not Agg Agents.
//
// If no XSL provided -- returns raw XML.
//
public class GenericQueryXML  implements GenericQuery
{
     protected Object myKey= null;
     protected Map preConfiguredParameters = null;

     //
     // if SAX parser is needed (IE. SAX ContentHandler provided for transform,
     // save handle to Parser...
     //
     private SAXParser mySaxParser = null;


     // Accumulates query state...
     // flushed when contents xmlized...
     private final String DEFAULT_XML_SERVICE = "org.cougaar.domain.mlm.ui.psp.xmlservice.XMLPlanObjectProvider";


     public GenericQueryXML() {
     }

     public Object getParam(Object key) {
         Object val = null;
         synchronized ( preConfiguredParameters) {
             val= preConfiguredParameters.get(key);
         }
         return val;
     }

     public void init(Object key, Map params){
         myKey = key;
         preConfiguredParameters = params;

         if( params.get(this.paramkey_XML_SERVICE) == null) {
               //
               // Install Default XML Service
               //
               try{
                  Object xobj = Class.forName(this.DEFAULT_XML_SERVICE).newInstance();
                  params.put(this.paramkey_XML_SERVICE, xobj);

               } catch (Exception ex) {
                  ex.printStackTrace();
               }
         }
     }

     // Key to the GenericQuery instance, eg. name
     public Object getKey() {
         return myKey;
     }

     // Generic PSP uses this predicate to subscribe to log plan.
     //
     public UnaryPredicate getPredicate() {
         UnaryPredicate predicate=null;
         synchronized( preConfiguredParameters ) {
             predicate = (UnaryPredicate)preConfiguredParameters.get(this.paramkey_PREDICATE);
         }
         return predicate;
     }

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // GenericQueryXML accumulates state
     public void execute( Collection matches ) {
           synchronized( matches )
           {
              Object [] objs = matches.toArray();
              for(int i=0; i< objs.length; i++){
                   Object o = objs[i];
                   // create XML document for selected plan objects
                   // accumulated!
                   XMLObjectProvider   myObjectProvider =
                            (XMLObjectProvider)this.getParam(this.paramkey_XML_SERVICE);
                   synchronized( myObjectProvider ) {
                       try{
                            myObjectProvider.addPlanObject(o);

                       } catch( Exception ex) {
                            System.err.println("#####################################################");
                            System.err.println("CORE LOGPLAN SERVICE FAILED.  Exception follows.");
                            System.err.println("[XMLService]  Entry=XMLObjectProvider.addPlanObject");
                            System.err.println("#####################################################");
                            ex.printStackTrace();
                       }

                   }
              }
           }
     }

     //
     // Returns current snapshot (in XML Document form) of current state of this
     // Query instance -- implicitly flushes state ...
     //
     // NOTE: "XSL=" URL parameter is optional.  If not null, takes precedence
     //            over default XSL assigned to this entry.
     //
     public void  returnVal( OutputStream out)
     {
          System.out.print("[GenericQueryXML.returnVal] called.");
          XMLObjectProvider   myObjectProvider = (XMLObjectProvider)this.getParam(this.paramkey_XML_SERVICE);

          XSLTInputSource  myXSL = null;
          String mySAXContentHandlerClassName = null;

          //
          // XSL PARAMETER?
          //
          StringBuffer sbuf_xsl = (StringBuffer)preConfiguredParameters.get(this.paramkey_XSL);
          if( sbuf_xsl != null) myXSL =  new XSLTInputSource(new StringReader(sbuf_xsl.toString()));
          System.out.println(" myXSL=" + myXSL);

          //
          // SAX CONTENTHANDLER PARAMETER?
          //
          String sbuf_sax = (String)preConfiguredParameters.get(this.paramkey_SAX);
          if( sbuf_sax != null) mySAXContentHandlerClassName = sbuf_sax.toString();
          System.out.println(" mySAXContentHandlerClassName=" + mySAXContentHandlerClassName);

          //#################################
          // XSL PROCESSING
          // ################################
          if( myXSL != null)
          {
              try {   /**
                  File xslf = new File( "../../gldictionary_entries/task.simple.1.xsl");
                  System.out.println("XSL FILE ATTEMPTED READ:" + xslf.getAbsolutePath() );
                  FileReader fr = new FileReader(xslf);
                  XSLTInputSource  xslsource =  new XSLTInputSource(fr);
                  myXSL= xslsource;
                  **/

                 TXDocument doc = null;
                 synchronized( myObjectProvider) {
                     Object odoc= myObjectProvider.getDocumentRef();
                     if( odoc instanceof TXDocument) doc = (TXDocument)odoc;
                 }
                 //
                 // this is TERRIBLE... converting DOM TO STRING TO APPLY XSL
                 //  inefficient, but until old
                 // ALP XML parser version can be updated, no choice...
                 //
                 if( (myXSL != null) && ( doc != null) )
                 {
                    StringWriter sw = new StringWriter();
                    doc.print(sw);
                    StringBuffer sb = sw.getBuffer();
                    //System.err.println("returnVal, sb.toSTring()=" + sb.toString().substring(0,120));
                    //System.err.println("returnVal, sbuf.toString()=" + sbuf.toString());
                    StringReader sr = new StringReader(sb.toString() );
                    applyXSL(sr,myXSL, out);

                 } else if( (doc != null)  ) {
                     doc.print(new PrintWriter(out));
                 }

                 // send document to client
                 //doc.print(new PrintWriter(out));
              } catch (Exception ex ){
                    ex.printStackTrace();
              }
              // flush!
          }
          //#################################
          // SAX PROCESSING
          // ################################
          else if ( mySAXContentHandlerClassName != null ) {

              //
              // Set from mySAXContentHandlerClassName...
              //
              BContentHandler myContentHandler=null;
              try{
                 Class klass = Class.forName(mySAXContentHandlerClassName);
                 myContentHandler = (BContentHandler)klass.newInstance();
              } catch (ClassNotFoundException cex ) {
                 System.err.println("[GenericQueryXML.returnVal()]  Class NOT FOUND trying to create new Content Handler instance.");
                 System.err.println("[[GenericQueryXML.returnVal()]  Be sure handler is in classpath.");
                 cex.printStackTrace();
              } catch (ClassCastException cex) {
                 System.err.println("[GenericQueryXML.returnVal()]  Class CAST EXCEPTION creating new Content Handler instance.");
                 System.err.println("[[GenericQueryXML.returnVal()]  Be sure handler implements interface BContentHandler!");
                 cex.printStackTrace();
              } catch (Exception ex) {
                 ex.printStackTrace();
              }
              //BContentHandler myContentHandler = new BContentHandler();


              if( mySaxParser == null) {
                    mySaxParser = new SAXParser();
                    try {
                         // try to activate validation
                         mySaxParser.setFeature("http://xml.org/sax/features/validation", true);
                    } catch (SAXException e) {
                         System.err.println("Cannot activate validation.");
                    }
              } // end create SAX parser

              if( mySaxParser != null)
              {
                  mySaxParser.setContentHandler(myContentHandler);
                  mySaxParser.setErrorHandler(new BErrorHandler());
              }

              TXDocument doc = null;
              synchronized( myObjectProvider) {
                     Object odoc= myObjectProvider.getDocumentRef();
                     if( odoc instanceof TXDocument) doc = (TXDocument)odoc;
              }
              if( ( doc != null) )
              {
                    StringWriter sw = new StringWriter();
                    try{
                        doc.print(sw);
                    } catch( IOException ex ) {
                        ex.printStackTrace();
                    }
                    StringBuffer sb = sw.getBuffer();
                    StringReader str_reader = new StringReader(sb.toString() );

                    try {
                        InputSource in = new InputSource(str_reader);
                        mySaxParser.parse(in);
                    } catch (IOException e) {
                        System.err.println("I/O exception reading XML document. " + e);
                        e.printStackTrace();
                    } catch (SAXException e) {
                        System.err.println("SAX exception parsing document. " + e);
                        e.printStackTrace();
                    }
                   //-----------------------------------------------------------------
                   // Traverse and "write" the tree
                   // Start from the root object
                   //-----------------------------------------------------------------
                   PrintWriter pw = new PrintWriter(out);
                   pw.println("<?xml version=\"1.0\"?>");
                   Iterator it3 = myContentHandler.getRootElements().iterator();
                   while(it3.hasNext()){
                      BElement be = (BElement)it3.next();
                      be.print(pw);
                   }
                   pw.flush();
                   System.out.println("[GenericQueryXML] pw.toString()=" + pw.toString() );

              }

          }

          // ##############################
          // FLUSH! object cache after we've consumed data
          // ##############################
          synchronized( myObjectProvider ) {
              myObjectProvider.reset();
          }
     }

	public  void applyXSL(StringReader xmldata, XSLTInputSource xsl, OutputStream out)
            throws java.io.IOException,
                   java.net.MalformedURLException
	{
    try{
        // Have the XSLTProcessorFactory obtain a interface to a
        // new XSLTProcessor object.
        XSLTProcessor processor = XSLTProcessorFactory.getProcessor();

        synchronized( processor ){

           // Have the XSLTProcessor processor object transform "foo.xml" to
           // System.out, using the XSLT instructions found in "foo.xsl".
           processor.process(
                      new XSLTInputSource(xmldata),
                      xsl,
                      new XSLTResultTarget(out));
        }
    } catch (org.xml.sax.SAXException ex ){
          ex.printStackTrace(new PrintWriter(System.out));
    }
  }

}

