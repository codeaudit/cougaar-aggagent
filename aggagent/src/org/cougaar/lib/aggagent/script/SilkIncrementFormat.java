
package org.cougaar.lib.aggagent.script;

import java.io.OutputStream;
import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.session.*;

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
   *  Encode the information contained in the RemoteSubscription in accordance
   *  with the SILK script embodied by this SilkIncrementFormat.
   */
  public void encode (OutputStream out, SubscriptionAccess sess, String key,
      String queryId, String clusterId)
  {
    silkProcedure.apply(new Object[] {out, sess, key, queryId, clusterId});
  }
}