package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.QueryResultAdapter;

/**
 *  A SessionManager is a container for Sessions (q.v.), which may be created,
 *  retrieved, or destroyed using the methods of this class.  Currently, only
 *  one type of Session is supported, that being a passive Session that reports
 *  results only when prompted.  Support for other reporting strategies may be
 *  provided elsewhere.
 */
public class SessionManager {
  public static boolean debug = false;

  private ServerPlugInSupport spis = null;
  private HashMap sessions = new HashMap();
  private int id_counter = 0;

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
    PSPSession s = new PSPSession(k, queryId, f);
    s.start(spis, p);

    sessions.put(k, s);

    return k;
  }

  public void cancelSession (String k) {
    if (debug)
    {
      System.out.println(
        "SessionManager::cancelSession:  called on \"" + k + "\"");
    }
    PSPSession sess = (PSPSession) sessions.remove(k);
    sess.endSession();
  }


  private PSPSession getSession (String k) {
    return (PSPSession) sessions.get(k);
  }

  public void sendUpdate(String k, OutputStream out) {
    PSPSession s = getSession(k);
    if (s != null)
      s.sendUpdate(out);
  }
}
