/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

package org.cougaar.lib.aggagent.test;

import java.util.*;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.*;
import org.cougaar.core.blackboard.*;

/**
 *  This is an example demonstrating the use of the Relay mechanism.
 *  It complements the ReceiverPlugin by sending messages to the address under
 *  which the ReceiverPlugin registers itself.
 *  <br><br>
 *  Incidentally, this class also demonstrates the usage of the AlarmService
 *  interface, which is registered in superclass ComponentPlugin.  Note the use
 *  of an Alarm implementation.
 */
public class SenderPlugin extends ComponentPlugin {

  /** Holds value of property UIDService. */
  private org.cougaar.core.service.UIDService UIDService;
  MessageAddress source;
  MessageAddress target;
  
  private class SendAfterDelay implements Alarm {
    private long detonate = -1;
    private boolean expired = false;

    public SendAfterDelay () {
      detonate = 3000l + System.currentTimeMillis();
    }

    public long getExpirationTime () {
      return detonate;
    }

    public void expire () {
      if (!expired)
        sendMessage();
      expired = true;
    }

    public boolean hasExpired () {
      return expired;
    }

    public boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }
  }


  private void delayedSend () {
    alarmService.addRealTimeAlarm(new SendAfterDelay());
  }

  
  private void sendMessage () {
    System.out.println("SenderPlugin::sendMessage ...");
    
    TestRelay tr = new TestRelay( getUIDService().nextUID(), source, target, "Foo", null);
    getBlackboardService().openTransaction();
    getBlackboardService().publishAdd(tr);
    getBlackboardService().closeTransaction();

    System.out.println("SenderPlugin::sendMessage:  done");
    delayedSend();
  }

  IncrementalSubscription sub;
  public void setupSubscriptions () {
    source = getAgentIdentifier();
    Iterator iter = this.getParameters().iterator();
    while (iter.hasNext()) {
        MessageAddress addr = MessageAddress.getMessageAddress((String)iter.next());
        target = addr;
        System.out.println("Sending to : "+addr);
    }
    sub = (IncrementalSubscription)getBlackboardService().subscribe(new GetTestRelayPredicate());
    delayedSend();
  }
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected void execute() {
      System.out.println("SenderPlugin: execute");
      Iterator iter = sub.getAddedCollection().iterator();
      while (iter.hasNext()) {
          System.out.println(" --- Added: "+iter.next());
      }
      iter = sub.getChangedCollection().iterator();
      while (iter.hasNext()) {
          TestRelay tr = (TestRelay)iter.next();
          System.out.println(" --- Changed: "+tr);
          if (tr.getContent().equals("Foo")) {
              tr.updateContent("Heard you", null);
              getBlackboardService().publishChange(tr);
          }
      }
          
  }
  
  /** Getter for property UIDService.
   * @return Value of property UIDService.
   */
  public org.cougaar.core.service.UIDService getUIDService() {
      return this.UIDService;
  }
  
  /** Setter for property UIDService.
   * @param UIDService New value of property UIDService.
   */
  public void setUIDService(org.cougaar.core.service.UIDService UIDService) {
      this.UIDService = UIDService;
  }
  
}
