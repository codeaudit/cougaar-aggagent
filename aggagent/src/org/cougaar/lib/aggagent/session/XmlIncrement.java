package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.lib.aggagent.plugin.Const;

public class XmlIncrement implements IncrementFormat {
  private XMLEncoder xmlEncoder = null;

  public XmlIncrement(XMLEncoder coder) {
    if (coder == null)
      throw new IllegalArgumentException("cannot accept a null XMLEncoder");

    xmlEncoder = coder;
  }

  public void encode (OutputStream out, SubscriptionAccess data, String key,
                      String queryId, String clusterId) {
    PrintStream ps = new PrintStream(out);
    ps.println(Const.XML_HEAD);
    ps.println("<update "+identifierAttributes(key, queryId, clusterId)+">");
    sendBunch(data.getAddedCollection(), "added", ps);
    sendBunch(data.getChangedCollection(), "changed", ps);
    sendBunch(data.getRemovedCollection(), "removed", ps);
    ps.println("</update>");
  }

  private String identifierAttributes (String key, String queryId,
                                       String clusterId) {
    StringBuffer s = new StringBuffer();
    s.append("session_key=\"");
    s.append(key);
    s.append("\" query_id=\"");
    s.append(queryId);
    s.append("\" cluster_id=\"");
    s.append(clusterId);
    s.append("\"");

    return s.toString();
  }

  private void sendBunch (Collection c, String type, PrintStream ps) {
    if (c.isEmpty())
      return;

    ps.println("  <" + type + ">");

    for (Iterator i = c.iterator(); i.hasNext(); )
      encode(i.next(), ps);

    ps.println("  </" + type + ">");
    ps.flush();
  }

  private void encode (Object o, PrintStream ps) {
    xmlEncoder.encode(o, ps);
  }
}
