package org.cougaar.lib.aggagent.servlet;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.NamingService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.lib.aggagent.session.XMLEncoder;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.aggagent.util.XmlUtils;

public class AggregationXMLInterface extends AggregationServletInterface
{
  private SessionManager man = null;
  private String agentId = null;
  private NamingService namingService = null;

  public AggregationXMLInterface (BlackboardService bs,
                                  SubscriptionMonitorSupport sms,
                                  String agentId,
                                  NamingService namingService,
                                  SessionManager man) {
    super(bs, sms);
    this.agentId = agentId;
    this.namingService = namingService;
    this.man = man;
  }

  public void handleRequest(PrintWriter out, HttpServletRequest request)
  {
    if (request.getParameter("GET_QUERIES") != null)
      getQueries(out);
    else if (request.getParameter("GET_ALERTS") != null)
      getAlerts(out);
    else if (request.getParameter("CREATE_QUERY") != null)
      publishXMLQuery(request, out);
    else if (request.getParameter("CREATE_ALERT") != null)
      publishXMLAlert(request, out);
    else if (request.getParameter("GET_CLUSTERS") != null)
      getClusters(out);
    else if (request.getParameter("GET_RESULT_SET") != null)
      getResultSet(request, out);
    else if (request.getParameter("CANCEL_QUERY") != null)
      removeQueryForThick(request, out);
    else if (request.getParameter("CANCEL_ALERT") != null)
      removeAlert(request, out);
    else if (request.getParameter("CREATE_PASSIVE_SESSION") != null)
      createPassiveSession(request, out);
    else if (request.getParameter("CANCEL_PASSIVE_SESSION") != null)
      cancelPassiveSession(request, out);
    else if (request.getParameter("REQUEST_UPDATE") != null)
      requestUpdate(request, out);
    else if (request.getParameter("GET_SYSTEM_PROPERTY") != null)
      getSystemProperty(request, out);
    else if (request.getParameter("CHECK_URL") != null)
      checkUrl(out);

    out.flush();
  }

  /**
   * get all active query result adaptors and send them to the thick client
   */
  private void getQueries(PrintWriter out)
  {
    Collection qs = query(new QuerySeeker());

    out.println("<queries>");
    for (Iterator i = qs.iterator(); i.hasNext();)
    {
      out.println(((QueryResultAdapter)i.next()).toWholeXml());
    }
    out.println("</queries>");
  }

  /**
   *  Return all alerts on the blackboard
   */
  private void getAlerts (PrintWriter out) {
    Collection c = query(new AlertSeeker());

    out.println("<alerts>");
    for (Iterator i = c.iterator(); i.hasNext();)
    {
      Alert a = (Alert)i.next();
      AlertDescriptor ad = new AlertDescriptor(a);
      out.println(ad.toXml());
    }
    out.println("</alerts>");
  }

  /**
   * return all clusters on society (except for this one)
   */
  private void getClusters(PrintWriter out)
  {
    List clusterIds = getAllEncodedAgentNames();

    out.println("<clusters>");
    for (Iterator i = clusterIds.iterator(); i.hasNext();)
    {
      Object clusterId = i.next();
      if (!clusterId.equals(agentId))
      {
        out.println("<cluster_id>");
        out.println(clusterId);
        out.println("</cluster_id>");
      }
    }
    out.println("</clusters>");
  }

  // copied from SimpleServletSupportImpl
  public List getAllEncodedAgentNames() {
    List toList = new LinkedList();

    try {
      javax.naming.directory.DirContext d = namingService.getRootContext();
      d = (javax.naming.directory.DirContext)d.lookup("WEBSERVERS");
      javax.naming.NamingEnumeration en = d.list("");
      while (en.hasMoreElements()) {
        javax.naming.NameClassPair ncp =
            (javax.naming.NameClassPair) en.nextElement();
        toList.add(ncp.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }

    return toList;
  }

  /**
   * publish a new query based on incoming XML.
   */
  private void publishXMLQuery (HttpServletRequest request, PrintWriter out) {
    try {
      Element root = XmlUtils.parse(request.getInputStream());
      AggregationQuery aq = new AggregationQuery(root);
      QueryResultAdapter qra = new QueryResultAdapter(aq);
      publishAdd(qra);

      if (aq.getType() == QueryType.PERSISTENT)
      {
        out.println(qra.getID());
      }
      else
      {
        if (aq.timeoutSupplied())
          waitForAndReturnResults(qra.getID(), out, true, aq.getTimeout());
        else
          waitForAndReturnResults(qra.getID(), out, true);
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
  private void publishXMLAlert (HttpServletRequest request, PrintWriter out) {
    try {
      Element root = XmlUtils.parse(request.getInputStream());
      AlertDescriptor ad = new AlertDescriptor(root);

      QueryResultAdapter qra = findQuery(ad.getQueryId());
      Alert alert = ad.createAlert();
      if (alert != null)
      {
        qra.addAlert(alert);
        publishAdd(alert);
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
  private void removeQueryForThick (HttpServletRequest request, PrintWriter out)
  {
    String queryId = request.getParameter("QUERY_ID");
    findAndRemoveQuery(queryId);
    out.println("0");
  }

  /**
   * remove alert from query result adapter and log plan
   */
  private void removeAlert(HttpServletRequest request, PrintWriter out)
  {
    String queryId = request.getParameter("QUERY_ID");
    String alertName = request.getParameter("ALERT_NAME");
    QueryResultAdapter qra = findQuery(queryId);
    Alert alert = qra.removeAlert(alertName);
    if (alert != null)
    {
      publishChange(qra);
      publishRemove(alert);
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
  private void getResultSet(HttpServletRequest request, PrintWriter out) {
    String id = request.getParameter("QUERY_ID");
    QueryResultAdapter qra = findQuery(id);
    if (qra == null)
    {
      out.println("<result_set_not_found />");
    }
    else
    {
      out.println(qra.getResultSet().toXml());
    }
  }

  /**
   * create a passive session on this cluster that monitors a set of alerts or
   * query result set.  Return session id.
   */
  private void createPassiveSession(HttpServletRequest request, PrintWriter out)
  {
    try {
      MonitorRequestParser requestParser = new MonitorRequestParser(request);
      String k = man.addSession(requestParser.unaryPredicate,
                                new XmlIncrement(requestParser.xmlEncoder),
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
  private void cancelPassiveSession(HttpServletRequest request, PrintWriter out)
  {
    String sessionId = request.getParameter("SESSION_ID");
    man.cancelSession(sessionId);
    out.println("done");
  }

  /**
   * return local system property to client
   */
  private void getSystemProperty(HttpServletRequest request, PrintWriter out)
  {
    String propertyName = request.getParameter("PROPERTY_NAME");
    out.println(System.getProperty(propertyName));
  }

  /**
   * check my url.  If I received this message, tell client that URL is ok.
   */
  private void checkUrl(PrintWriter out)
  {
    out.println("url ok");
  }

  /**
   * Handle update request from active passive session
   */
  private void requestUpdate(HttpServletRequest request, PrintWriter out)
  {
    String sessionId = request.getParameter("SESSION_ID");
    man.sendUpdate(sessionId, out);
  }

  private static class TrivialXMLEncoder implements XMLEncoder {
    public void encode (Object o, Collection c) {
      c.add(o);
    }
  }
  private static XMLEncoder sharedXMLEncoder = new TrivialXMLEncoder();

  /**
   * Used to create unary predicate and xml encoder based on incoming monitor
   * request from client.
   */
  public static class MonitorRequestParser
  {
    public UnaryPredicate unaryPredicate;
    public XMLEncoder xmlEncoder;
    public MonitorRequestParser(HttpServletRequest request) throws Exception
    {
      Element root = XmlUtils.parse(request.getInputStream());
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

        // create xml encoder--well, sort of borrow it, actually...
        xmlEncoder = sharedXMLEncoder;
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

        // create xml encoder--well, sort of borrow it, actually...
        xmlEncoder = sharedXMLEncoder;
      }
    }
  }
}
