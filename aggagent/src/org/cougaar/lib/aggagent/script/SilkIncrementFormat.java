/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
