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


import java.util.*;

import org.cougaar.core.blackboard.BlackboardServesLogicProvider;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.blackboard.XPlanServesBlackboard;
import org.cougaar.core.domain.Domain;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.util.log.*;

/**
 * This COUGAAR Domain package definition supports the aggregation agent functionality.
 **/
public class AggDomain implements Domain {
  private boolean debug;
  public AggDomain() {
    if (debug) System.out.println("Construct Aggregation domain");
  }

  public void initialize() {
    // register COUGAAR Verbs, etc... maybe just put 'em in the factory or somesuch
    if (debug) System.out.println("Initilized Aggregation domain");
  }

  public Factory getFactory(LDMServesPlugin ldm) {
    if (debug) System.out.println("Aggregation domain:: get factory");
    return new AggFactory(ldm);
  }

  public XPlanServesBlackboard createXPlan(Collection existingXPlans) {
    if (debug) System.out.println("Aggregation domain:: createXPlan");
    for (Iterator plans = existingXPlans.iterator(); plans.hasNext(); ) {
      XPlanServesBlackboard xPlan = (XPlanServesBlackboard) plans.next();
      if (xPlan instanceof LogPlan) return xPlan;
    }
    return new LogPlan();
  }

  public Collection createLogicProviders(BlackboardServesLogicProvider alpplan,
                                         ClusterServesLogicProvider cluster) {
    if (debug) System.out.println("Aggregation domain:: createLogicProviders");

    ArrayList l = new ArrayList(1); // don't let this be too small.
    LogPlanServesLogicProvider logplan = (LogPlanServesLogicProvider) alpplan;

    l.add(new RemoteSubscriptionLP(logplan, cluster));
    return l;
  }

  public Collection getAliases() {
    if (debug) System.out.println("Aggregation domain:: get aliases");
    ArrayList l = new ArrayList(2);
    l.add("agg");
    l.add("aggregation");
    return l;
  }
}