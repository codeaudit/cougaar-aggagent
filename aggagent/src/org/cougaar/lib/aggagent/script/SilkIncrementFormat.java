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

package org.cougaar.lib.aggagent.script;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;
import org.cougaar.lib.aggagent.session.UpdateDelta;

import silk.Procedure;
import silk.SI;

/**
 *  An implementation of the IncrementFormat interface that derives its
 *  behavior from a SILK script.
 */
public class SilkIncrementFormat implements IncrementFormat {
  private Procedure silkProcedure;

  /**
   *  Create a new SilkIncrementFormat instance.  The provided script is used
   *  herein as an implementation of the encode() method of interface
   *  IncrementFormat.
   *  @param silkScript the text of the SILK code
   */
  public SilkIncrementFormat (String silkScript) {
    this.silkProcedure = (Procedure) SI.eval(silkScript);
  }

  /**
   *  Encode the information contained in the SubscriptionAccess in accordance
   *  with the SILK script embodied by this SilkIncrementFormat.
   */
  public void encode (UpdateDelta out, SubscriptionAccess sacc) {
    silkProcedure.apply(new Object[] {out, sacc});
  }
}
