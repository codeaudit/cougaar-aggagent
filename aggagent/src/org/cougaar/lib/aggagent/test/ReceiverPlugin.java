
package org.cougaar.lib.aggagent.test;

import java.util.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.*;

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
