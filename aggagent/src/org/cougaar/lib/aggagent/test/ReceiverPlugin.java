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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;

/**
 *  This is an example demonstrating the use of the Relay mechanism.
 *  It registers itself to receive messages under the name "Target-Receiver"
 *  and reports any Messages as they arrive.
 */
public class ReceiverPlugin extends ComponentPlugin {

  IncrementalSubscription sub;
  public void setupSubscriptions () {
    sub = (IncrementalSubscription)getBlackboardService().subscribe(new GetTestRelayPredicate());
  }
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected void execute() {
      System.out.println("ReceiverPlugin: execute");
      Iterator iter = sub.getAddedCollection().iterator();
      while (iter.hasNext()) {
          TestRelay tr = (TestRelay)iter.next();
          System.out.println(" --- Added: "+tr);
          tr.updateResponse(null, "Hello, there:"+tr.getContent());
          getBlackboardService().publishChange(tr);
      }
      iter = sub.getChangedCollection().iterator();
      while (iter.hasNext()) {
          System.out.println(" --- Changed: "+iter.next());
      }
          
  }
}
