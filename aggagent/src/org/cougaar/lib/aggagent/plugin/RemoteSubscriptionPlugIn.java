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


  public void execute()
  {
    Iterator iter = queryMap.keySet().iterator();
    while (iter.hasNext()) {
      IncrementalSubscription sub = (IncrementalSubscription)iter.next();
      if (sub.hasChanged()) {
        RemotePushSession rps = (RemotePushSession)queryMap.get(sub);
        rps.subscriptionChanged();
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
        returnUpdate(root, createAggAddress(message.getOriginator().getAddress()));
      }
      else if (requestName.equals("pull_request"))
      {
        createPullSession(root, createAggAddress(message.getOriginator().getAddress()));
      }
      else if (requestName.equals("cancel_session_request"))
      {
        cancelSession(root, createAggAddress(message.getOriginator().getAddress()));
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
    } catch (Exception e) {
      tempSubscription.close();
      throw e;
    }
    tempSubscription.close();

    // Send response message
    sendMessage(originator, del.toXml());
  }



  private void createPushSession (Element root, MessageAddress originator)
    throws Exception {
    String queryId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    UnaryPredicate objectSeeker = ScriptSpec.makeUnaryPredicate(
      XmlUtils.getChildElement(root, "unary_predicate"));
    if (objectSeeker == null)
      throw new Exception("Could not create unary predicate");

    IncrementFormat formatter = ScriptSpec.makeIncrementFormat(
      XmlUtils.getChildElement(root, "xml_encoder"));
    if (formatter == null)
      throw new Exception("Could not create formatter");


    IncrementalSubscription sub =
      (IncrementalSubscription) getBlackboardService().subscribe(objectSeeker);
    RemotePushSession ps = new RemotePushSession(String.valueOf(idCounter++),
      queryId, formatter, requester, sub);

    queryMap.put(sub, ps);
  }

  private int idCounter = 0;
  private HashMap queryMap = new HashMap();

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

  private class RemotePushSession {

    public String requester;
    public String key;
    public String queryId;
    public IncrementFormat formatter;
    private SubscriptionAccess data = null;
    private IncrementalSubscription rawData = null;

    protected RemotePushSession (String k, String qId, IncrementFormat f,
        String req, SubscriptionAccess acc)
    {
      key = k;
      queryId = qId;
      formatter = f;
      requester = req;
      data = acc;
    }

    public RemotePushSession (String k, String qId, IncrementFormat f,
        String req, IncrementalSubscription sub)
    {
      this(k, qId, f, req, new SubscriptionWrapper(sub));
      rawData = sub;
    }

    public void pushUpdate () {
      System.out.println("Updating session to agg: " + queryId);

      UpdateDelta del = new UpdateDelta(
        getBindingSite().getAgentIdentifier().toString(), queryId, key);
      formatter.encode(del, getData());

      sendMessage(createAggAddress(requester), del.toXml());
    }

    public void subscriptionChanged () {
      pushUpdate();
    }

    public IncrementalSubscription getSubscription() {
      return rawData;
    }

    public SubscriptionAccess getData () {
      return data;
    }
  }

  private class RemotePullSession extends RemotePushSession {
    private RemoteBlackboardSubscription rbs;

    public RemotePullSession (String k, String queryId, IncrementFormat f,
        String requester, RemoteBlackboardSubscription sub)
    {
      super(k, queryId, f, requester, sub.getSubscription());
      this.rbs = sub;
    }

    public void subscriptionChanged () {
      rbs.subscriptionChanged();
    }

    public SubscriptionAccess getData () {
      return rbs;
    }
  }


  private void cancelSession (Element root, MessageAddress originator)
      throws Exception
  {
    String query_id = root.getAttribute("query_id");
    Iterator iter = queryMap.values().iterator();
    boolean found = false;
    while (iter.hasNext()) {
      RemotePushSession rps = (RemotePushSession)iter.next();
      if (rps.queryId.equals(query_id)) {
        found = true;
        IncrementalSubscription s = rps.getSubscription();
        getBlackboardService().unsubscribe(s);
        queryMap.remove(s);
        break;
      }
    }
    if (!found)
      System.out.println("Error cancelling session " + query_id + " at " +
        getBindingSite().getAgentIdentifier().getAddress());
  }



  private void createPullSession (Element root, MessageAddress originator)
    throws Exception {
    String queryId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    UnaryPredicate objectSeeker = ScriptSpec.makeUnaryPredicate(
      XmlUtils.getChildElement(root, "unary_predicate"));
    if (objectSeeker == null)
      throw new Exception("Could not create unary predicate");

    IncrementFormat formatter = ScriptSpec.makeIncrementFormat(
      XmlUtils.getChildElement(root, "xml_encoder"));
    if (formatter == null)
      throw new Exception("Could not create formatter");

    RemoteBlackboardSubscription rbs = new RemoteBlackboardSubscription(
      getBlackboardService(), objectSeeker);
    RemotePullSession ps = new RemotePullSession(String.valueOf(idCounter++),
      queryId, formatter, requester, rbs);

    System.out.println("Pull session created");
    queryMap.put(rbs.getSubscription(), ps);
  }


  private RemotePushSession findSessionById(String id) {
    Iterator iter = queryMap.values().iterator();
    RemotePushSession found = null;
    while (iter.hasNext()) {
      RemotePushSession rps = (RemotePushSession)iter.next();
      if (rps.queryId.equals(id)) {
        found = rps;
        break;
      }
    }
    return found;
  }

  private void returnUpdate (Element root, MessageAddress originator) throws Exception {
    String queryId = root.getAttribute("query_id");
    String requester = root.getAttribute("requester");

    RemotePullSession rps = (RemotePullSession)findSessionById(queryId);
    if (rps == null) {
      System.err.println("Error updating query: "+queryId+" : not found");
    } else {
      rps.rbs.open();
      rps.pushUpdate();
      rps.rbs.close();
    }
  }
}