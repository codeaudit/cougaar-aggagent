package org.cougaar.lib.aggagent.servlet;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.*;
import org.cougaar.lib.aggagent.util.Enum.*;

/**
 * HTML utilities to assist with HTML presentation and form post interpretation
 * related to core aggregation query objects.  This could be extended to become
 * non-static and to include look and feel description fields
 * (e.g. colors, fonts to use)
 */
public class HTMLPresenter
{
  public static String toHTML(QueryResultAdapter qra)
  {
    StringBuffer s = new StringBuffer();
    s.append("<B>Query #" + qra.getID() + "</B><P>");
    s.append(toHTML(qra.getResultSet()));

    return s.toString();
  }

  public static String toHTML(AggregationResultSet rs)
  {
    StringBuffer s = new StringBuffer("<TABLE BORDER=\"1\" BGCOLOR=\"WHITE\">\n");
    boolean firstElement = true;

    Iterator dataAtoms = rs.getAllAtoms();
    while (dataAtoms.hasNext())
    {
      ResultSetDataAtom d = (ResultSetDataAtom)dataAtoms.next();
      if (firstElement)
      {
        firstElement = false;
        s.append("<TR BGCOLOR=\"#888888\"><TH COLSPAN=\"");
        s.append(countElements(d.getIdentifierNames()));
        s.append("\"><FONT COLOR=\"WHITE\">Identifier(s)</FONT></TH><TH COLSPAN=\"");
        s.append(countElements(d.getValueNames()));
        s.append("\"><FONT COLOR=\"WHITE\">Value(s)</FONT></TH></TR>\n");
        s.append("<TR BGCOLOR=\"NAVY\">");
        addHeaderCells(s, d.getIdentifierNames());
        addHeaderCells(s, d.getValueNames());
        s.append("</TR>\n");
      }

      s.append("<TR>");
      Iterator ids = d.getIdentifierNames();
      while (ids.hasNext())
      {
        s.append("<TD>" + d.getIdentifier(ids.next()) + "</TD>");
      }
      Iterator values = d.getValueNames();
      while (values.hasNext())
      {
        s.append("<TD>" + d.getValue(values.next()) + "</TD>");
      }
      s.append("</TR>\n");
    }
    s.append("</TABLE>");

    return s.toString();
  }

  public static void sendAlertSummary(Collection alerts, PrintWriter out)
  {
    out.println("<table BORDER=\"1\" BGCOLOR=\"WHITE\">");
    sendAlertRow(out, "000080", "FFFFFF", "Query", "Name", "Triggered", true);
    for (Iterator i = alerts.iterator(); i.hasNext(); )
      sendAlert(out, (Alert) i.next());
    out.println("</table></center>");
  }

  public static void sendQueryForm (String selfName, PrintWriter out,
                                    boolean persistent) {
    QueryType queryType =
      persistent ? QueryType.PERSISTENT : QueryType.TRANSIENT;
    out.println("<CENTER>");
    out.println("<TABLE BORDER=1 cellpadding=8 BGCOLOR=\"#DDDDDD\">");
    out.print("<TR><TH>Create New ");
    out.print(queryType);
    out.println(" Query</TH></TR>");
    out.println("<TR><TD>");
    out.println("<form action=\"" + selfName + "\" ");
    out.println("method=\"GET\" " + (persistent ? "target=\"menu\">" : ">"));
    out.println("<input type=\"HIDDEN\" name=\"CREATE_QUERY\" value =\"1\">");
    out.print("<input name=\"query_type\" type=\"HIDDEN\" value=\"");
    out.println(queryType + "\"/>");
    if (persistent)
    {
      out.println("Query Name: ");
      out.println("<input name=\"query_name\" type=\"TEXT\" size=\"15\"/>");
      out.println("&nbsp;&nbsp;Update Method: ");
      out.println("<SELECT NAME=\"update_method\">");
      for (Iterator i=UpdateMethod.getValidValues().iterator(); i.hasNext();)
      {
        UpdateMethod updateMethod = (UpdateMethod)i.next();
        out.print("<OPTION VALUE=\"");
        out.print(updateMethod);
        out.print("\" ");
        out.print(updateMethod == UpdateMethod.PUSH ? "SELECTED>" : ">");
        out.println(updateMethod);
      }
      out.println("</SELECT>");
      out.println("&nbsp;&nbsp;Pull Wait Period: ");
      out.println("<input name=\"pull_rate\" value=\"60\" type=\"TEXT\"");
      out.println(" size=\"3\"/> sec.");
      out.println("<BR><BR>");
    }
    out.println("List of Source Cluster IDs:<br>");
    out.println("<input name=\"source_cluster\" type=\"TEXT\" size=\"70\"/>");
    out.println("<BR><BR>");
    out.println("Unary Predicate Script: ");
    sendLanguageSelector("unary_predicate_lang", out);
    out.println("<BR>");
    out.println("<TEXTAREA name=\"unary_predicate\" ROWS=\"4\" COLS=\"70\">");
    out.println("</TEXTAREA><BR><BR>");
    out.println("XML Encoder Script: ");
    sendCoderTypeSwitch("xml_encoder_type", "Full Format", out);
    sendLanguageSelector("xml_encoder_lang", out);
    out.println("<BR>");
    out.println("<TEXTAREA name=\"xml_encoder\" ROWS=\"8\" COLS=\"70\">");
    out.println("</TEXTAREA>");
    out.println("</TD></TR><TR><TD ALIGN=\"CENTER\">");
    out.println("<input value=\"Create Query\" type=\"SUBMIT\"/>");
    out.println("</form>");
    out.println("</TD></TR>");
    out.println("</TABLE>");
    out.println("</CENTER>");
  }

  public static AggregationQuery processQueryForm (HttpServletRequest request)
  {
    // get query type
    QueryType queryType =
        QueryType.fromString(request.getParameter("query_type"));

    // publish request to blackboard for Aggregation Plugin
    AggregationQuery aq = new AggregationQuery(queryType);
    if (queryType == QueryType.PERSISTENT)
    {
      aq.setName(request.getParameter("query_name"));
      aq.setUpdateMethod(
        UpdateMethod.fromString(request.getParameter("update_method")));
      aq.setPullRate(Integer.parseInt(request.getParameter("pull_rate")));
    }
    String sourceClusters = request.getParameter("source_cluster");
    StringTokenizer t = new StringTokenizer(sourceClusters, ",");
    while (t.hasMoreTokens())
    {
      aq.addSourceCluster(t.nextToken().trim());
    }

    String pred = request.getParameter("unary_predicate");
    Language pred_l =
      Language.fromString(request.getParameter("unary_predicate_lang"));
    aq.setPredicateSpec(
      new ScriptSpec(ScriptType.UNARY_PREDICATE, pred_l, pred));
    String coder = request.getParameter("xml_encoder");
    Language coder_l =
      Language.fromString(request.getParameter("xml_encoder_lang"));
    XmlFormat coder_f =
      (request.getParameter("xml_encoder_type") != null) ?
        XmlFormat.INCREMENT :
        XmlFormat.XMLENCODER;
    aq.setFormatSpec(new ScriptSpec(coder_l, coder_f, coder));

    return aq;
  }

  public static void sendAlertForm (String selfName,
                                    HttpServletRequest request,
                                    PrintWriter out) {
    String name = request.getParameter("QUERY");
    String id = request.getParameter("SESSION_ID");
    out.println("<center>");
    out.println("<form action=\"" + selfName +
                "\" method=\"GET\" target=\"data\">");
    out.println("<input type=\"HIDDEN\" name=\"ADD_ALERT\" value =\"1\">");
    out.println("<table border=1 cellpadding=8 bgcolor=\"#DDDDDD\">");
    out.print("<tr><th>Create new Alert for Query");
    if (name != null && name.length() > 0)
      out.print(" " + name);
    out.println(" (" + id + ")</th></tr>");
    out.println("<tr><td>");
    out.println(
      "<input name=\"query_id\" type=\"hidden\" value=\"" + id + "\"/>");
    out.println("Alert Name: ");
    out.println("<input name=\"alert_name\" type=\"text\" size=\"15\"/>");
    out.println("<br><br>Alert Specification: ");
    sendLanguageSelector("alert_script_lang", out);
    out.println("<br>");
    sendTextArea("alert_script", 8, 70, out);

    out.println("</td></tr><tr><td align=\"center\">" +
      "<input value=\"Create Alert\" type=\"submit\"/>");

    out.println("</td></tr>");
    out.println("</table>");
    out.println("</form>");
    out.println("</center>");
  }

  public static AlertDescriptor processAlertForm(HttpServletRequest request)
  {
    String name = request.getParameter("alert_name");
    String query = request.getParameter("query_id");
    String lang = request.getParameter("alert_script_lang");
    String script = request.getParameter("alert_script");

    // create the Alert
    AlertDescriptor ad =new AlertDescriptor(Language.fromString(lang), script);
    ad.setName(name);
    ad.setQueryId(query);

    return ad;
  }

  private static void sendAlert(PrintWriter out, Alert a) {
    StringBuffer buf = new StringBuffer();
    QueryResultAdapter qra = a.getQueryAdapter();
    AggregationQuery q = qra.getQuery();

    String query = q.getName();
    if (query != null)
      buf.append(query);
    buf.append(" (");
    buf.append(qra.getID());
    buf.append(")");

    String status = String.valueOf(a.isAlerted());

    sendAlertRow(
      out, "FFFFFF", "000000", buf.toString(), a.getName(), status, false);
  }

  private static void sendAlertRow (PrintWriter out, String bg, String fg,
      String query, String name, String status, boolean header)
  {
    out.println("<tr>");
    sendTableCell(out, bg, fg, query, header);
    sendTableCell(out, bg, fg, name, header);
    sendTableCell(out, bg, fg, status, header);
    out.println("</tr>");
  }

  private static void sendTableCell(
    PrintWriter out, String bg, String fg, String x, boolean header)
  {
    String tagName = header ? "th" : "td";
    out.print("<" + tagName);
    if (bg != null)
      out.print(" bgcolor=\"#" + bg + "\"");
    out.print(">");
    if (fg != null)
      out.print("<font color=\"#" + fg + "\">");
    if (x != null)
      out.print(x);
    if (fg != null)
      out.print("</font>");
    out.println("</" + tagName + ">");
  }

  private static void sendTextArea (String name, int rows, int cols,
                                    PrintWriter out) {
    out.println("<textarea name=\"" + name + "\" rows = \"" + rows +
      "\" cols=\"" + cols + "\"></textarea>");
  }

  private static void sendCoderTypeSwitch (
      String paramName, String appearance, PrintWriter out)
  {
    out.print("(");
    out.print("<input name=\"");
    out.print(paramName);
    out.print("\" type=\"checkbox\"/>");
    out.print(appearance);
    out.println(")");
  }

  private static void sendLanguageSelector (String paramName, PrintWriter out)
  {
    out.print("(");
    for (Iterator i = Language.getValidValues().iterator(); i.hasNext();)
    {
      Language lang = (Language)i.next();
      String langString = lang.toString();
      sendRadio(paramName,langString,langString,(lang == Language.JAVA),out);
      if (i.hasNext())
        out.println("&nbsp;&nbsp;");
    }
    out.println(")");
  }

  private static void sendRadio (String paramName, String appearance,
                                 String value,boolean selected,PrintWriter out)
  {
    out.print("<input name=\"");
    out.print(paramName);
    out.print("\" type=\"radio\" value=\"");
    out.print(value);
    out.print("\"");
    if (selected)
      out.print(" CHECKED ");
    out.print("/>");
    out.println(appearance);
  }

  private static void addHeaderCells(StringBuffer s, Iterator names)
  {
    while (names.hasNext())
    {
      s.append("<TH><FONT COLOR=\"WHITE\">" + names.next() + "</FONT></TH>");
    }
  }

  private static int countElements(Iterator i)
  {
    int count = 0;
    while(i.hasNext())
    {
      count++;
      i.next();
    }
    return count;
  }
}
