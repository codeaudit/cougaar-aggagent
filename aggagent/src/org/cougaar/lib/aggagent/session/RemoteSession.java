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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.cougaar.util.UnaryPredicate;

/**
 *  A RemoteSession represents a contract for a COUGAAR agent to provide data
 *  to another based on a Subscription to the local blackboard.  The primary
 *  purpose of this class is to provide standard algorithms for handling a
 *  SubscriptionAccess and any errors in the UnaryPredicate or IncrementFormat
 *  scripts.
 */
public abstract class RemoteSession implements Serializable {
  // collecting error reports
  private Collection newErrors = new LinkedList();

  // identify this session
  private String key = null;

  // identify the query that spawned this session
  private String queryId = null;

  // format the output for transmission
  private IncrementFormat formatter = null;

  // ID of the local COUGAAR agent
  private String agentId = null;

  /**
   *  Create a new RemoteSession instance.  It carries an ID for itself and
   *  the associated client-side query.  It also has an IncrementFormat for
   *  encoding data sets to return to the client and a String identifier
   *  indicating which agent initiated this session (and to whom responses
   *  should be sent).
   */
  protected RemoteSession (String k, String q, IncrementFormat f) {
    key = k;
    queryId = q;
    formatter = f;
  }

  /**
   *  Specify the name of the local COUGAAR agent.
   */
  protected void setAgentId (String a) {
    agentId = a;
  }

  /**
   *  Report the query ID associated with this session.  This method is useful
   *  for looking up sessions by client-supplied ID.
   */
  public String getQueryId () {
    return queryId;
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
   *  Get the SubscriptionAccess implementation containing the data to be
   *  encoded and sent to a client.  Different concrete subclasses will
   *  probably manage different types of these.
   */
  protected abstract SubscriptionAccess getData ();

  /**
   *  Encode the data contained in the local SubscriptionAccess and send the
   *  results back to the client.  If an error is detected during the gathering
   *  process, then an error report is returned in lieu of a data set.
   */
  protected UpdateDelta createUpdateDelta () {
    UpdateDelta del = new UpdateDelta(agentId, queryId, key);

    Iterator errors = getErrorCollection().iterator();
    if (errors.hasNext()) {
      del.setErrorReport((Throwable) errors.next());
    }
    else {
      try {
        formatter.encode(del, getData());
      }
      catch (Throwable err) {
        if (err instanceof ThreadDeath)
          throw (ThreadDeath) err;
        del.setErrorReport(err);
      }
    }

    return del;
  }

  // called during update processing--return errors and clear them out
  private Collection getErrorCollection () {
    Collection errors = newErrors;
    newErrors = new LinkedList();
    return errors;
  }

  // called by the ErrorTrapPredicate to record errors
  private void error (Throwable err) {
    newErrors.add(err);
  }

  /**
   *  This UnaryPredicate implementation is used as a wrapper for predicates
   *  derived from possibly buggy scripts.  Whenever an Exception or Error is
   *  encountered, it is recorded for later examination.  ThreadDeath is,
   *  of course, exempted from this treatment.  ErrorTrapPredicate::execute
   *  always returns false if an error is detected.
   *  Note:  It may be desirable to include some information about the object
   *  on which the predicate failed.  This may be added in the future.
   */
  protected class ErrorTrapPredicate implements UnaryPredicate {
    private UnaryPredicate delegate = null;

    public ErrorTrapPredicate (UnaryPredicate p) {
      delegate = p;
    }

    public boolean execute (Object o) {
      try {
        return delegate.execute(o);
      }
      catch (Throwable err) {
        if (err instanceof ThreadDeath)
          throw (ThreadDeath) err;
        error(err);
      }
      return false;
    }
  }
}