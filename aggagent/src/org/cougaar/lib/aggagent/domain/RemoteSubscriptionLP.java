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
package org.cougaar.lib.aggagent.domain;

import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.*;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;
import org.cougaar.core.util.*;
import java.util.*;

/**
 * This LogicProvider does the communications for the aggregation agent function.
 */
public class RemoteSubscriptionLP extends LogPlanLogicProvider implements MessageLogicProvider, EnvelopeLogicProvider {

  private boolean debug = false;
  private MessageAddress me;

  public RemoteSubscriptionLP(LogPlanServesLogicProvider logplan,
                ClusterServesLogicProvider cluster) {

    super (logplan, cluster);
    me = cluster.getClusterIdentifier();
    if (debug) System.out.println("RemoteSubLP("+cluster.getClusterIdentifier()+"): Cntr");
  }

  /**
   * A message got received...
   * Extract its XMLmessage and publish it.
   */
  public void execute(Directive m, Collection changeReports) {
    if (debug) System.out.println("RemoteSubLP("+cluster.getClusterIdentifier()+")  : got a directive: "+m+" FROM:"+m.getSource());
    if (m instanceof AggDirective) {
      if (debug) System.out.println("RemoteSubLP("+cluster.getClusterIdentifier()+"): directive is an AggDirective");
      AggDirective ad = (AggDirective)m;
      logplan.add(ad.getMessage());
      logplan.remove(m);
    }
  }


  /**
   * Something got published....
   * If it's an XMLMessage, turn it into a directive and send it
   */
  public void execute(EnvelopeTuple m, Collection changeReports) {
    if (m.getObject() instanceof XMLMessage) {
      if (m.isAdd()) {
        XMLMessage xmsg = (XMLMessage)m.getObject();
        if (!xmsg.getDestination().equals(me)) { // Only if it's not local...
          AggDirective ad = new AggDirective((XMLMessage)m.getObject());
          logplan.sendDirective(ad);
          if (debug) System.out.println("RemoteSubLP("+cluster.getClusterIdentifier()+"): Sent message to: "+ad.getDestination());
          if (debug) System.out.println("RemoteSubLP("+cluster.getClusterIdentifier()+"): Removing XMLMessage");
          logplan.remove(m.getObject());
        }
      }
    }
  }
}