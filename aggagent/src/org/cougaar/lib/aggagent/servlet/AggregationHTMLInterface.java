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

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.test.CycleSizeAlert;
import org.cougaar.lib.aggagent.util.Enum.QueryType;

public class AggregationHTMLInterface extends AggregationServletInterface
{
  private String servletName;

  public AggregationHTMLInterface (BlackboardService bs,
                                   SubscriptionMonitorSupport sms,
                                   String servletName, UIDService uidService) {
    super(bs, sms, uidService);

    // remove first forward slash
    this.servletName = servletName.trim().substring(1);
  }

  public void handleRequest(PrintWriter out, HttpServletRequest request)
  {
    // print the header
    out.println("<html><head><title>Aggregation Servlet</title></head>");

    if (!request.getParameterNames().hasMoreElements())
    {
      sendMainFrame(out);
    }
    else
    {
      out.println("<body BGCOLOR=\"#EEEEEE\">");

      // check the parameters and decide what to do
      if (request.getParameter("CREATE_QUERY") != null)
        publishHTMLQuery(request, out);
      else if (request.getParameter("CREATE_QUERY_FORM") != null)
        HTMLPresenter.sendQueryForm(servletName, out, true);
      else if (request.getParameter("CREATE_TRAN_QUERY_FORM") != null)
        HTMLPresenter.sendQueryForm(servletName, out, false);
      else if (request.getParameter("REPORT_QUERY") != null)
        generateQueryReport(request, out);
      else if (request.getParameter("CANCEL_QUERY") != null)
        removeQuery(request, out);
      else if (request.getParameter("HOME") != null)
        sendHomePage(out);
      else if (request.getParameter("TITLE") != null)
        sendTitle(out);
      else if (request.getParameter("DEFAULT_ALERT") != null)
        addDefaultAlert(out);
      else if (request.getParameter("ACTIVE_ALERTS") != null)
        checkActiveAlerts(out);
      else if (request.getParameter("ADD_ALERT_FORM") != null)
        HTMLPresenter.sendAlertForm(servletName, request, out);
      else if (request.getParameter("ADD_ALERT") != null)
        processAlertForm(request, out);
      else
        sendHomePage(out);

      out.println("</body>");
    }

    // print trailer and flush
    out.println("</html>");
    out.flush();
  }

  private void processAlertForm (HttpServletRequest request, PrintWriter out)
  {
    // create the alert
    AlertDescriptor ad = HTMLPresenter.processAlertForm(request);

    // find the indicated query, if any
    Iterator i = query(new QuerySeeker(ad.getQueryId())).iterator();
    if (i.hasNext()) {
      QueryResultAdapter qra = (QueryResultAdapter) i.next();
      try {
        Alert a = ad.createAlert();
        qra.addAlert(a);
        publishAdd(a);
        checkActiveAlerts(out);
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

  private void sendMainFrame(PrintWriter out) {
    out.println("<FRAMESET ROWS=\"10%, 90%\">");
    out.println("<FRAME NAME=\"title\" SRC=\"" + servletName + "?TITLE=foo\">");
    out.println("<FRAMESET COLS=\"30%, 70%\">");
    out.println("<FRAME NAME=\"menu\" SRC=\"" + servletName + "?HOME=foo\">");
    out.println("<FRAME NAME=\"data\" SRC=\"about:blank\">");
    out.println("</FRAMESET></FRAMESET>");
  }

  private void sendTitle(PrintWriter out) {
    out.println("<H1 align=\"center\">Aggregation Servlet</H1>");
  }

  private void sendHomePage (PrintWriter out) {
    out.println("<P><h3>Persistent Queries</h3>");
    summarizeQueries(out);
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
  private void checkActiveAlerts (PrintWriter out) {
    out.println("checking for active alerts ... <br>");
    Collection c = query(new AlertSeeker());
    out.println("Found " + c.size() + " alerts:<br><center>");
    HTMLPresenter.sendAlertSummary(c, out);
  }

  //
  //  Add the default Alert-ridden query to the blackboard
  //
  private void addDefaultAlert (PrintWriter out) {
    QueryResultAdapter qra =
      new QueryResultAdapter(CycleSizeAlert.createDefaultQuery(), getUIDService().nextUID());
    Alert ale = CycleSizeAlert.getDefaultAlert();
    qra.addAlert(ale);
    publishAdd(qra);
    publishAdd(ale);
    sendHomePage(out);
  }

  private void publishHTMLQuery (HttpServletRequest request,
                                 PrintWriter out) {
    // parse form post request
    AggregationQuery aq = HTMLPresenter.processQueryForm(request);
    QueryResultAdapter qra = new QueryResultAdapter(aq, getUIDService().nextUID());
    publishAdd(qra);

    if (aq.getType() == QueryType.PERSISTENT)
    {
      // update menu frame with new query
      sendHomePage(out);
    }
    else
    {
      if (aq.timeoutSupplied())
        waitForAndReturnResults(qra.getID(), out, false, aq.getTimeout());
      else
        waitForAndReturnResults(qra.getID(), out, false);
    }
  }

  private void removeQuery (HttpServletRequest in, PrintWriter out)
  {
    String queryId = in.getParameter("SESSION_ID");
    findAndRemoveQuery(queryId);
    sendHomePage(out);
  }

  private void generateQueryReport (HttpServletRequest in, PrintWriter out) {
    String id = in.getParameter("SESSION_ID");
    QueryResultAdapter qra = findQuery(id);
    printQueryReportPage(qra, out);
  }

  private void summarizeQueries (PrintWriter out) {
    Collection qs = query(new QuerySeeker());
    Iterator i = qs.iterator();
    if (i.hasNext()) {
      out.println("Currently active queries:");
      out.println("<TABLE>");
      while (i.hasNext()) {
        QueryResultAdapter qra = (QueryResultAdapter)i.next();
        String name = qra.getQuery().getName();
        String alertPragma = "ADD_ALERT_FORM";

        try {
          if (name != null)
            alertPragma = "QUERY=" + URLEncoder.encode(name, "UTF-8") + "&" +
                          alertPragma;
        } catch (java.io.UnsupportedEncodingException e)
        {
          e.printStackTrace();
        }

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

  private String selfLink (String text, String pragma, String id,
                           String target) {
    String hl = null;

    try {
      StringBuffer buf = new StringBuffer(servletName);
      if (pragma != null) {
        buf.append("?");
        buf.append(pragma);
        buf.append("=1");
        if (id != null) {
          buf.append("&");
          buf.append("SESSION_ID");
          buf.append("=");
          buf.append(URLEncoder.encode(id, "UTF-8"));
        }
      }
      hl = hyperlink(text, buf.toString(), target);
    } catch (java.io.UnsupportedEncodingException e)
    {
      e.printStackTrace();
    }

    return hl;
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