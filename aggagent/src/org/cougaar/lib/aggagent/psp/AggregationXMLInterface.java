package org.cougaar.lib.aggagent.psp;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import org.w3c.dom.*;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.lib.aggagent.session.XMLEncoder;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.util.AdvancedHttpInput;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.aggagent.util.XmlUtils;

public class AggregationXMLInterface extends AggregationPSPInterface
{
  public AggregationXMLInterface(SessionManager man)
  {
    super(man);
  }

  public void handleRequest(PrintStream out, AdvancedHttpInput ahi,
                            PlanServiceContext psc)
  {
    if (ahi.hasParameter("GET_QUERIES"))
      getQueries(out, psc);
    else if (ahi.hasParameter("GET_ALERTS"))
      getAlerts(out, psc);
    else if (ahi.hasParameter("CREATE_QUERY"))
      publishXMLQuery(ahi, psc, out);
    else if (ahi.hasParameter("CREATE_ALERT"))
      publishXMLAlert(ahi, psc, out);
    else if (ahi.hasParameter("GET_CLUSTERS"))
      getClusters(out, psc);
    else if (ahi.hasParameter("GET_RESULT_SET"))
      getResultSet(ahi, out, psc);
    else if (ahi.hasParameter("CANCEL_QUERY"))
      removeQueryForThick(ahi, out, psc);
    else if (ahi.hasParameter("CANCEL_ALERT"))
      removeAlert(ahi, out, psc);
    else if (ahi.hasParameter("CREATE_PASSIVE_SESSION"))
      createPassiveSession(ahi, out);
    else if (ahi.hasParameter("CANCEL_PASSIVE_SESSION"))
      cancelPassiveSession(ahi, out);
    else if (ahi.hasParameter("REQUEST_UPDATE"))
      requestUpdate(ahi, out);

    out.flush();
  }

  /**
   * get all active query result adaptors and send them to the thick client
   */
  private void getQueries(PrintStream out, PlanServiceContext psc)
  {
    Collection qs =
      psc.getServerPlugInSupport().queryForSubscriber(new QuerySeeker());

    out.println("<queries>");
    for (Iterator i = qs.iterator(); i.hasNext();)
    {
      out.println(((QueryResultAdapter)i.next()).toXML());
    }
    out.println("</queries>");
  }

  /**
   *  Return all alerts on the blackboard
   */
  private void getAlerts (PrintStream out, PlanServiceContext psc) {
    Collection c = psc.getServerPlugInSupport().queryForSubscriber(
      new AlertSeeker());

    out.println("<alerts>");
    for (Iterator i = c.iterator(); i.hasNext();)
    {
      Alert a = (Alert)i.next();
      AlertDescriptor ad = new AlertDescriptor(a);
      out.println(ad.toXML());
    }
    out.println("</alerts>");
  }

  /**
   * return all clusters on society (except for this one)
   */
  private void getClusters(PrintStream out, PlanServiceContext psc)
  {
    String thisCluster = psc.getServerPlugInSupport().getClusterIDAsString();
    Vector clusterIds = new Vector();
    psc.getAllNames(clusterIds, true);

    out.println("<clusters>");
    for (int i = 0; i < clusterIds.size(); i++)
    {
      if (!clusterIds.elementAt(i).equals(thisCluster))
      {
        out.println("<cluster_id>");
        out.println(clusterIds.elementAt(i));
        out.println("</cluster_id>");
      }
    }
    out.println("</clusters>");
  }

  /**
   * publish a new query based on incoming XML.
   */
  private void publishXMLQuery (AdvancedHttpInput in, PlanServiceContext psc,
                                PrintStream out) {
    try {
      Element root = XmlUtils.parse(in.getHttpInput().getBodyAsString());
      AggregationQuery aq = new AggregationQuery(root);
      QueryResultAdapter qra = new QueryResultAdapter(aq);
      psc.getServerPlugInSupport().publishAddForSubscriber(qra);

      if (aq.getType() == QueryType.PERSISTENT)
      {
        out.println(qra.getID());
      }
      else
      {
        waitForAndReturnResults(qra.getID(), psc, out, true);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      out.println(-1);
    }
  }

  /**
   * publish a new alert based on incoming XML.
   */
  private void publishXMLAlert (AdvancedHttpInput in, PlanServiceContext psc,
                                PrintStream out) {
    try {
      Element root = XmlUtils.parse(in.getHttpInput().getBodyAsString());
      AlertDescriptor ad = new AlertDescriptor(root);

      QueryResultAdapter qra = findQuery(ad.getQueryId(), psc);
      Alert alert = ad.createAlert();
      if (alert != null)
      {
        qra.addAlert(alert);
        psc.getServerPlugInSupport().publishAddForSubscriber(alert);
        out.println(0);
      }
      else
      {
        out.println(-1);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      out.println(-1);
    }
  }

  /**
   * remove query with given query id
   */
  private void removeQueryForThick (AdvancedHttpInput in, PrintStream out,
                                    PlanServiceContext psc)
  {
    String queryId = in.getParameter("QUERY_ID");
    findAndRemoveQuery(queryId, psc);
    out.println("0");
  }

  /**
   * remove alert from query result adapter and log plan
   */
  private void removeAlert(AdvancedHttpInput in, PrintStream out,
                           PlanServiceContext psc)
  {
    String queryId = in.getParameter("QUERY_ID");
    String alertName = in.getParameter("ALERT_NAME");
    QueryResultAdapter qra = findQuery(queryId, psc);
    Alert alert = qra.removeAlert(alertName);
    if (alert != null)
    {
      psc.getServerPlugInSupport().publishChangeForSubscriber(qra);
      psc.getServerPlugInSupport().publishRemoveForSubscriber(alert);
      out.println("0");
    }
    else
    {
      out.println("-1");
    }
  }

  /**
   * Return an up to date AggregationResultSet for given query id
   */
  private void getResultSet(AdvancedHttpInput in, PrintStream out,
                            PlanServiceContext psc) {
    String id = in.getParameter("QUERY_ID");
    QueryResultAdapter qra = findQuery(id, psc);
    if (qra == null)
    {
      out.println("<result_set_not_found />");
    }
    else
    {
      out.println(qra.getResultSet().toXML());
    }
  }

  /**
   * create a passive session on this cluster that monitors a set of alerts or
   * query result set.  Return session id.
   */
  private void createPassiveSession(AdvancedHttpInput in, PrintStream out)
  {
    try {
      MonitorRequestParser request =
        new MonitorRequestParser(in.getHttpInput());
      String k = man.addSession(request.unaryPredicate,
                                new XmlIncrement(request.xmlEncoder),
                                "PASSIVE SESSION");
      out.println(k);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      out.println(-1);
    }
  }

  /**
   * cancel passive session
   */
  private void cancelPassiveSession(AdvancedHttpInput in, PrintStream out)
  {
    String sessionId = in.getParameter("SESSION_ID");
    man.cancelSession(sessionId);
    out.println("done");
  }

  /**
   * Handle update request from active passive session
   */
  private void requestUpdate(AdvancedHttpInput in, PrintStream out)
  {
    String sessionId = in.getParameter("SESSION_ID");
    man.sendUpdate(sessionId, out);
  }

  /**
   * Used to create unary predicate and xml encoder based on incoming monitor
   * request from client.
   */
  public static class MonitorRequestParser
  {
    public UnaryPredicate unaryPredicate;
    public XMLEncoder xmlEncoder;
    public MonitorRequestParser(HttpInput in) throws Exception
    {
      Element root = XmlUtils.parse(in.getBodyAsString());
      boolean monitorAll =
        root.getElementsByTagName("monitor_all").getLength() != 0;
      String type = root.getAttribute("type");
      if (type.equals("result_set"))
      {
        // create unary predicate
        if (monitorAll)
        {
          unaryPredicate = new QuerySeeker();
        }
        else
        {
          LinkedList queries = new LinkedList();
          NodeList nl = root.getElementsByTagName("query");
          for (int i = 0; i < nl.getLength(); i++)
          {
            Element queryElement = (Element)nl.item(i);
            queries.add(queryElement.getAttribute("id"));
          }
          unaryPredicate = new QuerySeeker(queries);
        }

        // create xml encoder
        xmlEncoder = new XMLEncoder() {
            public void encode(Object o, PrintStream ps)
            {
              QueryResultAdapter qra = (QueryResultAdapter)o;
              String additionalAttributes = "query_id=\"" + qra.getID() + "\"";
              ps.print(qra.getResultSet().toXML(additionalAttributes));
            }
          };
      }
      else if (type.equals("alert"))
      {
        if (monitorAll)
        {
          unaryPredicate = new AlertSeeker();
        }
        else
        {
          // create unary predicate
          LinkedList alerts = new LinkedList();
          NodeList nl = root.getElementsByTagName("alert");
          for (int i = 0; i < nl.getLength(); i++)
          {
            Element alertElement = (Element)nl.item(i);
            alerts.add(
              new AlertIdentifier(alertElement.getAttribute("query_id"),
                                alertElement.getAttribute("alert_name")));
          }
          unaryPredicate = new AlertSeeker(alerts);
        }

        // create xml encoder
        xmlEncoder = new XMLEncoder() {
            public void encode(Object o, PrintStream ps)
            {
              Alert alert = (Alert)o;
              AlertDescriptor ad = new AlertDescriptor(alert);
              ps.print(ad.toXML());
            }
          };
      }
    }
  }
}
