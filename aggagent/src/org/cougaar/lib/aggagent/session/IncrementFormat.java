/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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
package org.cougaar.lib.aggagent.session;

import java.io.*;

/**
 *  IncrementFormat is the interface implemented by Objects that convert local
 *  subscription data for transportation to other agents (aggregation agents,
 *  generally).  Information obtained through the SubscriptionAccess interface
 *  can be translated in an arbitrary manner into an UpdateDelta, which is
 *  then shipped off as an XML document.
 *  <br><br>
 *  For many purposes, it suffices to use XmlIncrement (q.v.) with a suitable
 *  implementation of XMLEncoder, which is easier than creating a custom-built
 *  IncrementFormat from scratch.
 */
public interface IncrementFormat {
  /**
   *  Encode subscription data contained in the provided SubscriptionAccess as
   *  ResultSetDataAtoms and insert them as content.  into the provided
   *  UpdateDelta.  Please note that the implementation is responsible for
   *  calling setReplacement() with the appropriate value as part of this
   *  operation.
   */
  public void encode (UpdateDelta out, SubscriptionAccess sacc);
}
