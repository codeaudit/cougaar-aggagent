/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.lib.aggagent.psp;

import java.util.Iterator;
import java.util.Enumeration;
import java.util.ArrayList;

import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.IncrementalSubscription;


public class QuerySessionUISubscriber implements UISubscriber
{

  private ArrayList myIncomingItems = new ArrayList();
  private String myQuerySessionID=null;

  public QuerySessionUISubscriber( String qsession ){
       myQuerySessionID = qsession;
  }

  //
  // Call-back by which the PlanServerPlugin
  // updates this QuerySession
  //
  public void subscriptionChanged(Subscription subscription) {
      System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID + " {.subscriptionChanged()} called");
      Enumeration e = ((IncrementalSubscription)subscription).getAddedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addItem(obj);
      }
  }
  private void addItem(Object obj) {
      synchronized( myIncomingItems ) {
            myIncomingItems.add(obj);
      }
  }

  public String getQuerySessionID(){
      return myQuerySessionID;
  }

  //
  // @return Copy of internal cache of updates (added objects) from PlanServerPlugin
  //    Updates are with respect to last time this method was called.
  //
  public ArrayList grabUpdates(){
     System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID
                        + " {.grabUpdates()} called, size=" + myIncomingItems.size());
      ArrayList old =null;
      synchronized( myIncomingItems ){
          old= myIncomingItems;
          myIncomingItems = new ArrayList();  // new snap shot buffer
      }
      return old;
  }

}
