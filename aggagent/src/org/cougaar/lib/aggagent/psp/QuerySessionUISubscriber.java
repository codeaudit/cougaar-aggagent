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
 
package org.cougaar.lib.aggagent.psp;

import java.util.Iterator;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;

import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.IncrementalSubscription;


public class QuerySessionUISubscriber implements UISubscriber
{

  private ArrayList myIncomingAddItems = new ArrayList();
  private ArrayList myIncomingChangeItems = new ArrayList();
  private ArrayList myIncomingRemoveItems = new ArrayList();

  private String myQuerySessionID=null;

  public QuerySessionUISubscriber( String qsession ){
       myQuerySessionID = qsession;
  }

  //
  // Call-back by which the PlanServerPlugin
  // updates this QuerySession
  //
  public void subscriptionChanged(Subscription subscription) {
      // System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID
      //                     + " {.subscriptionChanged()} called");
      IncrementalSubscription isubscribe = (IncrementalSubscription)subscription;

      // ADD --
      Enumeration e = isubscribe.getAddedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         addItem(obj);
      }
      // CHANGE --
      e = isubscribe.getChangedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         changeItem(obj);
      }
      // REMOVE --
      e = isubscribe.getRemovedList();
      while (e.hasMoreElements()) {
         Object obj = e.nextElement();
         removeItem(obj);
      }
  }
  private void addItem(Object obj) {
      synchronized( myIncomingAddItems ) {
            myIncomingAddItems.add(obj);
      }
  }
  private void removeItem(Object obj) {
      synchronized( myIncomingRemoveItems ) {
            myIncomingRemoveItems.add(obj);
      }
  }
  private void changeItem(Object obj) {
      synchronized( myIncomingChangeItems ) {
            myIncomingChangeItems.add(obj);
      }
  }
  public String getQuerySessionID(){
      return myQuerySessionID;
  }

  //
  // @return Copy of internal cache of updates (added objects) from PlanServerPlugin
  //    Updates are with respect to last time this method was called.
  //
  public ArrayList grabAddUpdates(List updates){
     // System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID
     //                    + " {.grabAddUpdates()} called, size=" + myIncomingAddItems.size());
      ArrayList old =null;
      synchronized( myIncomingAddItems ){
          old= myIncomingAddItems;
          myIncomingAddItems = new ArrayList();  // new snap shot buffer
          updates.addAll(old);
      }
      /// Thread.dumpStack();
      return old;
  }
  //
  // @return Copy of internal cache of updates (added objects) from PlanServerPlugin
  //    Updates are with respect to last time this method was called.
  //
  public ArrayList grabChangeUpdates(List updates){
     // System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID
     //                    + " {.grabChangeUpdates()} called, size=" + myIncomingChangeItems.size());
      ArrayList old =null;
      synchronized( myIncomingChangeItems ){
          old= myIncomingChangeItems;
          myIncomingChangeItems = new ArrayList();  // new snap shot buffer
          updates.addAll(old);
      }
      return old;
  }
  //
  // @return Copy of internal cache of updates (added objects) from PlanServerPlugin
  //    Updates are with respect to last time this method was called.
  //
  public ArrayList grabRemoveUpdates(List updates){
     // System.out.println("[QuerySessionUISubscriber] query_session=" + myQuerySessionID
     //                    + " {.grabRemoveUpdates()} called, size=" + myIncomingRemoveItems.size());
      ArrayList old =null;
      synchronized( myIncomingRemoveItems ){
          old= myIncomingRemoveItems;
          myIncomingRemoveItems = new ArrayList();  // new snap shot buffer
          updates.addAll(old);
      }
      ///Thread.dumpStack();
      return old;
  }
}
