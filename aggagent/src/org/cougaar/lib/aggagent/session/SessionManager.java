package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.QueryResultAdapter;

/**
 *  A SessionManager is a container for Sessions (q.v.), which may be created,
 *  retrieved, or destroyed using the methods of this class.  Three types of
 *  Sessions are currently supported:
 *  <ul>
 *    <li>Passive Sessions -- which report new results only when prompted</li>
 *    <li>PushSessions -- which actively report new data when it arrives</li>
 *    <li>PullSessions -- which is a client-side device for prompting data flow</li>
 *  </ul>
 */
public class SessionManager {
  public static boolean debug = false;

  private static long PUSH_INTERVAL = 2000;

  private ServerPlugInSupport spis = null;
  private HashMap sessions = new HashMap();
  private int id_counter = 0;

  // maybe these could be the same?
  private Timer pullTimer = new Timer();
  private Timer pushTimer = new Timer();

  public SessionManager (ServerPlugInSupport s) {
    spis = s;
  }

  public Set getKeys () {
    return sessions.keySet();
  }

  public String addSession (
      UnaryPredicate p, IncrementFormat f, String queryId)
  {
    String k = String.valueOf(id_counter++);
    Session s = new Session(k, queryId, f);
    s.start(spis, p);

    sessions.put(k, s);

    return k;
  }

  public String addPushSession (
      UnaryPredicate p, IncrementFormat f, URL url, String queryId)
  {
    return addPushSession(p, f, url, queryId, PUSH_INTERVAL);
  }

  public String addPushSession (UnaryPredicate p, IncrementFormat f, URL url,
      String queryId, long interval)
  {
    String k = String.valueOf(id_counter++);
    PushSession s = new PushSession(k, queryId, url, f);
    s.setTimingModel(new PushTimingModel(pushTimer, s, interval));
    s.start(spis, p);

    sessions.put(k, s);

    return k;
  }

  public String addPullSession (String requesterId, QueryResultAdapter qra,
                                BlackboardService bs, NameService nameServer)
  {
    String k = String.valueOf(id_counter++);
    PullSession s = new PullSession(k, requesterId, qra, bs, nameServer);
    sessions.put(k, s);
    if (qra.getQuery().getPullRate() >= 0)
    {
      long waitPeriod = (long)(qra.getQuery().getPullRate() * 1000);
      pullTimer.scheduleAtFixedRate(s.getTimerTask(), 0, waitPeriod);
    }

    return k;
  }

  public void cancelSession (String k) {
    if (debug)
    {
      System.out.println(
        "SessionManager::cancelSession:  called on \"" + k + "\"");
    }
    Session sess = (Session) sessions.remove(k);
    sess.endSession();
  }


  private Session getSession (String k) {
    return (Session) sessions.get(k);
  }

  public void sendUpdate(String k, OutputStream out) {
    Session s = getSession(k);
    if (s != null)
      s.sendUpdate(out);
  }
}
