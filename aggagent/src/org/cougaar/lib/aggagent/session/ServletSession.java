/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.lib.aggagent.session;

import java.io.PrintWriter;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.lib.aggagent.servlet.SubscriptionListener;
import org.cougaar.lib.aggagent.servlet.SubscriptionMonitorSupport;
import org.cougaar.util.UnaryPredicate;

/**
 *  A Session is a handler for instances of the RemoteBlackboardSubscription
 *  class.  In the context of a Servlet, this class can be used to manage
 *  remote access to the local Blackboard through the
 *  RemoteBlackboardSubscription API.
 */
public class ServletSession extends RemoteSession
    implements SubscriptionListener {
  protected Object lock = new Object();
  protected RemoteBlackboardSubscription data = null;

  /**
   *  Create a new Session with the specified session ID to search the
   *  blackboard for Objects matching the predicate given.
   */
  public ServletSession (String k, String queryId, IncrementFormat f) {
    super(k, queryId, f);
  }

  /**
   *  Create the RemoteSubscription instance to be managed by this Session.
   *  Once this method is called, subscriptionChanged() events may start
   *  arriving.
   */
  public void start (String agentId, BlackboardService b,
                     SubscriptionMonitorSupport sms, UnaryPredicate p) {
    synchronized (lock) {
      setAgentId(agentId);
      try {
        b.openTransaction();
        data = new RemoteBlackboardSubscription(b, p);
        sms.setSubscriptionListener(data.getSubscription(), this);
      } finally {
        b.closeTransactionDontReset();
      }
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
  public void sendUpdate (PrintWriter out) {
    UpdateDelta del = null;
    synchronized (lock) {
      data.open();
      del = createUpdateDelta();
      data.close();
    }
    out.println(del.toXml());
    out.flush();
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