/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.servlet.SubscriptionMonitorSupport;

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
