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

package org.cougaar.lib.aggagent.plugin;

import java.util.Enumeration;
import java.util.Iterator;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.util.UnaryPredicate;

/**
 *  This Plugin serves a dual purpose.  It notices when queries have changed
 *  and updates the associated Alert instances, and also notices when an Alert
 *  has changed its state so that notices may be sent.
 *  <br><br>
 *  Currently, notices are not implemented, so none are actually sent.
 */
public class AlertPlugin extends ComponentPlugin {
  private static class ClassInstanceSeeker implements UnaryPredicate {
    private Class type = null;

    public ClassInstanceSeeker (Class c) {
      type = c;
    }

    public boolean execute (Object o) {
      return type.isInstance(o);
    }
  }
  private static UnaryPredicate resultSetSeeker =
    new ClassInstanceSeeker(QueryResultAdapter.class);
  private static UnaryPredicate alertSeeker =
    new ClassInstanceSeeker(Alert.class);

  private IncrementalSubscription resultSets = null;
  private IncrementalSubscription alerts = null;

  public void setupSubscriptions () {
    BlackboardService blackboard = getBlackboardService();
    resultSets =(IncrementalSubscription)blackboard.subscribe(resultSetSeeker);
    alerts = (IncrementalSubscription) blackboard.subscribe(alertSeeker);
  }

  public void execute () {
    if (resultSets.hasChanged()) {
      // update alerts associated with changed result set(s)
      for (Enumeration e = resultSets.getChangedList(); e.hasMoreElements(); ){
        updateAlerts(((QueryResultAdapter) e.nextElement()).getAlerts());
      }
    }
    if (alerts.hasChanged()) {
      // set initial state of new alert(s)
      updateAlerts(alerts.getAddedCollection().iterator());
      System.out.println("AlertPlugin::execute:  Alerts have changed");
    }
  }

  private void updateAlerts(Iterator alerts)
  {
    while (alerts.hasNext())
    {
      Alert a = (Alert) alerts.next();
      if (a.update())
        getBlackboardService().publishChange(a);
    }
  }
}
