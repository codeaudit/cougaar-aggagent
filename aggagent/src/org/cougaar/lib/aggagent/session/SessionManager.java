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
import java.util.HashMap;
import java.util.Set;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.lib.aggagent.servlet.SubscriptionMonitorSupport;
import org.cougaar.util.UnaryPredicate;

/**
 *  A SessionManager is a container for Sessions (q.v.), which may be created,
 *  retrieved, or destroyed using the methods of this class.  Currently, only
 *  one type of Session is supported, that being a passive Session that reports
 *  results only when prompted.  Support for other reporting strategies may be
 *  provided elsewhere.
 */
public class SessionManager {
  public static boolean debug = false;

  private String agentId = null;
  private BlackboardService blackboard = null;
  private SubscriptionMonitorSupport sms = null;
  private HashMap sessions = new HashMap();
  private int id_counter = 0;

  public SessionManager (String agentId, BlackboardService blackboard,
                         SubscriptionMonitorSupport sms) {
    this.agentId = agentId;
    this.blackboard = blackboard;
    this.sms = sms;
  }

  public Set getKeys () {
    return sessions.keySet();
  }

  public String addSession (
      UnaryPredicate p, IncrementFormat f, String queryId)
  {
    String k = String.valueOf(id_counter++);
    ServletSession s = new ServletSession(k, queryId, f);
    s.start(agentId, blackboard, sms, p);

    sessions.put(k, s);

    return k;
  }

  public void cancelSession (String k) {
    if (debug)
    {
      System.out.println(
        "SessionManager::cancelSession:  called on \"" + k + "\"");
    }
    ServletSession sess = (ServletSession) sessions.remove(k);
    sess.endSession();
  }

  private ServletSession getSession (String k) {
    return (ServletSession) sessions.get(k);
  }

  public void sendUpdate(String k, PrintWriter out) {
    ServletSession s = getSession(k);
    if (s != null)
      s.sendUpdate(out);
  }
}
