package org.cougaar.lib.aggagent.ldm;


import java.util.Collection;
import java.util.Enumeration;

import org.cougaar.core.cluster.PublishHistory;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.cluster.ALPPlanServesLogicProvider;
import org.cougaar.core.cluster.XPlanServesALPPlan;
import org.cougaar.core.cluster.ALPPlan;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.Directive;




public class Domain  implements org.cougaar.domain.planning.ldm.Domain
{

 /**
   * construct an LDM factory to serve the specified LDM instance.
   **/
  public Factory getFactory(LDMServesPlugIn ldm){
      return new AggClusterFactory();
  }

  /** initialize Domain. Called once on a new instance immediately
   * after creating the Domain instance via the zero-argument constructor.
   **/
  public void initialize(){
  }

  /**
   * Create new Domain-specific LogicProviders for loading into the LogPlan.
   * @return a Collection of the LogicProvider instances or null.
   **/
  public Collection createLogicProviders(ALPPlanServesLogicProvider logplan,
                                  ClusterServesLogicProvider cluster) {
       
       return null;
   }

  public XPlanServesALPPlan createXPlan(java.util.Collection existingXPlans) {
       return new XPlanServesALPPlan () {
          public void add(Object p0){}
          public void change(Object p0, Collection p1){}
          public void remove(Object p0){}
          public Enumeration searchALPPlan(UnaryPredicate p0){return null;}
          public void sendDirective(Directive p0){}
          public void sendDirective(Directive p0, Collection p1){}
          public void setupSubscriptions(ALPPlan alpPlan){}
          public PublishHistory getHistory() { return null; }
       };
  }

}