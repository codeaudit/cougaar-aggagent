/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
package org.cougaar.lib.aggagent.plugin;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.lib.aggagent.util.Enum.QueryType;
import org.cougaar.lib.aggagent.util.Enum.UpdateMethod;
import org.cougaar.util.UnaryPredicate;
import org.w3c.dom.Element;

/**
 * Receives aggregation requests in the form of QueryResultAdapter objects.
 */
public class AggregationPlugin extends ComponentPlugin
{
  // Subscribe to all QueryResultAdapter objects
  private IncrementalSubscription querySub;
  private IncrementalSubscription messageSub;

  private static class QuerySeeker implements UnaryPredicate
  {
    public boolean execute (Object o)
    {
      return o instanceof QueryResultAdapter;
    }
  }

  /**
   *  A convenience method for creating IncrementalSubscriptions
   */
  protected IncrementalSubscription subscribeIncr (UnaryPredicate p) {
    return (IncrementalSubscription) getBlackboardService().subscribe(p);
  }

  protected MessageAddress me;
  
  public void setupSubscriptions()
  {
    me = getAgentIdentifier();
    querySub = subscribeIncr(new QuerySeeker());
    messageSub = subscribeIncr(new MessageSeeker(true));
  }

  public void execute()
  {

    if (log != null && log.isDebugEnabled()) log.debug("("+me+")AggPlugin: execute");

    checkNewMessages();
    checkNewQueries();
    checkUpdatedQueries();
    checkRemovedQueries();
  }

  private void checkNewMessages() {
    // Only the changed messages are interesting.
    // The old ones will be deleted.
    // We need to ignore the ones that have *already* been deleted, though.
    Collection removedARs = messageSub.getRemovedCollection();
    for(Enumeration e = messageSub.getChangedList(); e.hasMoreElements();)
    {
      if (!removedARs.contains(e))
        receiveMessage((AggRelay)e.nextElement());
    }
  }

  private void checkNewQueries() {
    for(Enumeration e = querySub.getAddedList(); e.hasMoreElements();)
    {
      QueryResultAdapter qra = (QueryResultAdapter)e.nextElement();
      AggregationQuery aq = qra.getQuery();

      if (aq.getType() == QueryType.PERSISTENT)
      {
        if (aq.getUpdateMethod() == UpdateMethod.PUSH)
        {
          // create a push session on each of the source clusters listed
          // in query
          for (Enumeration sc = aq.getSourceClusters(); sc.hasMoreElements();)
          {
            String clusterString = (String)sc.nextElement();
            requestPushSession(qra.getID(), clusterString, qra);
          }
        }
        else
        {
          for (Enumeration sc = aq.getSourceClusters(); sc.hasMoreElements();)
          {
            String clusterString = (String)sc.nextElement();
            requestPullSession(qra.getID(), clusterString, qra);
          }
          // set up the aggregator timer
          if (qra.getQuery().getPullRate() >= 0)
          {
            java.util.Timer pullTimer = new java.util.Timer();
            long waitPeriod = (long)(qra.getQuery().getPullRate() * 1000);
            pullTimer.scheduleAtFixedRate(getTimerTask(qra), 0, waitPeriod);
            qra.getQuery().setPullTimer(pullTimer);
          }
        }
      }
      else
      {
        // query each cluster
        for (Enumeration sc = aq.getSourceClusters(); sc.hasMoreElements();)
        {
          String clusterString = (String)sc.nextElement();
          queryCluster(clusterString, qra);
        }
      }
    }
  }

  private void checkUpdatedQueries() {
    for(Enumeration e = querySub.getChangedList(); e.hasMoreElements();)
    {
      QueryResultAdapter qra = (QueryResultAdapter)e.nextElement();
      AggregationQuery aq = qra.getQuery();
      String queryId = qra.getID();

      try {
        // ignore update of transient queries
        if (qra.getQuery().getType() == QueryType.PERSISTENT) {
          Vector removedClusters = qra.getAndResetRemovedClusters();
          // remove the old cluster relays
          if (removedClusters != null && !removedClusters.isEmpty()) {
            Iterator iter = messageSub.getCollection().iterator();
            while(iter.hasNext()) {
                AggRelay ar = (AggRelay) iter.next();
                XMLMessage xmsg = (XMLMessage)ar.getContent();
                Element root = XmlUtils.parse(xmsg.getText());
                String this_query_id = root.getAttribute("query_id");
                Iterator targets = ar.getTargets().iterator();
                while (targets.hasNext()) {
                  String this_cluster_id = targets.next().toString();
                  if (queryId.equals(this_query_id) && removedClusters.contains(this_cluster_id)) {
                    getBlackboardService().publishRemove(ar);
                  }
                }
            }
            if (log != null && log.isDebugEnabled()) log.debug("("+me+")Updating remote session "+qra.getID());
          }
          Vector addedClusters = qra.getAndResetAddedClusters();
          // add the new cluster relays
          if (addedClusters != null && !addedClusters.isEmpty()) {
            if (log != null && log.isDebugEnabled()) log.debug("("+me+")Updating remote session "+qra.getID());
            Enumeration newClusters = addedClusters.elements();
            while (newClusters.hasMoreElements()) {
              String clusterId = (String) newClusters.nextElement();
              if (aq.getUpdateMethod() == UpdateMethod.PUSH) {
                requestPushSession(queryId, clusterId, qra);
              } else {
                requestPullSession(queryId, clusterId, qra);
              }
            }
          }
        }
      }  catch (Exception ioe) {
        if (log != null && log.isErrorEnabled()) log.error("AggPlugin:("+me+"):error updating session"+ioe);
      }
    }
  }
  
  private void checkRemovedQueries() {
    for (Enumeration e = querySub.getRemovedList(); e.hasMoreElements();)
    {
      QueryResultAdapter qra = (QueryResultAdapter)e.nextElement();
      AggregationQuery aq = qra.getQuery();
      String queryId = qra.getID();

      // ignore removal of transient queries
      if (aq.getType() == QueryType.PERSISTENT)
      {
        if (aq.getUpdateMethod() == UpdateMethod.PULL)
        {
          // cancel local pull session
          qra.getQuery().getPullTimer().cancel();
        }

        // cancel session on each of the source clusters listed in query
        if (log != null && log.isDebugEnabled()) log.debug("("+me+")Cancelling remote session "+queryId);
        cancelRemoteSession(queryId);
      }
    }
  }

  private String frameRequestXml (String action, String qId, String cId,
      boolean requester, AggregationQuery query)
  {
    InverseSax request = new InverseSax();
    request.addElement(action);
    request.addAttribute("query_id", qId);
    if (cId != null)
      request.addAttribute("cluster_id", cId);
    if (requester)
      request.addAttribute(
        "requester", getAgentIdentifier().toString());
    if (query != null)
      query.includeScriptXml(request);
    request.endElement();
    return request.toString();
  }

  /**
   * send query to cluster
   */
  private void queryCluster (String cId, QueryResultAdapter qra) {
    sendMessage(createAggAddress(cId), frameRequestXml(
      "transient_query_request", qra.getID(), cId, false, qra.getQuery()));
  }

  /**
   * Send request to given Generic Plugin URL for a push session back to
   * this cluster.
   */
  private void requestPushSession (
      String queryId, String clusterId, QueryResultAdapter qra)
  {
    sendMessage(createAggAddress(clusterId),
      frameRequestXml("push_request", queryId, null, true, qra.getQuery()));
    if (log != null && log.isDebugEnabled()) log.debug("AggPlugin:("+me+"):requestPushSession:  sent message");
  }

  /**
   * Send request to given Generic Plugin URL for a pull session back to
   * this cluster.
   */
  private void requestPullSession (
      String queryId, String clusterId, QueryResultAdapter qra)
  {
    sendMessage(createAggAddress(clusterId),
      frameRequestXml("pull_request", queryId, null, true, qra.getQuery()));
  }

  private TimerTask getTimerTask(QueryResultAdapter qra) {
    return new PullTimerTask(qra);
  }
    
  private void cancelRemoteSession(String queryId) {
      // todo: this is horribly inefficient.
      try {
          Iterator iter = messageSub.getCollection().iterator();
          while(iter.hasNext()) {
              AggRelay ar = (AggRelay) iter.next();
              XMLMessage xmsg = (XMLMessage)ar.getContent();
              Element root = XmlUtils.parse(xmsg.getText());
              String this_id = root.getAttribute("query_id");
              if (queryId.equals(this_id)) {
                  getBlackboardService().publishRemove(ar);
                  if (log != null && log.isDebugEnabled()) log.debug("AggPlugin:("+me+"):canceled session at "+ar.getTargets().iterator().next());
              }
          }
      } catch (Exception ioe) {
          if (log != null && log.isErrorEnabled()) log.error("AggPlugin:("+me+"):error canceling session"+ioe);
      }
  }

  /**
   * Receive a message.
   * Happens when a remote cluster sends me an update.
   */
  private void receiveMessage(AggRelay relay) {
    try {
      if (log != null && log.isDebugEnabled()) log.debug("AggPlugin:("+me+"):receiveMessage");
      XMLMessage xmsg = (XMLMessage)relay.getResponse();
      Element root = XmlUtils.parse(xmsg.getText());
//      String requestName = root.getNodeName();

      //
      // Handle a response to one of my previous queries
      //
      UpdateDelta delta = new UpdateDelta(root);
 
      String updatedQuery = delta.getQueryId();
      String updatedCluster = delta.getAgentId();

      if (log != null && log.isDebugEnabled())
        log.debug("AggPlugin ("+me+")Received a message at " +
        getAgentIdentifier() +
        " --- Query update to :" + updatedQuery + " from " + updatedCluster);

      // find query result adapter on blackboard
      Iterator updatedQueries = getBlackboardService().query(
        new QueryRAFinder(updatedQuery)).iterator();

      if (updatedQueries.hasNext()) {
        QueryResultAdapter qra = (QueryResultAdapter)updatedQueries.next();

        // update query result set based on reported changes
        qra.updateResults(delta);

        // publish changes to blackboard
        getBlackboardService().publishChange(qra);
        // Am I done with thie relay?
        if (qra.getQuery().getType().equals(QueryType.TRANSIENT))
            getBlackboardService().publishRemove(relay);
      }
      else {
        if (log != null && log.isErrorEnabled())
        log.error("AggPlugin: unable to find query ID: "+updatedQuery);
      }
    } catch (Exception ex) {
      System.err.println("Error receiving message");
      ex.printStackTrace();
    }
  }


  /**
   * Doesn't actually send a message, but published an object that
   * causes a message to be sent.
   */
  protected void sendMessage (MessageAddress address, String message) {
    if (log != null && log.isDebugEnabled()) log.debug("AggPlugins:("+me+"):sendMessage from: " +
      getAgentIdentifier() + " to " + address.getAddress());
    XMLMessage msg = new XMLMessage(message);
    AggRelay relay = new AggRelay(getUIDService().nextUID(), me, address, msg, null);
    // I need to flag this relay as one that I created, so I don't try to service it, too.
    relay.setLocal(true);
    getBlackboardService().publishAdd(relay);
    if (log != null && log.isDebugEnabled()) log.debug("AggPlugins:("+me+"):sendMessage:  done publishized it");
  }

  protected static final MessageAddress createAggAddress(String agentName) {
    return MessageAddress.getMessageAddress(agentName);
  }

  private static class QueryRAFinder implements UnaryPredicate
  {
    String queryId = null;

    public QueryRAFinder(String queryId)
    {
      this.queryId = queryId;
    }

    public boolean execute(Object o)
    {
      if (o instanceof QueryResultAdapter)
      {
        QueryResultAdapter qra = (QueryResultAdapter)o;
        return qra.checkID(queryId);
      }
      return false;
    }
  }

  private class PullTimerTask extends TimerTask {
    private QueryResultAdapter qra;

    public PullTimerTask (QueryResultAdapter qra) {
      this.qra = qra;
    }

    public void run () {
      String reqStr = frameRequestXml(
        "update_request", qra.getID(), null, true, null);

      Enumeration sources = qra.getQuery().getSourceClusters();
      while (sources.hasMoreElements())
        sendMessage(createAggAddress((String) sources.nextElement()), reqStr);
    }
  }

  protected LoggingService log = null;
  
  /** Holds value of property UIDService. */
  private UIDService UIDService;
  
  public void setLoggingService(LoggingService ls) {
    if ((ls == null) && (log != null) && log.isDebugEnabled())
      log.debug("Logger ("+me+")being reset to null");
    log = ls;
    if ((log != null) && log.isDebugEnabled())
      log.debug("Logging ("+me+")initialized");
  }

  public LoggingService getLoggingService() {
    return log;
  }
  
  /** Getter for property UIDService.
   * @return Value of property UIDService.
   */
  public UIDService getUIDService() {
      return this.UIDService;
  }
  
  /** Setter for property UIDService.
   * @param UIDService New value of property UIDService.
   */
  public void setUIDService(UIDService UIDService) {
      this.UIDService = UIDService;
  }
  
}

