/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
