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
package org.cougaar.lib.aggagent.dictionary;

import java.util.*;
import java.io.Serializable;
import java.io.OutputStream;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.List;

import org.cougaar.lib.planserver.HttpInput;


public interface GenericLogic
{
     public final String collectionType_ADD    = "ADD";
     public final String collectionType_REMOVE = "REMOVE";
     public final String collectionType_CHANGE = "CHANGE";

     public void init(Object key, Map params);

     public Object getParam(Object key);

     // Key to the GenericQuery instance, eg. name
     public Object getKey();

     // Generic PSP uses this predicate to subscribe to objects
     public UnaryPredicate getPredicate();

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // Query instances is responsible for accumulating state
     public void execute( Collection matches, final String collectionType );
}