package org.cougaar.lib.aggagent.plugin;

import java.io.*;
import java.net.*;
import java.util.*;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.util.*;
import org.cougaar.lib.aggagent.query.*;
import org.cougaar.lib.aggagent.session.*;

import org.cougaar.util.*;
import org.cougaar.core.society.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.*;


public class RemoteSubscriptionPlugIn extends AggregationPlugIn implements MessageTransportClient
{
  private static final boolean debug = false;
  private Object lock = new Object();

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
  }


  public void execute () {
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
  public void receiveMessage(Message message) {
    try {
      XMLMessage xmsg = (XMLMessage)message;
      Element root = XmlUtils.parse(xmsg.getText());
      String requestName = root.getNodeName();

System.out.println("RemotePlugin: Got message: "+requestName+":"+root.toString());

      if (requestName.equals("transient_query_request"))
      {
        transientQuery(root, createAggAddress(message.getOriginator().getAddress()));
      }
      else if (requestName.equals("push_request"))
      {
        createPushSession(root, createAggAddress(message.getOriginator().getAddress()));
      }
      else if (requestName.equals("update_request"))
      {
        returnUpdate(root);
      }
      else if (requestName.equals("pull_request"))
      {
        createPullSession(root, createAggAddress(message.getOriginator().getAddress()));
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

  // Wrap an IncrementalSubscription within the SubscriptionAccess interface.
  private static class SubscriptionWrapper implements SubscriptionAccess {
    private IncrementalSubscription sub = null;

    public SubscriptionWrapper (IncrementalSubscription s) {
      sub = s;
    }

    public IncrementalSubscription getSubscription () {
      return sub;
    }

    public Collection getAddedCollection () {
      return sub.getAddedCollection();
    }
    public Collection getChangedCollection () {
      return sub.getChangedCollection();
    }
    public Collection getRemovedCollection () {
      return sub.getRemovedCollection();
    }
    public Collection getMembership () {
      return sub.getCollection();
    }
  }

  // "BB" stands for "Blackboard".  This is the abstract base class for the
  // RemoteSession implementations used by this PlugIn.  It adds the ability
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
      System.out.println("Updating session to agg: " + getQueryId());
      sendMessage(requester, createUpdateDelta().toXml());
    }
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
      System.out.println("Updating session to agg: " + getQueryId());
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
      System.out.println("Error cancelling session " + qId + " at " +
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
    System.out.println("Pull session created");
  }

  private void returnUpdate (Element root) throws Exception {
    String qId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    BBSession bbs = findSessionById(qId);
    if (bbs != null)
      bbs.pushUpdate();
    else
      System.err.println(
        "Error:  query not found while updating " + qId + " for " + requester);
  }
}