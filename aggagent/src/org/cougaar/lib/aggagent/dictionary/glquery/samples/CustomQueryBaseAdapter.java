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
import java.util.Map;
import java.util.HashMap;
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

import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;



public class CustomQueryBaseAdapter     implements GenericQuery
{
     protected Object myKey= null;
     protected Map preConfiguredParameters = null;

     public CustomQueryBaseAdapter() {
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
             predicate = (UnaryPredicate)preConfiguredParameters.get("PREDICATE");
         }
         return predicate;
     }

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // GenericQueryXML accumulates state
     public void execute( Collection matches, final String collectionType  ){
         new RuntimeException( "Need to provide behavior to  CustomQueryBaseAdapter.execute()");
     }

     //
     public void returnVal( OutputStream out ) {
         new RuntimeException( "Need to provide behavior to CustomQueryBaseAdapter.returnVal()");
     }


}

