
package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.lib.planserver.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  A Session is a handler for instances of the RemotePSPSubscription class.
 *  In the context of a PSP, this class can be used to manage remote access to
 *  the local Blackboard through the RemotePSPSubscription API.
 */
public class PSPSession extends RemoteSession implements UISubscriber {
  protected Object lock = new Object();
  protected RemotePSPSubscription data = null;

  /**
   *  Create a new Session with the specified session ID to search the
   *  blackboard for Objects matching the predicate given.  The
   *  ServerPlugInSupport argument is included so that the RemoteSubscription
   *  may be created.
   */
  public PSPSession (String k, String queryId, IncrementFormat f) {
    super(k, queryId, f);
  }

  /**
   *  Create the RemoteSubscription instance to be managed by this Session.
   *  Once this method is called, subscriptionChanged() events may start
   *  arriving.
   */
  public void start (ServerPlugInSupport s, UnaryPredicate p) {
    synchronized (lock) {
      setAgentId(s.getClusterIDAsString());
      data = new RemotePSPSubscription(s, p, this);
    }
  }

  /**
   *  Get the SubscriptionAccess implementation containing the data to be
   *  encoded and sent to a client.  In the case of a PSPSession, we have a
   *  RemotePSPSubscription.
   */
  protected SubscriptionAccess getData () {
    return data;
  }

  /**
   *  End this session and let it halt all active and passive functions.
   */
  public void endSession () {
    data.shutDown();
  }

  /**
   *  Check to see whether new information has been gathered since the last
   *  report.
   */
  public boolean hasChanged () {
    return data.hasChanged();
  }

  /**
   *  Send an update of recent changes to the resident RemoteSubscription
   *  through the provided OutputStream.  An IncrementFormat instance is used
   *  to encode the data being sent.
   */
  public void sendUpdate (OutputStream out) {
    UpdateDelta del = null;
    synchronized (lock) {
      data.open();
      del = createUpdateDelta();
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