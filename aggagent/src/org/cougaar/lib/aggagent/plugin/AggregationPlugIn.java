package org.cougaar.lib.aggagent.plugin;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import org.cougaar.core.cluster.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.naming.NamingService;
import org.cougaar.lib.planserver.server.FDSProxy;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.lib.planserver.server.ProxyMapAdapter;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.*;
import org.cougaar.lib.aggagent.session.*;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.mts.*;
import org.cougaar.core.component.*;

public class AggregationPlugIn extends ComponentPlugin implements MessageTransportClient, ServiceRevokedListener
{
  private static final boolean debug = false;

  protected static MessageAddress myAddress;

  // Subscribe to all QueryResultAdapter objects
  private IncrementalSubscription querySub = null;

  protected MessageTransportService messenger = null;



  private class QuerySeeker implements UnaryPredicate
  {
    public boolean execute (Object o)
    {
      return o instanceof QueryResultAdapter;
    }
  }

  public void setupSubscriptions()
  {
    myAddress = createAggAddress(getBindingSite().getAgentIdentifier().toString());

    messenger = (MessageTransportService) getServiceBroker().getService(
      this, MessageTransportService.class, this);

    if (messenger != null)
      // One must explicitly register the MessageTransportClient; otherwise,
      // notification of messages will not be received (even though it was
      // passed in to the call to ServiceBroker::getService
      messenger.registerClient(this);
    else
      System.out.println(
        "  X\n  X\nMessageTransportService not granted.  Too bad.\n  X\n  X");

    querySub = (IncrementalSubscription)getBlackboardService().subscribe(new QuerySeeker());
  }

  public void execute()
  {

    System.out.println("AggPlugin: execute");

    checkNewQueries();
    checkRemovedQueries();


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
        for (Enumeration sc = aq.getSourceClusters(); sc.hasMoreElements();)
        {
          String clusterString = (String)sc.nextElement();
System.out.println("Cancelling remote session at "+clusterString);
          cancelRemoteSession(queryId, clusterString);
        }
      }
    }
  }

  /**
   * send query to cluster
   */
  private void queryCluster(String clusterString, QueryResultAdapter qra)
  {
    AggregationQuery query = qra.getQuery();
    MessageAddress sourceAddr = createAggAddress(clusterString);

    StringBuffer transientQueryRequest = new StringBuffer(Const.XML_HEAD);
    transientQueryRequest.append("<transient_query_request query_id=\"");
    transientQueryRequest.append(qra.getID());
    transientQueryRequest.append("\" cluster_id=\"");
    transientQueryRequest.append(clusterString);
    transientQueryRequest.append("\">");
    transientQueryRequest.append(query.scriptXML());
    transientQueryRequest.append("</transient_query_request>");

    sendMessage(sourceAddr, transientQueryRequest.toString());
  }

  /**
   * Send request to given Generic Plugin URL for a push session back to
   * this cluster.
   */
  private void requestPushSession(String query_id, String sourceCluster,
                                  QueryResultAdapter qra)
  {
    AggregationQuery query = qra.getQuery();

    StringBuffer pushSessionRequest = new StringBuffer(Const.XML_HEAD);
    pushSessionRequest.append("<push_request query_id=\"");
    pushSessionRequest.append(query_id);
    pushSessionRequest.append("\" requester=\"");
    pushSessionRequest.append(getBindingSite().getAgentIdentifier().toString());
    pushSessionRequest.append("\">");
    pushSessionRequest.append(query.scriptXML());
    pushSessionRequest.append("</push_request>");


    MessageAddress sourceAddr = createAggAddress(sourceCluster);
    sendMessage(sourceAddr, pushSessionRequest.toString());
    System.out.println("AggPlugIn::createPushSession:  sent message:");
  }

  /**
   * Send request to given Generic Plugin URL for a pull session back to
   * this cluster.
   */
  private void requestPullSession(String query_id, String sourceCluster,
                                  QueryResultAdapter qra)
  {
    AggregationQuery query = qra.getQuery();

    StringBuffer pushSessionRequest = new StringBuffer(Const.XML_HEAD);
    pushSessionRequest.append("<pull_request query_id=\"");
    pushSessionRequest.append(query_id);
    pushSessionRequest.append("\" requester=\"");
    pushSessionRequest.append(getBindingSite().getAgentIdentifier().toString());
    pushSessionRequest.append("\">");
    pushSessionRequest.append(query.scriptXML());
    pushSessionRequest.append("</pull_request>");


    MessageAddress sourceAddr = createAggAddress(sourceCluster);
    sendMessage(sourceAddr, pushSessionRequest.toString());
    System.out.println("AggPlugIn::createPullSession:  sent message:");

  }

  private TimerTask getTimerTask(QueryResultAdapter qra) {
    return new PullTimerTask(qra);
  }

  private void cancelRemoteSession(String query_id, String sourceCluster)
  {
    // send request to given Generic Plugin URL to cancel a push session back
    // to this cluster
    StringBuffer cancelSessionRequest = new StringBuffer(Const.XML_HEAD);
    cancelSessionRequest.append("<cancel_session_request query_id=\"");
    cancelSessionRequest.append(query_id);
    cancelSessionRequest.append("\" requester=\"");
    cancelSessionRequest.append(getBindingSite().getAgentIdentifier().toString());
    cancelSessionRequest.append("\"></cancel_session_request>");
    sendMessage(createAggAddress(sourceCluster), cancelSessionRequest.toString());
    System.out.println("AggPlugIn::cancelRemoteSession:  sent message:");
  }


  /**
   * Receive a message.
   * Happens when a remote cluster sends me an update.
   */
  public void receiveMessage(Message message) {
    try {
      // System.out.println(message);
      XMLMessage xmsg = (XMLMessage)message;

      Element root = XmlUtils.parse(xmsg.getText());
      String requestName = root.getNodeName();

      //
      // Handle a response to one of my previous queries
      //
      UpdateDelta delta = new UpdateDelta(root);

      String updatedQuery = delta.getQueryId();
      String updatedCluster = delta.getAgentId();

      System.out.println("AggPlugin Received a message at " +
        getBindingSite().getAgentIdentifier());
      System.out.println(
        "   Query update to :" + updatedQuery + " from " + updatedCluster);

      // find query result adapter on blackboard
      Iterator updatedQueries = getBlackboardService().query(
        new QueryRAFinder(updatedQuery)).iterator();

      if (updatedQueries.hasNext()) {
        QueryResultAdapter qra = (QueryResultAdapter)updatedQueries.next();

        // update query result set based on reported changes
        qra.updateResults(delta);

        // publish changes to blackboard
        getBlackboardService().openTransaction();
        getBlackboardService().publishChange(qra);
        getBlackboardService().closeTransaction();
      }
      else {
        System.err.println("AggPlugin: unable to find query ID: "+updatedQuery);
      }
    } catch (Exception ex) {
      System.err.println("Error receiving message");
      ex.printStackTrace();
    }


  }

  public MessageAddress getMessageAddress() {
    return myAddress;
  }
  public void serviceRevoked (ServiceRevokedEvent evt) {
    System.out.println("MessageTransportService Revoked.  Too bad.");
  }



  protected static class XMLMessage extends Message {
    private String text;
    public String getText() {
      return text;
    }
    public XMLMessage (MessageAddress self, MessageAddress other, String text) {
      super(self, other);
      this.text = text;
    }
    public String toString() {
      return this.getOriginator() + " TO "+getTarget()+"\n"+text;
    }
  }

  protected void sendMessage (MessageAddress address, String message) {
    System.out.println("AggPlugins::sendMessage from: " +
      getBindingSite().getAgentIdentifier() + " to " + address.getAddress());
    XMLMessage msg = new XMLMessage(
      getBindingSite().getAgentIdentifier(), address, message);
    messenger.sendMessage(msg);
    System.out.println("AggPlugins::sendMessage:  done");
  }



  protected static final MessageAddress createAggAddress(String agentName) {
    return new ClusterIdentifier(agentName + "-agg");
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
    public PullTimerTask(QueryResultAdapter qra) {
      this.qra = qra;
    }
    public void run() {
      StringBuffer updateRequest = new StringBuffer(Const.XML_HEAD);
      updateRequest.append("<update_request query_id=\"");
      updateRequest.append(qra.getID());
      updateRequest.append("\" requester=\"");
      updateRequest.append(getBindingSite().getAgentIdentifier().toString());
      updateRequest.append("\">");
      updateRequest.append("</update_request>");
      String updateRequestString = updateRequest.toString();

System.out.println("PULLTIMER: pulling "+qra.getID());

      Enumeration sources = qra.getQuery().getSourceClusters();
      while (sources.hasMoreElements()) {
        MessageAddress addr = createAggAddress((String)sources.nextElement());
        sendMessage(addr, updateRequestString);
      }
    }
  }

}