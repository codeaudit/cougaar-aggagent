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
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.cluster.*;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.session.Session;

/**
 *  This keep-alive PSP is used for monitoring the aggregation agent's
 *  blackboard by way of incremental updates passed over a keep alive
 *  connection.
 */
public class AggregationKeepAlivePSP extends PSP_BaseAdapter
    implements PlanServiceProvider, KeepAlive
{
  KeepAliveSession kas;
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
    kas =
      new KeepAliveSession(psc.getServerPlugInSupport(),
                           request.unaryPredicate,
                           new XmlIncrement(request.xmlEncoder), out);
    try {
      // this check will EVENTUALLY get triggered after the client drops the
      // connection.  It would be nice if I could receive a cancel message
      // from the client.
      while (!out.checkError())
      {
        System.out.println("---------Keep Alive Session is Alive---------");
        Thread.sleep(5000);
      }
    }
    catch (Exception done_in) {
      System.out.println("AggregationKeepAlivePSP::execute:  aborted!");
    }
    System.out.println("AggregationKeepAlivePSP::execute:  leaving");
  }

  private static class KeepAliveSession extends Session
  {
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
System.out.println("KeepAlivePSP: subscriptionChanged");
      this.data.subscriptionChanged();
      if (out != null)
      {
        sendUpdate(out);
        endMessage(out);
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

  /**
   *  We don't need no stinkin' ACK message.
   */
  public String getConnectionACKMessage () {
    return null;
  }

  /**
   *  And we don't want your stinkin' ACK message, either.
   */
  public void setConnectionACKMessage (String s) {
  }

  // More PSP methods that are basically useless, and are not implemented in
  // any meaningful sense.
  public String getDTD () { return null; }
  public boolean returnsHTML () { return false; }
  public boolean returnsXML () { return false; }
  public boolean test (HttpInput p0, PlanServiceContext p1) { return false; }
}