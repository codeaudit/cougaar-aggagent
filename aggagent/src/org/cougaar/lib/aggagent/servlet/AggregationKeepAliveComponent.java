package org.cougaar.lib.aggagent.servlet;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.*;
import org.cougaar.core.domain.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardService;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.session.ServletSession;
import org.cougaar.lib.aggagent.util.Const;

/**
 *  This keep-alive component is used for monitoring the aggregation agent's
 *  blackboard by way of incremental updates passed over a keep alive
 *  connection.
 */
public class AggregationKeepAliveComponent extends BlackboardServletComponent {
  private Object outputLock = new Object();

  /**
   * Constructor.
   */
  public AggregationKeepAliveComponent()
  {
    super();
    myServlet = new AggregationKeepAliveServlet();
  }

  /**
   * Here is our inner class that will handle all HTTP and
   * HTTPS service requests for our <tt>myPath</tt>.
   */
  private class AggregationKeepAliveServlet extends HttpServlet
  {
    /**
     *  Run a keep-alive session.  This method will continue to send lines of
     *  output to the provided PrintStream until an error is detected.
     */
    public void doPut(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
      System.out.println("AggKeepAliveServlet: doPut");

      PrintWriter out = response.getWriter();

      try {
        AggregationXMLInterface.MonitorRequestParser monitorRequest =
          new AggregationXMLInterface.MonitorRequestParser(request);
        new KeepAliveSession(agentId.toString(),
                             blackboard, createSubscriptionSupport(),
                             monitorRequest.unaryPredicate,
                             new XmlIncrement(monitorRequest.xmlEncoder), out);

        // this check will EVENTUALLY get triggered after the client drops the
        // connection.  It would be nice if I could receive a cancel message
        // from the client.
        while (!out.checkError())
        {
          System.out.println("---------Keep Alive Session is Alive---------");
          Thread.sleep(5000);

          // Without an ack message, a keep alive connection over which no
          // updates are sent will never die
          // (even without a client on the other end).
          synchronized (out)
          {
            out.print(Const.KEEP_ALIVE_ACK_MESSAGE);
            KeepAliveSession.endMessage(out);
          }
        }
      }
      catch (Exception done_in) {
        System.out.println("AggregationKeepAliveServlet::doPut:  aborted!");
      }
      System.out.println("AggregationKeepAliveServlet::doPut:  leaving");
    }
  }

  private static class KeepAliveSession extends ServletSession {
    PrintWriter out = null;

    KeepAliveSession(String agentId, BlackboardService blackboard,
                     SubscriptionMonitorSupport sms,
                     UnaryPredicate predicate, IncrementFormat format,
                     PrintWriter out)
    {
      super("", "", format);
      synchronized (out)
      {
        this.out = out;
        start(agentId, blackboard, sms, predicate);
      }
    }

    /**
     *  This method is called by the resident RemoteSubscription whenever new
     *  subscription information is available.
     */
    public void subscriptionChanged (Subscription sub) {
      synchronized (lock) {
        data.subscriptionChanged();
        if (out != null) {
          synchronized (out) {
            sendUpdate(out);
            endMessage(out);
          }
        }
      }
    }

    private static void endMessage(PrintWriter out)
    {
      try {
        out.write('\f');
        out.flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}