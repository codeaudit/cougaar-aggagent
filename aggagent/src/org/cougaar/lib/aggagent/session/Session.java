package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.core.cluster.*;
import org.cougaar.lib.planserver.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.UpdateDelta;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  A Session is a handler for instances of the RemotePSPSubscription class.
 *  In the context of a PSP, this class can be used to manage remote access to
 *  the local Blackboard through the RemotePSPSubscription API.
 */
public class Session implements UISubscriber {
  protected Object lock = new Object();

  private String key = null;
  private String queryId = null;
  protected String clusterId = null;
  private IncrementFormat sender = null;

  protected RemotePSPSubscription data = null;

  /**
   *  Create a new Session with the specified session ID.  This constructor
   *  should only be called by subclasses, where other required initializations
   *  will be implemented.
   */
  protected Session (String k, String queryId) {
    key = k;
    this.queryId = queryId;
  }

  /**
   *  Create a new Session with the specified session ID to search the
   *  blackboard for Objects matching the predicate given.  The
   *  ServerPlugInSupport argument is included so that the RemoteSubscription
   *  may be created.
   */
  public Session (String k, String queryId, IncrementFormat f) {
    this(k, queryId);
    sender = f;
  }

  /**
   *  Create the RemoteSubscription instance to be managed by this Session.
   *  Once this method is called, subscriptionChanged() events may start
   *  arriving.
   */
  public void start (ServerPlugInSupport s, UnaryPredicate p) {
    synchronized (lock) {
      clusterId = s.getClusterIDAsString();
      data = new RemotePSPSubscription(s, p, this);
    }
  }

  /**
   *  End this session and let it halt all active and passive functions.
   */
  public void endSession () {
    data.shutDown();
  }

  /**
   *  For purposes of tabulation, this key identifies the session existing
   *  between this Object and the remote client.  Requests concerning the
   *  session (such as ending it, checking its status, etc.) should use this
   *  key.
   */
  public String getKey () {
    return key;
  }

  /**
   *  Check to see whether new information has been gathered since the last
   *  report.
   */
  public boolean hasChanged () {
    return data.hasChanged();
  }

  /**
   *  Specify the IncrementFormat (q.v.) used to transmit data gathered by the
   *  RemoteSubscription for this Session.
   */
  public void setIncrementFormat (IncrementFormat f) {
    sender = f;
  }

  /**
   *  Send an update of recent changes to the resident RemoteSubscription
   *  through the provided OutputStream.  An IncrementFormat instance is used
   *  to encode the data being sent.
   */
  public void sendUpdate (OutputStream out) {
    UpdateDelta del = new UpdateDelta(clusterId, queryId, key);
    synchronized (lock) {
      data.open();
      try {
        sender.encode(del, data);
      } catch (Exception e) {
        XmlUtils.sendException(queryId, clusterId, e, new PrintStream(out));
      }
      data.close();
    }
    PrintStream ps = new PrintStream(out);
    ps.println(del.toXml());
    ps.flush();
  }

  /**
   *  Implementation of the UISubscriber interface.  When the COUGAAR agent has
   *  changes to report, it will call this method so that the Session can
   *  respond accordingly.  The only action included by default is to call the
   *  subscriptionChanged() method of the underlying RemotePSPSubscription.
   *  Subclasses may wish to take additional steps, such as sending notices to
   *  other agents, etc.
   *  <br><br>
   *  The Subscription argument is ignored, as the relevant Subscription is
   *  presumed to be the one encapsulated within the RemotePSPSubscription.
   */
  public void subscriptionChanged (Subscription sub) {
    synchronized (lock) {
      data.subscriptionChanged();
    }
  }
}
