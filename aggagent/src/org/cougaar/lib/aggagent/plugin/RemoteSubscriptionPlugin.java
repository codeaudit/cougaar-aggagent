/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.util.*;
import org.cougaar.lib.aggagent.query.*;
import org.cougaar.lib.aggagent.session.*;
import org.cougaar.lib.aggagent.domain.*;

import org.cougaar.util.*;
import org.cougaar.core.node.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.component.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.service.*;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.domain.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.MessageTransportService;

/**
 * This Plugin services remote subscription requests.  It sends data back to an aggregation 
 * as requested.  It depends on the presence of the AggDomain.
 */
public class RemoteSubscriptionPlugin extends ComponentPlugin
{
  private boolean debug;
  private Object lock = new Object();
  private IncrementalSubscription messageSub;
  protected ClusterIdentifier me;

  public void setupSubscriptions()
  {
    me = getBindingSite().getAgentIdentifier();
    messageSub = subscribeIncr(new MessageSeeker(getBindingSite().getAgentIdentifier()));
  }


  public void execute () {

    if (log.isDebugEnabled()) log.debug("RemotePlugin:("+me+") Nmessages= "+messageSub.getCollection().size());

    // process new messages
    for(Enumeration e = messageSub.getAddedList(); e.hasMoreElements();)
    {
      receiveMessage((XMLMessage)e.nextElement());
    }

    // process changed subscriptions
    synchronized (lock)
    {
      Iterator iter = queryMap.keySet().iterator();
      while (iter.hasNext()) {
        IncrementalSubscription sub = (IncrementalSubscription) iter.next();
        if (sub.hasChanged())
          ((BBSession) queryMap.get(sub)).subscriptionChanged();
      }
    }
  }

  /**
   * Receive a message.
   * Happens when an agg agent sends me a message.
   */
  private void receiveMessage(XMLMessage xmsg) {
    try {
      if (log.isDebugEnabled()) log.debug("RemotePlugin:("+me+") receiveMessage "+xmsg);
      getBlackboardService().publishRemove(xmsg);
      Element root = XmlUtils.parse(xmsg.getText());
      String requestName = root.getNodeName();
      if (log.isDebugEnabled()) log.debug("RemotePlugin:("+me+") Got message: "+requestName+":"+root.toString());

      if (requestName.equals("transient_query_request"))
      {
        transientQuery(root, createAggAddress(xmsg.getSource().getAddress()));
      }
      else if (requestName.equals("push_request"))
      {
        createPushSession(root, createAggAddress(xmsg.getSource().getAddress()));
      }
      else if (requestName.equals("update_request"))
      {
        returnUpdate(root);
      }
      else if (requestName.equals("pull_request"))
      {
        createPullSession(root, createAggAddress(xmsg.getSource().getAddress()));
      }
      else if (requestName.equals("cancel_session_request"))
      {
        cancelSession(root);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }




  private void transientQuery (Element root, MessageAddress originator)
      throws Exception
  {
    UnaryPredicate objectSeeker = ScriptSpec.makeUnaryPredicate(
      XmlUtils.getChildElement(root, "unary_predicate"));
    if (objectSeeker == null)
      throw new Exception("Could not create unary predicate");

    IncrementFormat formatter = ScriptSpec.makeIncrementFormat(
      XmlUtils.getChildElement(root, "xml_encoder"));
    if (formatter == null)
      throw new Exception("Could not create formatter");

    RemoteBlackboardSubscription tempSubscription =
      new RemoteBlackboardSubscription(getBlackboardService(), objectSeeker, true);

    UpdateDelta del = new UpdateDelta(
      root.getAttribute("cluster_id"), root.getAttribute("query_id"), "");
    // Use xml encoder to encode data from blackboard
    tempSubscription.open();
    try {
      formatter.encode(del, tempSubscription);
    }
    catch (Throwable err) {
      if (err instanceof ThreadDeath)
        throw (ThreadDeath) err;
      del.setErrorReport(err);
    }
    tempSubscription.close();

    // Send response message
    sendMessage(originator, del.toXml());
  }

  private void createPushSession (Element root, MessageAddress originator)
    throws Exception {
    String queryId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    UnaryPredicate seeker = ScriptSpec.makeUnaryPredicate(
      XmlUtils.getChildElement(root, "unary_predicate"));
    if (seeker == null)
      throw new Exception("Could not create unary predicate");

    IncrementFormat formatter = ScriptSpec.makeIncrementFormat(
      XmlUtils.getChildElement(root, "xml_encoder"));
    if (formatter == null)
      throw new Exception("Could not create formatter");

    new RemotePushSession(
      String.valueOf(idCounter++), queryId, formatter, requester, seeker);
  }

  private int idCounter = 0;
  private HashMap queryMap = new HashMap();

  // "BB" stands for "Blackboard".  This is the abstract base class for the
  // RemoteSession implementations used by this Plugin.  It adds the ability
  // to cancel, to respond to subscription events from the host agent, and send
  // updates via COUGAAR messaging (all abstractly).
  private abstract class BBSession extends RemoteSession {
    protected MessageAddress requester;

    protected BBSession (String k, String q, IncrementFormat f, String r) {
      super(k, q, f);
      setAgentId(getBindingSite().getAgentIdentifier().toString());
      requester = createAggAddress(r);
    }

    public abstract void cancel ();

    public abstract void subscriptionChanged ();

    public abstract void pushUpdate ();
  }

  // This is the implementation of RemoteSession used for the PUSH method.  It
  // always sends notification immediately whenever the managed Subscription
  // is updated by the host agent.
  private class RemotePushSession extends BBSession {
    private SubscriptionAccess data = null;
    private IncrementalSubscription rawData = null;

    public RemotePushSession (
        String k, String q, IncrementFormat f, String r, UnaryPredicate p)
    {
      super(k, q, f, r);
      synchronized (lock)
      {
        rawData = subscribeIncr(new ErrorTrapPredicate(p));
        data = new SubscriptionWrapper(rawData);
        queryMap.put(rawData, this);
      }
    }

    public void cancel () {
      synchronized (lock)
      {
        queryMap.remove(rawData);
        getBlackboardService().unsubscribe(rawData);
      }
    }

    public void subscriptionChanged () {
      pushUpdate();
    }

    public SubscriptionAccess getData () {
      return data;
    }

    public void pushUpdate () {
      if (log.isDebugEnabled()) log.debug("Updating session to agg("+me+"): " + getQueryId());
      sendMessage(requester, createUpdateDelta().toXml());
    }
  }
  /**
   * Doesn't actually send a message, but published an object that
   * causes a message to be sent.
   */
  protected void sendMessage (MessageAddress address, String message) {
    if (log.isDebugEnabled()) log.debug("RemoteSubPlugins:("+me+"):sendMessage from: " +
      getBindingSite().getAgentIdentifier() + " to " + address.getAddress());
    XMLMessage msg = new XMLMessage(
      getBindingSite().getAgentIdentifier(), address, message);
    getBlackboardService().publishAdd(msg);
    if (log.isDebugEnabled()) log.debug("RemoteSubPlugins:("+me+"):sendMessage:  done publishized it");
  }

  // This is the implementation of RemoteSession used for the PULL method.  It
  // uses the RemoteBlackboardSubscription class to defer event notification
  // until requested by the client.
  private class RemotePullSession extends BBSession {
    private RemoteBlackboardSubscription rbs;

    public RemotePullSession (
        String k, String q, IncrementFormat f, String r, UnaryPredicate p)
    {
      super(k, q, f, r);
      synchronized (lock)
      {
        rbs = new RemoteBlackboardSubscription(
          getBlackboardService(), new ErrorTrapPredicate(p));
        queryMap.put(rbs.getSubscription(), this);
      }
    }

    public void pushUpdate () {
      if (log.isDebugEnabled()) log.debug("Updating session to agg("+me+"): " + getQueryId());
      rbs.open();
      UpdateDelta del = createUpdateDelta();
      rbs.close();
      sendMessage(requester, del.toXml());
    }

    public void cancel () {
      synchronized (lock)
      {
        queryMap.remove(rbs.getSubscription());
        rbs.shutDown();
      }
    }

    public void subscriptionChanged () {
      rbs.subscriptionChanged();
    }

    public SubscriptionAccess getData () {
      return rbs;
    }
  }


  private void cancelSession (Element root) throws Exception {
    String qId = root.getAttribute("query_id");
    BBSession match = findSessionById(qId);
    if (match != null)
      match.cancel();
    else
      if (log.isWarnEnabled()) log.warn("Error cancelling session ("+me+")" + qId + " at " +
        getBindingSite().getAgentIdentifier().getAddress());
  }

  private BBSession findSessionById (String id) {
    Iterator iter = queryMap.values().iterator();
    BBSession found = null;
    while (iter.hasNext()) {
      BBSession bbs = (BBSession) iter.next();
      if (bbs.getQueryId().equals(id)) {
        found = bbs;
        break;
      }
    }
    return found;
  }

  private void createPullSession (Element root, MessageAddress originator)
    throws Exception {
    String queryId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    UnaryPredicate seeker = ScriptSpec.makeUnaryPredicate(
      XmlUtils.getChildElement(root, "unary_predicate"));
    if (seeker == null)
      throw new Exception("Could not create unary predicate");

    IncrementFormat formatter = ScriptSpec.makeIncrementFormat(
      XmlUtils.getChildElement(root, "xml_encoder"));
    if (formatter == null)
      throw new Exception("Could not create formatter");

    new RemotePullSession(
      String.valueOf(idCounter++), queryId, formatter, requester, seeker);
    if (log.isDebugEnabled()) log.debug("Pull session created("+me+")");
  }

  private void returnUpdate (Element root) throws Exception {
    String qId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    BBSession bbs = findSessionById(qId);
    if (bbs != null)
      bbs.pushUpdate();
    else
      if (log.isWarnEnabled()) log.warn(
        "Error: ("+me+") query not found while updating " + qId + " for " + requester);
  }
  
  protected LoggingService log;
  public void setLoggingService(LoggingService ls) {
    if ((ls == null) && log.isDebugEnabled())
      log.debug("Logger ("+me+")being reset to null");
    log = ls;
    if ((log != null) && log.isDebugEnabled())
      log.debug("Logging ("+me+")initialized");
  }

  public LoggingService getLoggingService() {
    return log;
  }

  protected IncrementalSubscription subscribeIncr (UnaryPredicate p) {
    return (IncrementalSubscription) getBlackboardService().subscribe(p);
  }
  protected static final MessageAddress createAggAddress(String agentName) {
//    return new ClusterIdentifier(agentName + "-agg");
    return new ClusterIdentifier(agentName);
  }

}
