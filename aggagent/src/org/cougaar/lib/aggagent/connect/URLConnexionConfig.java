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
