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
package org.cougaar.lib.aggagent.dictionary.glquery;

import java.io.Serializable;
import java.io.OutputStream;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.List;

import org.cougaar.lib.aggagent.dictionary.GenericLogic;


/** @param myxsl_alias : (optional).  If specified, look up XSL using this key.
  *                       if null, use default associated with this Entry
  *                       if defined, otherwise no xsl.
  **/
public interface GenericQuery extends GenericLogic, Serializable
{
     //
     // this keys are used to locate input data, configuration which
     // parameterizes this query.
     //
     public final String paramkey_XML_SERVICE = "XML_SERVICE";
     public final String paramkey_PREDICATE = "PREDICATE";
     public final String paramkey_XSL = "XSL";
     public final String paramkey_SAX = "SAX";

     // Obtain parameter data
     public Object getParam(Object key);


     // Returns current snapshot (in XML Document form) of current state of this
     // Query instance -- implicitly flushes state
     //
     public void returnVal( OutputStream out);
}
