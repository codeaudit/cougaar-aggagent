package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;

import org.w3c.dom.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.plugin.Const;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 * Create a passive session on a set of remote source clusters.  Periodically
 * polls passive sessions on all source clusters and publish changes to
 * log plan.
 */
public class PullSession extends Session
{
  private Collection servers = null;
  private BlackboardService bs = null;
  private QueryResultAdapter qra = null;
  private String requesterId = null;
  private TimerTask tt = null;

  public PullSession(String sessionKey, String requesterId,
                     QueryResultAdapter qra, BlackboardService bs,
                     NameService nameServer)
  {
    super(sessionKey, qra.getID());
    this.requesterId = requesterId;
    this.qra = qra;
    this.bs = bs;

    servers = new LinkedList();
    AggregationQuery aq = qra.getQuery();
    for (Enumeration sc = aq.getSourceClusters(); sc.hasMoreElements();)
    {
      String clusterString = (String)sc.nextElement();
      ServerInfo si = new ServerInfo();
      si.clusterId = clusterString;
      si.serverURL = requestPassiveSession(clusterString, nameServer);
      servers.add(si);
    }

    tt = new TimerTask() {
        public void run()
        {
          sendUpdate(null);
        }
      };
  }

  public TimerTask getTimerTask()
  {
    return tt;
  }

  /**
   *  End this session; cancel passive sessions, stop pulling from URLs.
   */
  public void endSession () {
    tt.cancel();
  }

  /**
   * Send request to given Generic Plugin URL for a passive session
   */
  private String requestPassiveSession(String sourceCluster, NameService ns)
  {
    StringBuffer passiveSessionRequest = new StringBuffer(Const.XML_HEAD);
    passiveSessionRequest.append("<passive_request query_id=\"");
    passiveSessionRequest.append(qra.getID());
    passiveSessionRequest.append("\" requester=\"");
    passiveSessionRequest.append(requesterId);
    passiveSessionRequest.append("\">");
    passiveSessionRequest.append(qra.getQuery().scriptXML());
    passiveSessionRequest.append("</passive_request>");

    String sourceURL = createSourceURL(sourceCluster, ns);
    String response =
      XmlUtils.requestString(sourceURL, passiveSessionRequest.toString());

    System.out.println(
                "AggregationPlugIn::createPassiveSession:  received response:");
    System.out.println(response);

    return sourceURL;
  }

  private void requestUpdate(ServerInfo si)
  {
    StringBuffer updateRequest = new StringBuffer(Const.XML_HEAD);
    updateRequest.append("<update_request query_id=\"");
    updateRequest.append(qra.getID());
    updateRequest.append("\" requester=\"");
    updateRequest.append(requesterId);
    updateRequest.append("\">");
    updateRequest.append("</update_request>");

    Element root = XmlUtils.requestXML(si.serverURL, updateRequest.toString());

    // update result set based on response
    qra.getResultSet().incrementalUpdate(si.clusterId, root);

    // publish changes to blackboard
    bs.openTransaction();
    bs.publishChange(qra);
    bs.closeTransaction();
  }

  /**
   *  Request an update of recent changes to the RemoteSubscriptions on
   *  source clusters.  Out parameter is ignored (only included for override).
   */
  public void sendUpdate (OutputStream out)
  {
    Iterator i = servers.iterator();
    while (i.hasNext())
    {
      ServerInfo si = (ServerInfo)i.next();
      requestUpdate(si);
    }
  }

  private String createSourceURL(String clusterID, NameService ns)
  {
    URL clusterURL = ns.lookupURL(clusterID);
    System.out.println("PullSession::createSourceURL:  " + clusterURL);
    String genericPSPURL = clusterURL.toString() + "test/generic.psp";

    return genericPSPURL;
  }

  private class ServerInfo
  {
    public String clusterId;
    public String serverURL;
  }
}