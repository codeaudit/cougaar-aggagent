package org.cougaar.lib.aggagent.session;

import org.cougaar.lib.planserver.*;
import org.cougaar.util.*;
import org.cougaar.core.cluster.*;

public class RemotePSPSubscription extends RemoteBlackboardSubscription {

  private ServerPlugInSupport spis;
  private UnaryPredicate p;
  private UISubscriber subscriber;

  public RemotePSPSubscription(ServerPlugInSupport spis, UnaryPredicate p, UISubscriber subscriber) {
    super(spis.getDirectDelegate().getBlackboardService(), p);
    this.spis = spis;
    this.p = p;
    this.subscriber = subscriber;

    spis.unsubscribeForSubscriber(subs);
    subs = (IncrementalSubscription)spis.subscribe(subscriber, p);

  }
}