/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.lib.aggagent.connect;

import org.w3c.dom.Node;

import org.cougaar.lib.aggagent.xml.XMLParseCommon;

public class URLConnexionConfig {
  public String name = null;

  public String psp = null;

  public String url = null;

  public String interval = null;

  public URLConnexionConfig () {
  }

  public URLConnexionConfig (Node n) {
    name = XMLParseCommon.getAttributeOrChildNodeValue("name", n);
    psp = XMLParseCommon.getAttributeOrChildNodeValue("pspquery", n);
    url = XMLParseCommon.getAttributeOrChildNodeValue("url", n);
    interval = XMLParseCommon.getAttributeOrChildNodeValue("pollInterval", n);
  }

  /**
   *  Create a new URLConnexionConfig from the four basic components
   */
  public URLConnexionConfig (String n, String p, String u, String i) {
    name = n;
    psp = p;
    url = u;
    interval = i;
  }
}
