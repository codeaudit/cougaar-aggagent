package org.cougaar.lib.aggagent.psp;

import java.io.PrintStream;

import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.lib.planserver.PSP_BaseAdapter;

import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.lib.aggagent.util.AdvancedHttpInput;

/**
 *  The AggregationPSP provides external access (via HTTP) to the Aggregation
 *  functionality.  Currently, it supports an HTML user interface with frames.
 */
public class AggregationPSP
    extends PSP_BaseAdapter
    implements PlanServiceProvider
{
  private Object lock = new Object();
  private boolean first_call = true;

  private AggregationPSPInterface htmlInterface = null;
  private AggregationPSPInterface xmlInterface = null;

  private void makeSubscription (PlanServiceContext psc) {
    SessionManager man = new SessionManager(psc.getServerPlugInSupport());
    htmlInterface = new AggregationHTMLInterface(man);
    xmlInterface = new AggregationXMLInterface(man);
    first_call = false;
  }

  public void execute (PrintStream out, HttpInput in, PlanServiceContext psc,
      PlanServiceUtilities psu)
      throws Exception
  {
    synchronized (lock) {
      if (first_call)
        makeSubscription(psc);

      // parse the parameters
      AdvancedHttpInput ahi = new AdvancedHttpInput(in);

      // delegate request to the correct interface
      if (ahi.hasKeyword("THICK_CLIENT"))
      {
        xmlInterface.handleRequest(out, ahi, psc);
      }
      else
      {
        htmlInterface.handleRequest(out, ahi, psc);
      }

      // This call ensures that execute will not be called for this request
      // again.  (workaround for an apparent Cougaar bug)
      in.getBody();
    }
  }

  public String getDTD () { return null; }
  public boolean returnsHTML () { return false; }
  public boolean returnsXML () { return false; }
  public boolean test (HttpInput p0, PlanServiceContext p1) { return false; }
}
