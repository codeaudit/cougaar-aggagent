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
package org.cougaar.lib.aggagent.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.RemoteSession;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;
import org.cougaar.lib.aggagent.session.SubscriptionWrapper;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.util.Const;
import org.cougaar.util.UnaryPredicate;

/**
 *  This keep-alive component is used for monitoring the aggregation agent's
 *  blackboard by way of incremental updates passed over a keep alive
 *  connection.
 */
public class AggregationKeepAliveComponent extends BlackboardServletComponent {
  private Map sessionMap = new HashMap();
  private int sessionCounter = 0;

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
      if (log.isDebugEnabled()) log.debug("doPut");

      // check if this is a cancel request
      String cancelSessionId = request.getParameter("CANCEL_SESSION_ID");
      if (cancelSessionId != null)
      {
        synchronized (sessionMap)
        {
            sessionMap.put(cancelSessionId, Boolean.TRUE);
        }
        return; // done canceling session
      }

      //
      // Handle Keep Alive Session Request
      //
      PrintWriter out = new PrintWriter(response.getOutputStream());
      KeepAliveSession kaSession = null;

      // establish session id, send to client
      String thisSession;
      synchronized (sessionMap)
      {
        thisSession = String.valueOf(sessionCounter++);
        sessionMap.put(thisSession, new Boolean(false));
      }
      synchronized (out)
      {
        out.println("<session_created id=\"" + thisSession + "\" />");
        endMessage(out);
      }

      try {
        AggregationXMLInterface.MonitorRequestParser monitorRequest =
          new AggregationXMLInterface.MonitorRequestParser(request);
        kaSession = new KeepAliveSession(agentId.toString(),
                             blackboard, createSubscriptionSupport(),
                             monitorRequest.unaryPredicate,
                             new XmlIncrement(monitorRequest.xmlEncoder), out);

        boolean outputError = false;
        boolean sessionCanceled = false;

        while ((!outputError) && (!sessionCanceled))
        {
          if (log.isDebugEnabled()) log.debug("---------Keep Alive Session " + 
                  thisSession +" is Alive---------");

          Thread.sleep(5000);

          // Without an ack message, a keep alive connection over which no
          // updates are sent will never die
          // (even without a client on the other end).
          synchronized (out)
          {
            out.print(Const.KEEP_ALIVE_ACK_MESSAGE);
            endMessage(out);
            outputError= out.checkError();
          }

          synchronized (sessionMap)
          {
            sessionCanceled =
                ((Boolean)sessionMap.get(thisSession)).booleanValue();
          }
        }
      }
      catch (Exception done_in) {
        if (log.isDebugEnabled()) log.debug("doPut:  aborted!");
      }
      finally {
        kaSession.cancel();
        synchronized (sessionMap)
        {
          sessionMap.remove(thisSession);
        }
          if (log.isDebugEnabled()) log.debug("doPut:  leaving");
        }
    }
  }

  private class KeepAliveSession extends RemoteSession
    implements SubscriptionListener
  {
    PrintWriter out = null;
    Subscription rawData = null;
    SubscriptionAccess data = null;
    SubscriptionMonitorSupport sms = null;

    KeepAliveSession(String agentId, BlackboardService blackboard,
                     SubscriptionMonitorSupport sms,
                     UnaryPredicate predicate, IncrementFormat format,
                     PrintWriter out)
    {
      super("", "", format);
      setAgentId(agentId);
      this.out = out;
      this.sms = sms;

      // This is a separate transaction from the one that calls
      // subscriptionChanged.  No additional synchronization is necessary.
      try {
        blackboard.openTransaction();
        rawData = blackboard.subscribe(predicate, true);
        sms.setSubscriptionListener(rawData, this);
        data = new SubscriptionWrapper((IncrementalSubscription)rawData);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        blackboard.closeTransactionDontReset();
      }
    }

    public SubscriptionAccess getData()
    {
      return data;
    }

    /**
     *  This method is called by the plugin component's execute() whenever new
     *  subscription information is available.
     *
     *  (since it is called from BlackboardServletComponent.execute() method;
     *   it is in a separate blackboard transaction from the constructor and
     *   cancel method)
     */
    public void subscriptionChanged (Subscription sub) {
      if (out != null) {
        synchronized (out) {
          sendUpdate(out);
          endMessage(out);
        }
      }
    }

    /**
     *  Send an update of recent changes to the resident Subscription
     *  through the provided OutputStream.  An IncrementFormat instance
     *  is used to encode the data being sent.
     *
     *  This is called from the execute() method, don't open a transaction.
     */
    public void sendUpdate (PrintWriter out) {
      out.println(createUpdateDelta().toXml());
      out.flush();
    }

    public void cancel () {
      sms.removeSubscriptionListener(rawData);
      try {
        blackboard.openTransaction();
        blackboard.unsubscribe(rawData);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        blackboard.closeTransactionDontReset();
      }
    }
  }

  private static void endMessage(PrintWriter out)
  {
    out.print('\f');
    out.flush();
  }
}