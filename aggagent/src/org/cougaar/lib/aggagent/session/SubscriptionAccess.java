package org.cougaar.lib.aggagent.session;

import java.util.Collection;

/**
 *  This interface provides access to incremental changes using the methods
 *  of IncrementalSubscription.  Mainly, this is used for converting Objects
 *  found on the blackboards of society agents into XML for transfer to
 *  aggregation agents.  Various subscription or subscription-like entities may
 *  implement SubscriptionAccess so that their contents may be transferred in
 *  this fashion.
 */
public interface SubscriptionAccess {
  public Collection getAddedCollection ();
  public Collection getChangedCollection ();
  public Collection getRemovedCollection ();
  public Collection getMembership ();
}