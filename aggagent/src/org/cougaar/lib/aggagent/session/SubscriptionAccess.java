package org.cougaar.lib.aggagent.session;

import java.util.Collection;

public interface SubscriptionAccess {
  public Collection getAddedCollection ();
  public Collection getChangedCollection ();
  public Collection getRemovedCollection ();
  public Collection getMembership ();
}