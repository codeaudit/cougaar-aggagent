package org.cougaar.lib.aggagent.psp;

import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.lib.aggagent.session.HtmlIncrement;
import org.cougaar.lib.aggagent.util.AdvancedHttpInput;
import org.cougaar.lib.aggagent.util.Enum.*;

import org.cougaar.lib.aggagent.test.CycleSizeAlert;

public class AggregationHTMLInterface extends AggregationPSPInterface
{
  public AggregationHTMLInterface () {
  }

  public void handleRequest(PrintStream out, AdvancedHttpInput ahi,
                            PlanServiceContext psc)
  {
    // print the header
    out.println("<html><head><title>Aggregation PSP</title></head>");

    Vector v = ahi.getHttpInput().getURLParameters();
    if (v.size() == 0 || v.firstElement().equals("null"))
    {
      sendMainFrame(out);
    }
    else
    {
      out.println("<body BGCOLOR=\"#EEEEEE\">");

      // check the parameters and decide what to do
      if (ahi.hasParameter("CREATE_QUERY"))
        publishHTMLQuery(ahi, psc, out);
      else if (ahi.hasParameter("CREATE_QUERY_FORM"))
        HTMLPresenter.sendQueryForm(selfName, out, true);
      else if (ahi.hasParameter("CREATE_TRAN_QUERY_FORM"))
        HTMLPresenter.sendQueryForm(selfName, out, false);
      else if (ahi.hasKeyword("REPORT_QUERY"))
        generateQueryReport(ahi, out, psc);
      else if (ahi.hasKeyword("CANCEL_QUERY"))
        removeQuery(ahi, out, psc);
      else if (ahi.hasKeyword("HOME"))
        sendHomePage(out, psc);
      else if (ahi.hasKeyword("TITLE"))
        sendTitle(out);
      else if (ahi.hasKeyword("DEFAULT_ALERT"))
        addDefaultAlert(out, psc);
      else if (ahi.hasKeyword("ACTIVE_ALERTS"))
        checkActiveAlerts(out, psc);
      else if (ahi.hasKeyword("ADD_ALERT_FORM"))
        HTMLPresenter.sendAlertForm(selfName, ahi, out);
      else if (ahi.hasKeyword("ADD_ALERT"))
        processAlertForm(ahi, out, psc);
      else
        sendHomePage(out, psc);

      out.println("</body>");
    }

    // print trailer and flush
    out.println("</html>");
    out.flush();
  }

  private void processAlertForm (
      AdvancedHttpInput ahi, PrintStream out, PlanServiceContext psc)
  {
    ServerPlugInSupport spis = psc.getServerPlugInSupport();

    // create the alert
    AlertDescriptor ad = HTMLPresenter.processAlertForm(ahi);

    // find the indicated query, if any
    Iterator i =
      spis.queryForSubscriber(new QuerySeeker(ad.getQueryId())).iterator();
    if (i.hasNext()) {
      QueryResultAdapter qra = (QueryResultAdapter) i.next();
      try {
        Alert a = ad.createAlert();
        qra.addAlert(a);
        spis.publishAddForSubscriber(a);
        checkActiveAlerts(out, psc);
      } catch(Exception e) {
        out.println("<h2>Alert Request Error</h2>");
        out.println("The Alert could not be constructed.");
        e.printStackTrace(out);
      }
    }
    else {
      out.println("<h2>Alert Request Error</h2>");
      out.println("The query does not exist.  The alert has been cancelled.");
    }
  }

  private void sendMainFrame(PrintStream out) {
    out.println("<FRAMESET ROWS=\"10%, 90%\">");
    out.println("<FRAME NAME=\"title\" SRC=\"" + selfName + "?TITLE\">");
    out.println("<FRAMESET COLS=\"30%, 70%\">");
    out.println("<FRAME NAME=\"menu\" SRC=\"" + selfName + "?HOME\">");
    out.println("<FRAME NAME=\"data\" SRC=\"about:blank\">");
    out.println("</FRAMESET></FRAMESET>");
  }

  private void sendTitle(PrintStream out) {
    out.println("<H1 align=\"center\">Aggregation PSP</H1>");
  }

  private void sendHomePage (PrintStream out, PlanServiceContext psc) {
    out.println("<P><h3>Persistent Queries</h3>");
    summarizeQueries(out, psc);
    out.println(selfLink("Create", "CREATE_QUERY_FORM", null, "data") +
                " new persistent query");
    out.println("<br><br><br>");
    out.println("<P><h3>Transient Queries</h3>");
    out.println(selfLink("Create", "CREATE_TRAN_QUERY_FORM", null, "data") +
                " new transient query");
    out.println("<br><br><br>");
    out.println("<P><h3>Alerts<BR>on Aggregation Agent</h3>");
    out.println(selfLink("Add Default", "DEFAULT_ALERT", null, "menu"));
    out.println("<br><br>");
    out.println(selfLink("Check for Alerts", "ACTIVE_ALERTS", null, "data"));
  }

  //
  //  Check for alerts on the blackboard
  //
  private void checkActiveAlerts (PrintStream out, PlanServiceContext psc) {
    out.println("checking for active alerts ... <br>");
    Collection c = psc.getServerPlugInSupport().queryForSubscriber(
      new AlertSeeker());
    out.println("Found " + c.size() + " alerts:<br><center>");
    HTMLPresenter.sendAlertSummary(c, out);
  }

  //
  //  Add the default Alert-ridden query to the blackboard
  //
  private void addDefaultAlert (PrintStream out, PlanServiceContext psc) {
    QueryResultAdapter qra =
      new QueryResultAdapter(CycleSizeAlert.createDefaultQuery());
    Alert ale = CycleSizeAlert.getDefaultAlert();
    qra.addAlert(ale);
    psc.getServerPlugInSupport().publishAddForSubscriber(qra);
    psc.getServerPlugInSupport().publishAddForSubscriber(ale);
    sendHomePage(out, psc);
  }

  private void publishHTMLQuery (AdvancedHttpInput in, PlanServiceContext psc,
                             PrintStream out) {
    // parse form post request
    AggregationQuery aq = HTMLPresenter.processQueryForm(in);
    QueryResultAdapter qra = new QueryResultAdapter(aq);
    psc.getServerPlugInSupport().publishAddForSubscriber(qra);

    if (aq.getType() == QueryType.PERSISTENT)
    {
      // update menu frame with new query
      sendHomePage(out, psc);
    }
    else
    {
      waitForAndReturnResults(qra.getID(), psc, out, false);
    }
  }

  private void removeQuery (AdvancedHttpInput in, PrintStream out,
                            PlanServiceContext psc)
  {
    String queryId = in.getParameter("SESSION_ID");
    findAndRemoveQuery(queryId, psc);
    sendHomePage(out, psc);
  }

  private void generateQueryReport (AdvancedHttpInput in, PrintStream out,
                                    PlanServiceContext psc) {
    String id = in.getParameter("SESSION_ID");
    QueryResultAdapter qra = findQuery(id, psc);
    printQueryReportPage(qra, out);
  }

  private void summarizeQueries (PrintStream out, PlanServiceContext psc) {
    Collection qs =
      psc.getServerPlugInSupport().queryForSubscriber(new QuerySeeker());
    Iterator i = qs.iterator();
    if (i.hasNext()) {
      out.println("Currently active queries:");
      out.println("<TABLE>");
      while (i.hasNext()) {
        QueryResultAdapter qra = (QueryResultAdapter)i.next();
        String name = qra.getQuery().getName();
        String alertPragma = "ADD_ALERT_FORM";
        if (name != null)
          alertPragma = alertPragma + "?QUERY=" + URLEncoder.encode(name);

        String queryViewLink =
          selfLink(qra.getQuery().getName() + " (" + qra.getID() + ") ",
                   "REPORT_QUERY", qra.getID(), "data");
        String queryCancelLink =
          selfLink("cancel", "CANCEL_QUERY", qra.getID(), "menu");
        String addAlertLink =
          selfLink("add alert", alertPragma, qra.getID(), "data");

        out.println("<TR><TD>&nbsp;&nbsp;&nbsp;" + queryViewLink +
          "</td><td>&nbsp;&nbsp;[" + addAlertLink + "]" +
          "</TD><TD>&nbsp;&nbsp;[" + queryCancelLink + "]" + "</TD></TR>");
      }
      out.println("</TABLE><BR>");
    }
    else {
      out.println("There are no active queries.");
      out.println("<BR><BR>");
    }
  }

  private static String selfLink (String text, String pragma, String id,
                                  String target) {
    StringBuffer buf = new StringBuffer(selfName);
    if (pragma != null) {
      buf.append("?");
      buf.append(pragma);
      if (id != null) {
        buf.append("?");
        buf.append("SESSION_ID");
        buf.append("=");
        buf.append(URLEncoder.encode(id));
      }
    }
    return hyperlink(text, buf.toString(), target);
  }

  private static String hyperlink (String text, String url, String target) {
    StringBuffer buf = new StringBuffer("<a href=\"");
    buf.append(url);
    buf.append("\" TARGET=\"" + target + "\">");
    buf.append(text);
    buf.append("</a>");
    return buf.toString();
  }
}