package org.cougaar.lib.aggagent.psp;

import java.io.*;
import java.util.*;

import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.KeepAlive;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.cluster.*;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.session.RemotePSPSubscription;
import org.cougaar.lib.aggagent.session.Session;

/**
 *  This keep-alive PSP is used for monitoring the aggregation agent's
 *  blackboard by way of incremental updates passed over a keep alive
 *  connection.
 */
public class AggregationKeepAlivePSP extends PSP_BaseAdapter
    implements PlanServiceProvider, KeepAlive
{
  /**
   *  Run a keep-alive session.  This method will continue to send lines of
   *  output to the provided PrintStream until an error is detected.
   */
  public void execute (PrintStream out, HttpInput in, PlanServiceContext psc,
      PlanServiceUtilities psu)
      throws Exception
  {
System.out.println("AggKeepAlivePSP: execute");
    AggregationXMLInterface.MonitorRequestParser request =
      new AggregationXMLInterface.MonitorRequestParser(in);
    new KeepAliveSession(psc.getServerPlugInSupport(), request.unaryPredicate,
      new XmlIncrement(request.xmlEncoder), out);

    try {
      // this check will EVENTUALLY get triggered after the client drops the
      // connection.  It would be nice if I could receive a cancel message
      // from the client.
      while (!out.checkError())
      {
        System.out.println("---------Keep Alive Session is Alive---------");
        Thread.sleep(10000);
      }
    }
    catch (Exception done_in) {
      System.out.println("AggregationKeepAlivePSP::execute:  aborted!");
    }
    System.out.println("AggregationKeepAlivePSP::execute:  leaving");
  }

  private static class KeepAliveSession extends Session {
    OutputStream out = null;

    KeepAliveSession(ServerPlugInSupport spis, UnaryPredicate predicate,
                     IncrementFormat format, OutputStream out)
    {
      super("", "", format);
      start(spis, predicate);
      this.out = out;
      sendUpdate(out);
      endMessage(out);
    }

    /**
     *  This method is called by the resident RemoteSubscription whenever new
     *  subscription information is available.
     */
    public void subscriptionChanged (Subscription sub) {
      synchronized (lock) {
        data.subscriptionChanged();
        if (out != null) {
          sendUpdate(out);
          endMessage(out);
        }
      }
    }
  }

  private static void endMessage(OutputStream out)
  {
    try {
      out.write('\f');
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Without an ack message, a keep alive connection over which no updates are
  // sent will never die (even without a client on the other end).
  public static
    String ackMessage = "I M   A L I V E ,   A L L   I S   W E L L";
  private static String terminatedAckMessage = ackMessage + '\f';

  /**
   *  Provide ack message to periodically send to keep alive client.
   */
  public String getConnectionACKMessage () {
    return terminatedAckMessage;
  }

  /**
   *  This is never called.
   */
  public void setConnectionACKMessage (String s) {
    System.out.println("ACK set to " + s);
    ackMessage = s;
    terminatedAckMessage = ackMessage + '\f';
  }

  public String getDTD () { return null; }
  public boolean returnsHTML () { return false; }
  public boolean returnsXML () { return false; }
  public boolean test (HttpInput p0, PlanServiceContext p1) { return false; }
}