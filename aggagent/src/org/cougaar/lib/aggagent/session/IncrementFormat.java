package org.cougaar.lib.aggagent.session;

import java.io.*;

public interface IncrementFormat {
  public void encode (OutputStream out, SubscriptionAccess sess, String key,
                      String queryId, String clusterId);
}
