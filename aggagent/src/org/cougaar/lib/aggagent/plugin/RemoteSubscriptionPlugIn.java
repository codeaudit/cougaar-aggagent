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




  private void transientQuery(Element root, MessageAddress originator) throws Exception
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

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // Use xml encoder to encode data from blackboard
    tempSubscription.open();
    try {
      String query_id = root.getAttribute("query_id");
      String cluster_id = root.getAttribute("cluster_id");
      formatter.encode(out, tempSubscription, "", query_id, cluster_id);
    } catch (Exception e) {
      tempSubscription.close();
      throw e;
    }
    tempSubscription.close();

    // Send response message
    sendMessage(originator, new String(out.toByteArray()));
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

  private void updatePushSession(RemotePushSession rps) {
    System.out.println("Updating session to agg: "+rps.queryId);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    rps.formatter.encode(baos, rps, rps.key, rps.queryId, getBindingSite().getAgentIdentifier().toString());

    MessageAddress originator = createAggAddress(rps.requester);
    sendMessage(originator, new String(baos.toByteArray()));
  }


  private class RemotePushSession implements SubscriptionAccess {

    public String requester;
    public String key;
    public String queryId;
    public IncrementFormat formatter;
    public IncrementalSubscription sub;

    public RemotePushSession (String k, String queryId, IncrementFormat f,
        String requester, IncrementalSubscription sub)
    {
      this.requester = requester;
      this.key = k;
      this.queryId = queryId;
      this.formatter = f;
      this.sub = sub;
    }

    public void subscriptionChanged () {
      updatePushSession(this);
    }

    public IncrementalSubscription getSubscription() {
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

    public Collection getAddedCollection () {
      return rbs.getAddedCollection();
    }
    public Collection getChangedCollection () {
      return rbs.getChangedCollection();
    }
    public Collection getRemovedCollection () {
      return rbs.getRemovedCollection();
    }
    public Collection getMembership () {
      return rbs.getMembership();
    }
  }


  private void cancelSession(Element root, MessageAddress originator) throws Exception
  {
    String query_id = root.getAttribute("query_id");
    Iterator iter = queryMap.values().iterator();
    RemotePushSession to_be_deleted = null;
    while (iter.hasNext()) {
      RemotePushSession rps = (RemotePushSession)iter.next();
      if (rps.queryId.equals(query_id)) {
        to_be_deleted = rps;
        break;
      }
    }
    if (to_be_deleted != null) {
      getBlackboardService().unsubscribe(to_be_deleted.sub);
      queryMap.remove(to_be_deleted.sub);
    } else {
      System.out.println("Error cancelling session "+query_id+" at "+getBindingSite().getAgentIdentifier().getAddress());
    }
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
      updatePushSession(rps);
      rps.rbs.close();
    }
  }
}