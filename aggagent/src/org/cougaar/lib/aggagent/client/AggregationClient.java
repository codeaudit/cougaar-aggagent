/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.client;

import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;

import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.lib.aggagent.util.Enum.QueryType;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The AggregationPSPClient provides a client-side communication abstraction
 * to a specific Aggregation Servlet.  It provides a simple interface to the
 * functionality provided by the Aggregation Agent utilizing the Aggregation
 * PSP's XML interface. It manages both transient requests and persistent
 * sessions using both transient and keep alive connection(s) with the
 * Aggregation Servlet.<BR><BR>
 *
 * The core functions provided by the Aggregation Agent that can be accessed via
 * this interface follow:
 * <UL>
 * <LI>Creation and management of both transient and persistent queries that
 * can gather and aggregate any type of data from a set of clusters. </LI>
 * <LI>Creation and management of Alerts which can be used to watch for a
 * particular condition to be met in a persistent query's result set.</LI>
 * <LI>Event driven incremental monitoring of changing result sets and alerts
 * on the Aggregation Agent's blackboard</LI>
 * </LI>
 * </UL>
 */
public class AggregationClient
{
  private String aggregationURL = null;
  private String keepAliveURL = null;
  private Timer pullTimer = new Timer();
  private Logger logger;
  
  /**
   * Create a new client interface to a specific Aggregation Agent.
   *
   * @param clusterURL        text url of the aggregation agent's cluster
   * @param aggregationName name given to aggregation servlet on given
   *                          aggregation agent. (typical:  "aggregator")
   * @param keepAliveName  name given to keep alive aggregation servlet on the
   *                          given aggregation agent.
   *                          (typical: "aggregatorkeepalive")
   */
  public AggregationClient(String clusterURL, String aggregationName,
                           String keepAliveName)
  {
    logger = Logging.getLogger(this);

    aggregationURL =
        clusterURL + "/" + aggregationName + "?THICK_CLIENT=1";
    keepAliveURL = clusterURL + "/" + keepAliveName + "?KEEP_ALIVE=1";

    // check url
    String response =
      XmlUtils.requestString(aggregationURL + "&CHECK_URL=1", null);

    if (response == null)
    {
      throw new NullPointerException("Cannot contact aggregation agent at " +
                                     aggregationURL);
    }
  }

  /**
   * Returns a collection of all Query Result Adapters on the aggregation
   * agent's log plan.  Each query result adapter includes a query, a
   * result set, and a set of alerts.  This is basically a snapshot of
   * everything on the aggregation agent's blackboard.  Only persistent queries
   * will be found.
   *
   * @return a collection of all query result adapters on the aggregation
   * agent's log plan.
   */
  public Collection getActiveQueries()
  {
    Element root =
        XmlUtils.requestXML(aggregationURL + "&GET_QUERIES=1", null);

    NodeList queryNodes =
      root.getElementsByTagName(QueryResultAdapter.QUERY_RESULT_TAG);

    LinkedList queries = new LinkedList();
    for (int i = 0; i < queryNodes.getLength(); i++)
    {
      queries.add(new QueryResultAdapter((Element)queryNodes.item(i)));
    }

    return queries;
  }

  /**
   * Returns a collection of Alert Descriptors for all alerts on the
   * aggregation agent's log plan.  Alert descriptors are client-side
   * descriptions of Alerts. These alert descriptors will be orphans
   * (i.e. getQueryResultAdapter() == null) but they will include a reference
   * to the query id of their query result adaptor on the aggregation agent
   * (accessed via getQueryId()).
   *
   * @return a collection of Alert Descriptors for all alerts on the
   * aggregation agent's log plan.
   */
  public Collection getActiveAlerts()
  {
    Element root =
      XmlUtils.requestXML(aggregationURL + "&GET_ALERTS=1", null);

    NodeList alertNodes = root.getElementsByTagName(Alert.ALERT_TAG);

    LinkedList alerts = new LinkedList();
    for (int i = 0; i < alertNodes.getLength(); i++)
    {
      alerts.add(new AlertDescriptor((Element)alertNodes.item(i)));
    }

    return alerts;
  }

  /**
   * Request the creation of a query on aggregation agent.
   *
   * @param aq aggregation query object that fully describes the query.  Query
   *           can be either transient or persistent.
   *
   * @return If query is a persistent query, a queryId String will be returned.
   *         If query is transient an AggregationResultSet will be returned.
   */
  public Object createQuery(AggregationQuery aq)
  {
    Object response = null;
    String taggedURL = aggregationURL + "&CREATE_QUERY=1";
    if (aq.getType() == QueryType.PERSISTENT)
    {
      response = XmlUtils.requestString(taggedURL, aq.toXml()).trim();
    }
    else
    {
      Element root = XmlUtils.requestXML(taggedURL, aq.toXml());
      response = new AggregationResultSet(root);
    }
    return response;
  }

  /**
   * Request the creation of an alert on aggregation agent.
   *
   * @param ad alert descriptor that fully describes the alert.  This alert
   *           descriptor must include the query id of the query result adapter
   *           to which this alert should be added.
   *
   * @return true if successful; false otherwise
   */
  public boolean createAlert(AlertDescriptor ad)
  {
    String response = null;
    String taggedURL = aggregationURL + "&CREATE_ALERT=1";
    response = XmlUtils.requestString(taggedURL, ad.toXml()).trim();

    return response.equals("0");
  }

  /**
   * Request a list of all clusters in the society excluding the aggregation
   * agent.  These can all be used as source clusters in future queries.
   *
   * @return a collection of clusterId Strings.
   */
  public Collection getClusterIds()
  {
    Element root =
      XmlUtils.requestXML(aggregationURL + "&GET_CLUSTERS=1", null);

    NodeList clusterNodes = root.getElementsByTagName("cluster_id");

    LinkedList clusters = new LinkedList();
    for (int i = 0; i < clusterNodes.getLength(); i++)
    {
      clusters.add(clusterNodes.item(i).getFirstChild().getNodeValue().trim());
    }

    return clusters;
  }

  /**
   * Get an updated result set for an active persistent query from blackboard
   * of aggregation agent.
   *
   * @param queryId id of query result adapter for needed result set.
   *
   * @return an updated result set for an active persistent query from
   *         blackboard of aggregation agent.  Returns null of query is not
   *         found.
   */
  public AggregationResultSet getUpdatedResultSet(String queryId)
  {
    String loadedURL =
        aggregationURL + "&GET_RESULT_SET=1&QUERY_ID=" + queryId;
    Element root = XmlUtils.requestXML(loadedURL, null);
    if (root.getNodeName().equals(AggregationResultSet.RESULT_SET_TAG))
      return new AggregationResultSet(root);

    // result set not found
    return null;
  }

  /**
   * Request cancelation of an active persistent query.  Removes query from
   * aggregation agent's blackboard.  Cancels all collection activity related
   * to this query.  Also cancels all alerts attached to this query.
   *
   * @param queryId id of query result adapter to be cancelled.
   *
   * @return true if successful; false otherwise
   */
  public boolean cancelQuery(String queryId)
  {
    String loadedURL =
        aggregationURL + "&CANCEL_QUERY=1&QUERY_ID=" + queryId;
    String response = XmlUtils.requestString(loadedURL, null);
    return response.equals("0");
  }

  /**
   * Send an update to an active persistent query.  Updates query on
   * aggregation agent's blackboard.  
   *
   * @param qra the query result adapter to be updated.
   *
   * @return true if successful; false otherwise
   */
  public boolean updateQuery(QueryResultAdapter qra)
  {
    
    String loadedURL =
        aggregationURL + "&UPDATE_QUERY=1&QUERY_ID=" + qra.getID();
    String response = XmlUtils.requestString(loadedURL, qra.getQuery().toXml().trim());
    return response.equals("0");
  }

  /**
   * Request cancelation of an active alert.
   *
   * @param queryId   id of alert's query result adapter
   * @param alertName name of alert
   *
   * @return true if successful; false otherwise
   */
  public boolean cancelAlert(String queryId, String alertName)
  {
    boolean success = false;
    try {
      String encodedAlertName = URLEncoder.encode(alertName, "UTF-8");
      String loadedURL = aggregationURL + "&CANCEL_ALERT=1&QUERY_ID=" +
                        queryId + "&ALERT_NAME=" + encodedAlertName;
      String response = XmlUtils.requestString(loadedURL, null);
      success = response.equals("0");
    } catch (Exception e)
    {
      e.printStackTrace();
    }
    return success;
  }

  /**
   * Create an monitor object that can be used to keep a set of monitored
   * alerts current by using a keep alive session which sends incremental
   * updates from the AggregationPSP.  Monitored alerts will be updated on the
   * client as soon as they change on the aggregation agent.
   *
   * @return an object that can be used to monitor alerts on the aggregation
   *         agent's log plan.
   */
  public AlertMonitor createAlertMonitor()
  {
    // setup keep alive task on the client
    return new AlertMonitor(keepAliveURL, Monitor.KEEP_ALIVE_METHOD);
  }

  /**
   * Create an monitor object that can be used to keep a set of monitored
   * alerts current by periodically pulling from a passive session on
   * AggregationPSP.  By using this type of monitor, instead of a keep alive
   * monitor, the load can be reduced on both the client and the aggregation
   * agent.  Increasing the wait peroid will reduce the load even more.
   * Updates to objects on the aggregation agent might be reported as long as
   * the wait period late.
   *
   * @param waitPeriod wait period between client pulls (update requests)
   *                   from aggregation agent in seconds.
   *
   * @return an object that can be used to monitor alerts on the aggregation
   *         agent's log plan.
   */
  public AlertMonitor createAlertMonitor(int waitPeriod)
  {
    // Setup pull task on client
    AlertMonitor alertMonitor =
      new AlertMonitor(aggregationURL, Monitor.PULL_METHOD);
    pullTimer.scheduleAtFixedRate(alertMonitor.getPullTask(), 0,
                                  waitPeriod * 1000);
    return alertMonitor;
  }

  /**
   * Create an monitor object that can be used to keep a set of monitored
   * result sets current by using a keep alive session which sends incremental
   * updates from the AggregationPSP.  Monitored result sets will be updated on
   * the client as soon as they change on the aggregation agent.
   *
   * @return an object that can be used to monitor result sets on the
   *         aggregation agent's log plan.
   */
  public ResultSetMonitor createResultSetMonitor()
  {
    // setup keep alive task on the client
    return new ResultSetMonitor(keepAliveURL, Monitor.KEEP_ALIVE_METHOD);
  }

  /**
   * Create an monitor object that can be used to keep a set of monitored
   * result sets current by periodically pulling from a passive session on
   * AggregationPSP.  By using this type of monitor, instead of a keep alive
   * monitor, the load can be reduced on both the client and the aggregation
   * agent.  Increasing the wait peroid will reduce the load even more.
   * Updates to objects on the aggregation agent might be reported as long as
   * the wait period late.
   *
   * @param waitPeriod wait period between client pulls (update requests)
   *                   from aggregation agent in seconds.
   *
   * @return an object that can be used to monitor result sets on the
   *         aggregation agent's log plan.
   */
  public ResultSetMonitor createResultSetMonitor(int waitPeriod)
  {
    // Setup pull task on client
    ResultSetMonitor resultSetMonitor =
      new ResultSetMonitor(aggregationURL, Monitor.PULL_METHOD);
    pullTimer.scheduleAtFixedRate(resultSetMonitor.getPullTask(), 0,
                                  waitPeriod * 1000);
    return resultSetMonitor;
  }

  /**
   * Get the value of a system property from the aggregation agent's
   * environment. (e.g. "org.cougaar.core.agent.startTime")
   *
   * @param propertyName the name of the property.
   * @return the value of the property.
   */
   public String getSystemProperty(String propertyName)
   {
     String propertyValue =
       XmlUtils.requestString(aggregationURL +
                              "&GET_SYSTEM_PROPERTY=1&PROPERTY_NAME=" +
                              propertyName, null);
     return propertyValue.trim();
   }
}