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

import org.cougaar.core.agent.ClusterServesLogicProvider;

import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.XPlanServesBlackboard;

import org.cougaar.core.component.BindingSite;

import org.cougaar.core.domain.DomainAdapter;
import org.cougaar.core.domain.DomainBindingSite;

import org.cougaar.core.service.LoggingService;

import org.cougaar.util.log.*;

/**
 * This COUGAAR Domain package definition supports the aggregation agent functionality.
 **/
public class AggDomain extends DomainAdapter {
  private static final String AGGAGENT_NAME = "aggagent".intern();
  private boolean debug;

  public AggDomain() {
    if (debug) System.out.println("Construct Aggregation domain");
  }

  public String getDomainName() {
    return AGGAGENT_NAME;
  }

  public void initialize() {
    super.initialize();
    // register COUGAAR Verbs, etc... maybe just put 'em in the factory or somesuch
    if (debug) System.out.println("Initilized Aggregation domain");
  }

  public Collection getAliases() {
    if (debug) System.out.println("Aggregation domain:: get aliases");
    ArrayList l = new ArrayList(2);
    l.add("agg");
    l.add("aggregation");
    return l;
  }

  protected void loadFactory() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                                 "Unable to initialize domain Factory without a binding site.");
    } 

    getLoggingService().debug("Aggregation domain:: loadfactory");

    setFactory(new AggFactory(bindingSite.getClusterServesLogicProvider().getLDM()));
  }

  protected void loadXPlan() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                                 "Unable to initialize domain XPlan without a binding site.");
    } 

    getLoggingService().debug("Aggregation domain:: loadXPlan");

    Collection xPlans = bindingSite.getXPlans();
    LogPlan logPlan = null;
    
    for (Iterator iterator = xPlans.iterator(); iterator.hasNext();) {
      XPlanServesBlackboard  xPlan = (XPlanServesBlackboard) iterator.next();
      if (xPlan instanceof LogPlan) {
        // Note that this means there are 2 paths to the plan.
        // Is this okay?
        logPlan = (LogPlan) logPlan;
        break;
      }
    }
    
    if (logPlan == null) {
      logPlan = new LogPlan();
    }
    
    setXPlan(logPlan);
  }

  protected void loadLPs() {
    DomainBindingSite bindingSite = (DomainBindingSite) getBindingSite();

    if (bindingSite == null) {
      throw new RuntimeException("Binding site for the domain has not be set.\n" +
                                 "Unable to initialize domain LPs without a binding site.");
    } 

    getLoggingService().debug("Aggregation domain:: loadLPs");
    ClusterServesLogicProvider cluster = bindingSite.getClusterServesLogicProvider();
    LogPlan logPlan = (LogPlan) getXPlan();

    addLogicProvider(new RemoteSubscriptionLP(logPlan, cluster));
  }

}
