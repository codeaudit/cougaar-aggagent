package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.core.cluster.*;
import org.cougaar.util.*;
import org.cougaar.core.blackboard.*;

/**
 *  <p>
 *  A RemoteSubscription is a mechanism that allows remote clients to collect
 *  information from a Cluster in much the same way as one of its resident
 *  PlugIns.  Instances of this class behave like an IncrementalSubscription,
 *  that is, they accumulate and incrementally report lists of blackboard
 *  objects that have been added, removed, or modified.  However, their
 *  reporting model is not tied directly to the Cluster's event thread.
 *  </p><p>
 *  Each RemoteSubscription is backed-up by an IncrementalSubscription, which
 *  actually gets information from the blackboard.
 *  </p><p>
 *  Access to incremental information is granted during a reporting
 *  transaction, which is started by calling open() and ended by calling
 *  close().  An IllegalStateException may be raised if the expected protocol
 *  is not followed.
 *  </p><p>
 *  Since the constructors use a ServerPlugInSupport reference to create the
 *  subscription (and UISubscriber interface for call-backs), the use of this
 *  class is apparently restricted to PSPs, which is probably where they will
 *  be the most useful.
 *  </p>
 */
public class RemoteBlackboardSubscription implements SubscriptionAccess {
  private Object lock = new Object();
  private boolean transientQuery = false;
  private boolean opened = false;
  private boolean hasNewStuff = false;
  private boolean dead = false;

  protected IncrementalSubscription subs = null;
  protected BlackboardService bbs;

  private Set added = null;
  private Set changed = null;
  private Set removed = null;

  private Set newAdds = new HashSet();
  private Set newChanges = new HashSet();
  private Set newRemoves = new HashSet();


  /**
   *  Create a new RemoteSubscription to gather Objects matching the given
   *  predicate.  The ServerPlugInSupport reference allows the underlying
   *  IncrementalSubscription to be created.  No listening Session is assigned
   *  initially.
   */
  public RemoteBlackboardSubscription (BlackboardService s, UnaryPredicate p) {
    this(s, p, false);
  }


  /**
   * Used for transient queries.  Fill added collection with the results
   * of a one-time query.
   */
  public RemoteBlackboardSubscription (BlackboardService s, UnaryPredicate p,
                             boolean transientQuery)
  {
    this.transientQuery = transientQuery;
    bbs = s;

    subs = (IncrementalSubscription) bbs.subscribe(p);
    add(subs.getCollection());
  }

  /**
   *  Unsubscribe from the Cluster's blackboard and destroy the underlying
   *  subscription.  After this method is called, the RemoteSubscription is no
   *  longer operational (all of the client interface methods will throw
   *  IllegalStateException).
   */
  public void shutDown () {
    synchronized (lock) {
      bbs.unsubscribe(subs);
      subs = null;
      dead = true;
    }
  }

  private void checkDead (String s) {
    if (dead)
      throw new IllegalStateException(
        "operation " + s + " is illegal after shutDown");
  }

  private void checkOpened (String s) {
    if (opened)
      throw new IllegalStateException(
        "operation " + s + " is illegal -- a transaction is in progress");
  }

  private void checkClosed (String s) {
    if (!opened)
      throw new IllegalStateException(
        "operation " + s + " is illegal -- must open a transaction first");
  }

  /**
   *  Tell whether unreported changes to the subscription have been posted.
   *  This operation is not legal during reporting transactions.
   */
  public boolean hasChanged () {
    synchronized (lock) {
      checkDead("hasChanged");
      checkOpened("hasChanged");
      return hasNewStuff;
    }
  }

  /**
   *  Begin a reporting transaction.  Lists of added, changed, and removed
   *  blackboard objects are constructed and held constant until the end of
   *  the transaction, as marked by a call to the close method.  An
   *  IllegalStateException is raised if this method is called while a
   *  transaction is already in progress.  Clients attempting to open a
   *  reporting transaction should catch this Exception and refrain from any
   *  calls to methods "getAddedCollection", "getChangedCollection",
   *  "getRemovedCollection", and "close" until such a time as the open method
   *  is allowed to succeed.
   */
  public void open () {
System.out.println("RBS: open");
    synchronized (lock) {
      checkDead("open");
      checkOpened("open");

      opened = true;
      added = newAdds;
      changed = newChanges;
      removed = newRemoves;
      newAdds = new HashSet();
      newChanges = new HashSet();
      newRemoves = new HashSet();
      hasNewStuff = false;
    }
  }

  /**
   *  End a reporting transaction.  Lists of added, changed, and removed
   *  blackboard objects are flushed so that they may be refilled when a new
   *  transaction is started by the open method.  Calls to this method while
   *  there is no transaction in progress have no effect.
   */
  public void close () {
System.out.println("RBS: close");
    synchronized (lock) {
      opened = false;
      added = null;
      changed = null;
      removed = null;
    }
  }

  /**
   *  This method is legal only during a reporting transaction; calling it at
   *  another time raises an IllegalStateException.  A Collection view of the
   *  list of blackboard Objects added since the start of the last transaction
   *  is returned.
   */
  public Collection getAddedCollection () {
    synchronized (lock) {
      checkDead("getAddedCollection");
      checkClosed("getAddedCollection");
      return new HashSet(added);
    }
  }

  /**
   *  This method is legal only during a reporting transaction; calling it at
   *  another time raises an IllegalStateException.  A Collection view of the
   *  list of blackboard Objects changed since the start of the last
   *  transaction is returned.
   */
  public Collection getChangedCollection () {
    synchronized (lock) {
      checkDead("getChangedCollection");
      checkClosed("getChangedCollection");
      return new HashSet(changed);
    }
  }

  /**
   *  This method is legal only during a reporting transaction; calling it at
   *  another time raises an IllegalStateException.  A Collection view of the
   *  list of blackboard Objects removed since the start of the last
   *  transaction is returned.
   */
  public Collection getRemovedCollection () {
    synchronized (lock) {
      checkDead("getRemovedCollection");
      checkClosed("getRemovedCollection");
      return new HashSet(removed);
    }
  }

  /**
   *  Obtain a Collection view of all blackboard Objects matching the predicate
   *  of this RemoteSubscription.  The underlying IncrementalSubscription is
   *  queried for its contents, which are not necessarily synchronized with the
   *  reporting model maintained by this class.  Calling this method is legal
   *  both inside and outside of reporting transactions.
   */
  public Collection getMembership () {
    checkDead("getMembership");
    if (transientQuery)
      return new HashSet(added);
    return new HashSet(subs);
  }

  private void add (Collection c) {
    synchronized (lock) {
      newAdds.addAll(c);
      newRemoves.removeAll(c);
      newChanges.removeAll(c);
    }
  }

  private void change (Collection c) {
    synchronized (lock) {
      newChanges.addAll(c);
      newChanges.removeAll(newAdds);
      newChanges.removeAll(newRemoves);
    }
  }

  private void remove (Collection c) {
    synchronized (lock) {
      newRemoves.addAll(c);
      newAdds.removeAll(c);
      newChanges.removeAll(c);
    }
  }

  /**
   *  <p>
   *  Implementation of the UISubscriber interface.  This method is called by
   *  the Cluster when it has posted updates to the Subscription underlying
   *  this RemoteSubscription.  The Subscription argument required by the
   *  interface is ignored, and it is assumed that the changes are relevant to
   *  the Subscription managed locally.
   *  </p><p>
   *  If this method should happen to be called after shutDown, it is ignored.
   *  </p>
   */
  public void subscriptionChanged (Subscription ignored) {
System.out.println("RBS: Subscription changed "+
subs.getAddedCollection().size()+":"+
subs.getChangedCollection().size()+":"+
subs.getRemovedCollection().size());
    synchronized (lock) {
      if (dead) {
        System.out.println(
          "RemoteSubscription::subscriptionChanged:  ignored (never appear).");
      }
      else {
        add(subs.getAddedCollection());
        change(subs.getChangedCollection());
        remove(subs.getRemovedCollection());
        hasNewStuff = true;
      }
    }
  }

  public IncrementalSubscription getSubscription() {
    return subs;
  }
}