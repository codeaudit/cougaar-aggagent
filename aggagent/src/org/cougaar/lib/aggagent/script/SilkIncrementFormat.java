
package org.cougaar.lib.aggagent.script;

import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;

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
