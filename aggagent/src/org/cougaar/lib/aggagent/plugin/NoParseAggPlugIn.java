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

package org.cougaar.lib.aggagent.plugin;

import org.cougaar.lib.aggagent.ldm.XMLPlanObject;

/**
 *  The NoParseAggPlugIn behaves pretty much like an AggregatorPlugin except
 *  that it does not parse incoming XML documents.  In stead, reminiscent of
 *  the treatment of HTML documents, they are wrapped as plain text inside
 *  XMLPlanObjects (which are analogous to HTMLPlanObjects, q.v.) and published
 *  on the logplan.
 */
public class NoParseAggPlugIn extends AggregatorPlugin {
    /**
     *  Handle a complete response from one of the connections, presuming the
     *  contents to be formatted as an XML document.  In this implementation,
     *  an XMLPlanObject is created to contain the XML document on the logplan.
     *
     *  @param str a String containing the XML document
     */
    protected void processDataFromConnection_asXML (String str) {
      publishAdd(new XMLPlanObject(str));
    }
}
