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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.w3c.dom.Attr;

import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;
import org.cougaar.lib.aggagent.dictionary.GenericLogic;
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

         // don't need the XML SERVICE at the AGG AGENT
         if( params.get(this.paramkey_XML_SERVICE) == null) {
               //
               // Install Default XML Service
               //
               try{
                  Object xobj_service_add = Class.forName(this.DEFAULT_XML_SERVICE).newInstance();
                  Object xobj_service_remove = Class.forName(this.DEFAULT_XML_SERVICE).newInstance();
                  Object xobj_service_changed = Class.forName(this.DEFAULT_XML_SERVICE).newInstance();

                  HashMap XML_Object_Services = new HashMap();
                  XML_Object_Services.put(GenericLogic.collectionType_ADD, xobj_service_add);
                  XML_Object_Services.put(GenericLogic.collectionType_REMOVE, xobj_service_remove);
                  XML_Object_Services.put(GenericLogic.collectionType_CHANGE, xobj_service_changed);

                  params.put(this.paramkey_XML_SERVICE, XML_Object_Services);

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
     public void execute( Collection matches, final String collectionType  )
     {
           synchronized( matches )
           {
              Object [] objs = matches.toArray();
              for(int i=0; i< objs.length; i++){
                   Object o = objs[i];
                   // create XML document for selected plan objects
                   // accumulated!

                   //XMLObjectProvider myObjectProvider =
                   //       ObjectProvider_x_SubscriptionCollectionTypes.get(collectionType);

                   HashMap   myObjectProviderMap =
                            (HashMap)this.getParam(this.paramkey_XML_SERVICE);
                   XMLObjectProvider myObjectProvider =
                            (XMLObjectProvider)myObjectProviderMap.get(collectionType);

                   synchronized( myObjectProvider ) {
                        // if( collectionType.equals(GenericLogic.collectionType_ADD) ) {
                            System.out.println("[GenericQueryXML.execute] Placing Object in ObjectProvider:"
                                               + collectionType);
                            addObject(o, myObjectProvider);
                        // }
                   }
              }
           }
     }

     protected boolean existsXSLParameter(){
          StringBuffer sbuf_xsl = (StringBuffer)this.getParam(this.paramkey_XSL);
          return (sbuf_xsl != null);
     }

     protected XSLTInputSource getXSLFromParameter(){
          XSLTInputSource xslis = null;
          StringBuffer sbuf_xsl = (StringBuffer)this.getParam(this.paramkey_XSL);
          if( sbuf_xsl != null) {
               System.out.println("[GenericQueryXML] XSL script read, string length=" + sbuf_xsl.length());
               xslis =  new XSLTInputSource(new StringReader(sbuf_xsl.toString()));
          } else
          {
               System.out.println("[GenericQueryXML] XSL script read: null");
          }
          return xslis;
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
          System.out.println("[GenericQueryXML.returnVal] Called.");

          HashMap myObjectProviderMap = (HashMap)this.getParam(this.paramkey_XML_SERVICE);

          // XMLObjectProvider   myObjectProvider = (XMLObjectProvider)this.getParam(this.paramkey_XML_SERVICE);

          XSLTInputSource  myXSL = null;
          String mySAXContentHandlerClassName = null;

          //StringBuffer sbuf_xsl = (StringBuffer)preConfiguredParameters.get(this.paramkey_XSL);
          //if( sbuf_xsl != null) myXSL =  new XSLTInputSource(new StringReader(sbuf_xsl.toString()));
          //System.out.println(" myXSL=" + myXSL);

          //
          // SAX CONTENTHANDLER PARAMETER?
          //
          String sbuf_sax = (String)this.getParam(this.paramkey_SAX);
          if( sbuf_sax != null) mySAXContentHandlerClassName = sbuf_sax.toString();
          if( mySAXContentHandlerClassName != null) {
              System.out.println("[GenericQueryXML.returnVal] SAX HANDLER USED:" + mySAXContentHandlerClassName);
          }
          else {
              System.out.println("[GenericQueryXML.returnVal] SAX HANDLER NOT USED." );
          }

          //#################################
          // XSL PROCESSING  (DOM)
          // ################################

          //
          //  Potentially, sending multiple documents (1 per subscription event type
          //  for which objects are available...
          //
          //   <LogPlan>...</LogPlan>
          //   <LogPlan>...</LogPlan>
          //   <LogPlan>...</LogPlan>
          //
          if( existsXSLParameter() )
          {
              try {
                  myXSL=getXSLFromParameter();

                  boolean output = false;
                  String delimiter = new String("&&&");

                  XMLObjectProvider myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_ADD);
                  if( myObjectProvider.size() > 0)
                  {
                      System.out.println("[GenericQueryXML.returnVal] XSL (collectionType_ADD)");
                      if( output == true ) {
                                         out.write(delimiter.getBytes());
                                         out.flush();
                       }
                      output = true;
                      processContainerXSL(
                               myObjectProvider, myXSL, out, GenericLogic.collectionType_ADD );

                  }
                  myXSL=getXSLFromParameter();

                  myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_CHANGE);
                  if( myObjectProvider.size() > 0 )
                  {
                      System.out.println("[GenericQueryXML.returnVal] XSL (collectionType_CHANGE)");
                      if( output == true ) {
                                         out.write(delimiter.getBytes());
                                         out.flush();
                       }
                       output = true;
                       processContainerXSL(myObjectProvider, myXSL, out, GenericLogic.collectionType_CHANGE );
                  }
                  myXSL=getXSLFromParameter();

                  myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_REMOVE);
                  if( myObjectProvider.size() > 0 )
                  {
                       System.out.println("[GenericQueryXML.returnVal] XSL (collectionType_REMOVE)");
                       if( output == true ) {
                                         out.write(delimiter.getBytes());
                                         out.flush();
                       }
                       output = true;
                       processContainerXSL(myObjectProvider, myXSL, out, GenericLogic.collectionType_REMOVE );
                  }
                 /**
                 synchronized( myObjectProvider) {
                     Object odoc= getDocument(myObjectProvider);
                     if( odoc instanceof TXDocument) doc = (TXDocument)odoc;
                     if( doc != null)
                     {
                         //Element ce = doc.createElement("Container");
                         //ce.setAttribute("Event", "ADD");
                         //doc.getFirstChild().appendChild(ce);

                         NodeList children = doc.getFirstChild().getChildNodes();
                         for(int i=0; i<children.getLength(); i++)
                         {
                            Node n= (Node)children.item(i);
                            Element elem = doc.createElement("Subscription");
                            elem.setAttribute("Event","ADD");
                            n.appendChild(elem);
                         }
                     }
                 }
                 //
                 // This is BAD.
                 // TODO:  FIX THIS
                 // converting TO STRING TO APPLY XSL is v. inefficient.
                 // When COUGAAR XML parser version is updated, will go away.
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

                    // DEBUG
                    //doc.print(new PrintWriter(System.out));
                    //
                 } else if( (doc != null)  ) {
                     doc.print(new PrintWriter(out));
                 }

                 // send document to client
                 //doc.print(new PrintWriter(out));
                 **/
              } catch (Exception ex ){
                    ex.printStackTrace();
              }
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

              XMLObjectProvider myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_ADD);

              if( myObjectProvider.size() > 0) {
                    processContainerSAX(myObjectProvider, myContentHandler, mySaxParser, out,
                                   GenericLogic.collectionType_ADD );
              }

              myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_CHANGE);

              if( myObjectProvider.size() > 0 ) {
                   processContainerSAX(myObjectProvider, myContentHandler, mySaxParser, out,
                                   GenericLogic.collectionType_CHANGE );
              }

              myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_REMOVE);
              if( myObjectProvider.size() > 0 ) {
                   processContainerSAX(myObjectProvider, myContentHandler, mySaxParser, out,
                                   GenericLogic.collectionType_REMOVE );
              }

              /**
              TXDocument doc = null;
              synchronized( myObjectProvider)
              {
                     Object odoc= getDocument(myObjectProvider);
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
              **/
          }
          // ##############################
          // FLUSH! object cache after we've consumed data
          // ##############################

          XMLObjectProvider myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_ADD);
          synchronized( myObjectProvider ) {
              reset(myObjectProvider);
          }

          myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_CHANGE);
          synchronized( myObjectProvider ) {
              reset(myObjectProvider);
          }

          myObjectProvider =
                      (XMLObjectProvider)myObjectProviderMap.get(GenericLogic.collectionType_REMOVE);
          synchronized( myObjectProvider ) {
              reset(myObjectProvider);
          }
  }

  //
  // Process one container of subscription Objects (ADD,CHANGED,REMOVED)
  // and converts to XML and applies SAX.  The result is written to provided
  // output stream.
  //
  private void processContainerSAX(XMLObjectProvider myObjectProvider,
                                   BContentHandler myContentHandler, SAXParser mySaxParser, OutputStream out,
                                   final String collectionType){
       try{
           TXDocument doc = null;
           synchronized( myObjectProvider) {
                     Object odoc= getDocument(myObjectProvider);
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
       } catch (Exception ex ){
            ex.printStackTrace();
       }
  }


  //
  // Process one container of subscription Objects (ADD,CHANGED,REMOVED)
  // and converts to XML and applies XSL.  The result is written to provided
  // output stream.
  //
  private void processContainerXSL(XMLObjectProvider myObjectProvider,
                                   XSLTInputSource  myXSL, OutputStream out,
                                   final String collectionType )
  {
       TXDocument doc = null;
       try{
             synchronized( myObjectProvider)
             {
                     Object odoc= getDocument(myObjectProvider);
                     if( odoc instanceof TXDocument) doc = (TXDocument)odoc;
                     if( doc != null)
                     {
                         //Element ce = doc.createElement("Container");
                         //ce.setAttribute("Event", "ADD");
                         //doc.getFirstChild().appendChild(ce);

                         NodeList children = doc.getFirstChild().getChildNodes();
                         for(int i=0; i<children.getLength(); i++)
                         {
                             Node n= (Node)children.item(i);
                             Element elem = doc.createElement("Subscription");
                             if( collectionType.equals(GenericLogic.collectionType_ADD)) {
                                 elem.setAttribute("Event","ADD");
                             }
                             else if( collectionType.equals(GenericLogic.collectionType_CHANGE)){
                                 elem.setAttribute("Event","CHANGE");
                             }
                             else if( collectionType.equals(GenericLogic.collectionType_REMOVE)){
                                 elem.setAttribute("Event","REMOVE");
                             } else
                                 elem.setAttribute("Event","UNKNOWN");

                             n.appendChild(elem);
                         }
                     }
             }
             //
             // This is BAD.
             // TODO:  FIX THIS
             // converting TO STRING TO APPLY XSL is v. inefficient.
             // When COUGAAR XML parser version is updated, will go away.
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

                    // DEBUG
                    //doc.print(new PrintWriter(System.out));
                    //

             } else if( (doc != null)  )
                           doc.print(new PrintWriter(out));
       } catch (Exception ex ) {
           ex.printStackTrace();
       }
  }

  //-------------------------------------------------------------------------
  // WRAPPERS TO XmlObjectProvider methods-- to trap exceptions and notify
  //-------------------------------------------------------------------------
  private void addObject( Object obj, XMLObjectProvider provider) {
      try{
           provider.addObject(obj);
      } catch (Exception ex){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Exception follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.addObject");
            System.err.println("#####################################################");
            ex.printStackTrace();
      } catch (Error err){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Error follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.addObject");
            System.err.println("#####################################################");
            err.printStackTrace();
      }
  }
  private void reset(XMLObjectProvider provider){
      try{
           provider.reset();
      } catch (Exception ex){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Exception follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.reset");
            System.err.println("#####################################################");
            ex.printStackTrace();
      } catch (Error err){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Error follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.reset");
            System.err.println("#####################################################");
            err.printStackTrace();
      }
  }
  private Document getDocument(XMLObjectProvider provider) {
      try{
           return provider.getDocumentRef();
      } catch (Exception ex){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Exception follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.getDocumentRef");
            System.err.println("#####################################################");
            ex.printStackTrace();
      } catch (Error err){
            System.err.println("#####################################################");
            System.err.println("CORE SERVICE FAILED.  Error follows.");
            System.err.println("[XMLService]  Entry=XMLObjectProvider.getDocumentRef");
            System.err.println("#####################################################");
            err.printStackTrace();
      }
      return null;
  }
  //-------------------------------------------------------------------------
  // END - WRAPPERS TO XmlObjectProvider methods-- 
  //-------------------------------------------------------------------------

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
           PrintWriter pw = new PrintWriter(System.out);
           pw.println("#####################################################");
           System.err.println("XSL APPLICATION FAILED.  Exception follows.");
           ex.printStackTrace(pw);
           pw.println("#####################################################");
       }
  }

}

