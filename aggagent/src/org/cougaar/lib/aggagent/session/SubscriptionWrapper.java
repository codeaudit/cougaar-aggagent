package org.cougaar.lib.aggagent.session;

import java.util.Collection;
import org.cougaar.core.blackboard.IncrementalSubscription;

// Wrap an IncrementalSubscription within the SubscriptionAccess interface.
public class SubscriptionWrapper implements SubscriptionAccess
{
  private IncrementalSubscription sub = null;

  public SubscriptionWrapper (IncrementalSubscription s) {
    sub = s;
  }

  public IncrementalSubscription getSubscription () {
    return sub;
  }

  public Collection getAddedCollection () {
    return sub.getAddedCollection();
  }
  public Collection getChangedCollection () {
    return sub.getChangedCollection();
  }
  public Collection getRemovedCollection () {
    return sub.getRemovedCollection();
  }
  public Collection getMembership () {
    return sub.getCollection();
  }
}
