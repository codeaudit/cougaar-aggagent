package org.cougaar.lib.aggagent.session;

import java.io.*;

public interface IncrementFormat {
  public void encode (UpdateDelta out, SubscriptionAccess sacc);
}
