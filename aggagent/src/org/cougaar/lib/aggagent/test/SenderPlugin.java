/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.lib.aggagent.test;

import java.util.Iterator;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;

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
