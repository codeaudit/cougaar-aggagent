package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

public class HtmlIncrement implements IncrementFormat {
  private boolean include_outer_tags = false;

  public void setIncludeOuterTags (boolean boo) {
    include_outer_tags = boo;
  }

  public void encode (OutputStream out, SubscriptionAccess data, String key,
                      String queryId, String clusterId) {
    PrintStream ps = new PrintStream(out);
    printHeaders(ps, key, queryId, clusterId);
    printTable(ps, data);
    printTrailers(ps);
  }

  private void printTable (PrintStream out, SubscriptionAccess numbers) {
    out.println("<table>");
    printTableRow(out, "<B>Added</B>", numbers.getAddedCollection());
    printTableRow(out, "<B>Changed</B>", numbers.getChangedCollection());
    printTableRow(out, "<B>Removed</B>", numbers.getRemovedCollection());
    printTableRow(out, "<B>Membership</B>", numbers.getMembership());
    out.println("</table>");
    out.flush();
  }

  private void printTableRow (
      PrintStream out, String header, Collection stuff)
  {
    out.print("<tr VALIGN=\"top\"><td>");
    out.print(header);
    out.println("</td><td>");
    Iterator i = stuff.iterator();
    if (i.hasNext()) {
      out.print(i.next());
      while (i.hasNext()) {
        out.println("<br/>");
        out.print(i.next());
      }
    }
    out.println("</td></tr>");
  }

  private void printHeaders (PrintStream out, String key, String queryId,
                             String clusterId) {
    if (include_outer_tags)
      out.println("<html><head></head><body>");

    out.println("<center>");
    out.println(
      "<B>Subscription data for<BR>session_id=\"" + key + "\", queryId=\"" +
      queryId + "\", clusterId=\"" + clusterId + "\"</B><P>");
    out.flush();
  }

  private void printTrailers (PrintStream out) {
    out.println("</center>");

    if (include_outer_tags)
      out.println("</body></html>");

    out.flush();
  }
}
