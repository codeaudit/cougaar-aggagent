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
package org.cougaar.lib.aggagent.dictionary.glquery.samples;


import java.io.Serializable;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;

import org.cougaar.lib.aggagent.ldm.PlanObject;


//
// This GL essentially aggregates all the incoming individual "organizations"
// collections
//
//   <collection>
//       <Cluster> </Cluster>
//       ...
//   </collection>
//   <collection>
//       <Cluster> </Cluster>
//       ...
//   </collection>
//
public class AggQueryOrgHierarchy extends CustomQueryBaseAdapter
{
     private List myStuff = new ArrayList();

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // GenericQueryXML accumulates state
     public void execute( Collection matches, final String collectionType  ) {

           synchronized( matches) {

              Object [] objs = matches.toArray();
              for(int i=0; i< objs.length; i++)
              {
                   Object obj = objs[i];
                   // save it.
                   myStuff.add(obj);
              }
           }
     }

     //
     public void returnVal( OutputStream out) {
          System.out.println("[AggQueryOrgHierarchy] called");

          Document agg = new org.apache.xerces.dom.DocumentImpl();
          Element root = agg.createElement("collection");

          //Iterator it = myStuff.iterator();
          PrintWriter pw = new PrintWriter(out);

          synchronized( myStuff )
          {
             Object [] objs = myStuff.toArray();
             for( int i=0; i< objs.length; i++ ){
                 Object obj = objs[i];
                 if( obj instanceof PlanObject ) {
                      PlanObject p = (PlanObject)obj;
                      Document doc = p.getDocument();
                      NodeList nl = doc.getElementsByTagName("Cluster");
                      if( nl.getLength() > 0 ) {
                          //pw.println("\t<FoundAsset/>");
                          System.out.println("[AggQueryOrgHierarchy] ... found Cluster");
                          for(int j=0; j< nl.getLength(); j++)
                          {
                              Node n = nl.item(j);
                              Node importedn = agg.importNode(n,true);
                              root.appendChild(importedn);
                          }
                      }
                 }
             }
          }
          agg.appendChild(root);
          System.out.println("---------------[AggQueryOrgHierarych] agg len=" + agg.getChildNodes().getLength() );

          StringWriter sw = new StringWriter();
          FormatterToXML fxml = new FormatterToXML(sw);
          TreeWalker treew = new TreeWalker(fxml);
          try{
              treew.traverse(agg);
          } catch (Exception ex ) {
              ex.printStackTrace();
          }

          StringBuffer sb = sw.getBuffer();
          pw.print(sb.toString());

          pw.flush();
     }
}


