package org.cougaar.lib.aggagent.session;

import java.io.*;

import org.cougaar.lib.aggagent.query.UpdateDelta;

public interface IncrementFormat {
  public void encode (UpdateDelta out, SubscriptionAccess sacc);
}
