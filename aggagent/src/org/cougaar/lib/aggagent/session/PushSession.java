package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.net.*;
import java.util.*;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.util.XmlUtils;

public class PushSession extends Session {
  public static boolean debug = false;
  private URL client = null;
  private PushTimingModel timer = null;

  /**
   *  Create a new Session that pushes information to the client.
   */
  public PushSession (String k, String queryId, URL url, IncrementFormat f) {
    super(k, queryId, f);
    client = url;
  }

  public void setTimingModel (PushTimingModel ptm) {
    timer = ptm;
  }

  /**
   *  End this session and let it halt all active and passive functions.
   */
  public void endSession () {
    // order is important here.  Make sure the timer is killed before ending
    // the session; otherwise it may attempt to send while the Session is dead.
    if (timer != null)
      timer.cancel();

    super.endSession();
  }

  /**
   *  This method is a notification from the contained RemoteSession that new
   *  information is available.  This class reports immediately if the timing
   *  model deems it ready.  Otherwise, it waits for another event.
   */
  public void subscriptionChanged (Subscription sub) {
    if (timer == null)
      pushUpdate();
    else
      timer.schedule();
  }

  /**
   *  Report recent events to the client via HTTP post.
   */
  public void pushUpdate () {
    try {
      // open a connection to the "client"
      HttpURLConnection conn = (HttpURLConnection) client.openConnection();
      conn.setDoOutput(true);
      conn.setDoInput(true);

      // transmit the data
      OutputStream out = conn.getOutputStream();
      sendUpdate(out);
      out.close();

      // read the response
      String response = XmlUtils.readToString(conn.getInputStream());

      if (debug)
      {
        System.out.println(
          "PushSession::sendUpdate:  \"" + getKey() + "\" received response:");
        System.out.println(response);
      }
    }
    catch (Exception eek) {
      System.out.println("PushSession::sendUpdate:  Error contacting client");
      eek.printStackTrace();
    }
  }
}