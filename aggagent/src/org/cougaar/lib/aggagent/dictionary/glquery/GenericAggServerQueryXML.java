/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.lib.aggagent.dictionary.glquery;

import java.io.Serializable;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Vector;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;


import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.lib.planserver.HttpInput;
//import org.cougaar.domain.mlm.ui.psp.xmlservice.XMLPlanObjectProvider;
//import mil.darpa.log.alpine.ui.psp.xmlservice.XMLPlanObjectProvider;


import com.ibm.xml.parser.TXDocument;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTProcessor;
import org.apache.xalan.xslt.XSLTInputSource;
import org.apache.xalan.xslt.XSLTResultTarget;
import org.apache.xalan.xslt.XSLTProcessorFactory;
import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.apache.xerces.dom.NodeImpl;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;
import org.cougaar.lib.aggagent.ldm.PlanObject;



//
// Generic Query plugin for Agg Agent Generic PSPs --
// Difference with Society Generic Query plugins is that
// it works with multiple DOM data structures vs. LDM
// objects.  Example, one bit of overhead due to consolidation
// of multiple DOMs into single DOM for single document generation.
//
// Non-Keep Alive usuable.
//
public class GenericAggServerQueryXML  extends GenericQueryXML
{

     private List myObjects = new ArrayList();

     public GenericAggServerQueryXML(){
         super();
     }

     public void init( Object key, Map params) {
         // super.init(key,params);
         myKey = key;
         preConfiguredParameters = params;
     }

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // GenericQueryXML accumulates state
     //
     public void execute( Collection matches ) {
           Iterator it = matches.iterator();
           while( it.hasNext() ){
                Object o = it.next();
                PlanObject po = (PlanObject)o;
                Document doc =po.getDocument();
                // extract and save document
                System.out.println("added child to myObjects!");
                myObjects.add(doc);
           }

     }
     //
     // Returns current snapshot (in XML Document form) of current state of this
     // Query instance -- implicitly flushes state ...
     //
     public void returnVal(OutputStream out) {

          System.out.print("[GenericAggServerQueryXML.returnVal] called.");
          XSLTInputSource  myXSL = null;

          //StringBuffer  sbuf = (StringBuffer)preConfiguredParameters.get("XSL");
          //if( sbuf != null) myXSL =  new XSLTInputSource(new StringReader(sbuf.toString()));
          //
          //System.out.println(" myXSL=" + myXSL);

          //
          // XSL PARAMETER?
          //
          myXSL=this.getXSLFromParameter();

          try {   /**
                   DEBUG ---------------------------
                  File xslf = new File( "../../gldictionary_entries/task.simple.1.xsl");
                  System.out.println("XSL FILE ATTEMPTED READ:" + xslf.getAbsolutePath() );
                  FileReader fr = new FileReader(xslf);
                  XSLTInputSource  xslsource =  new XSLTInputSource(fr);
                  myXSL= xslsource;
                  **/

                  Document agg = new org.apache.xerces.dom.DocumentImpl();
                  Element root = agg.createElement("LogPlan");

                  //
                  // CONSOLIDATE DOCS into SINGLE DOC
                  //

                  Object [] objs = myObjects.toArray();
                  Document doc = null;
                  for( int k=0; k< objs.length; k++) {
                        doc= (Document)objs[k];
                        //System.out.println("-- another child inserted!");
                        NodeList nl = doc.getChildNodes();
                        int size = nl.getLength();
                        for(int i=0; i<size; i++) {
                            Node n = (Node)nl.item(i);       // this is <LogPlan> node
                            NodeList nl2 = n.getChildNodes();
                            int size2 = nl2.getLength();
                            for(int j=0; j<size2; j++)
                            {
                                Node n2 = (Node)nl2.item(j);
                                //System.out.println("--------------nodename="+ n2.getNodeName());
                                Node importedn = agg.importNode(n2,true);
                                root.appendChild(importedn);
                                // wrong element: root.appendChild(n2);
                            }
                        }
                        //root.appendChild(agg.createElement("height"));
                  }

                  agg.appendChild(root);
                  System.out.println("[GenericAggServerQueryXML] Created consolidated doc len=" + agg.getChildNodes().getLength() );


                // TEST: OUTPUT CONSOLIDATED DOC ----------------
                //
                // Use the FormatterToXML and TreeWalker to print the DOM to System.out
                // Note: Not yet sure how to get the Xerces Serializer to  handle
                // arbitrary nodes.
                //StringWriter sw2 = new StringWriter();
                //FormatterToXML fxml2 = new FormatterToXML(sw2);
                //TreeWalker treew2 = new TreeWalker(fxml2);
                //treew2.traverse(agg);
                //System.out.println("SW2=" + sw2.toString() );
                // END TEST -------------------------------------

                 if( (myXSL != null) && ( doc != null) )
                 {
                    StringWriter sw = new StringWriter();
                    FormatterToXML fxml = new FormatterToXML(sw);
                    TreeWalker treew = new TreeWalker(fxml);
                    treew.traverse(agg);
                    // doc.print(sw); // old TXDocument api


                    StringBuffer sb = sw.getBuffer();
                    //System.err.println("returnVal, sb.toSTring()=" + sb.toString().substring(0,120));
                    //System.err.println("returnVal, sbuf.toString()=" + sbuf.toString());
                    StringReader sr = new StringReader(sb.toString() );
                    applyXSL(sr,myXSL, out);

                 } else if (agg != null) {
                     FormatterToXML fl = new FormatterToXML(out);
                     TreeWalker tw = new TreeWalker(fl);
                     tw.traverse(agg);
                 }

          } catch (Exception ex ){
                 ex.printStackTrace();
          }
          /**
          String comment = new String();
          comment += "!--";
          comment += paramkey_PREDICATE + ":" + preConfiguredParameters.get(paramkey_PREDICATE);
          comment += ";" + paramkey_XSL + ":" + preConfiguredParameters.get(paramkey_XSL);
          comment += "-->";
          PrintWriter pw = new PrintWriter(out);
          pw.println(comment);
          pw.close();
          **/

          // flush!
          myObjects = new ArrayList();
     }

}

