
package org.cougaar.lib.aggagent.plugin;

import java.util.*;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.*;

/**
 *  This PlugIn serves a dual purpose.  It notices when queries have changed
 *  and updates the associated Alert instances, and also notices when an Alert
 *  has changed its state so that notices may be sent.
 *  <br><br>
 *  Currently, notices are not implemented, so none are actually sent.
 */
public class AlertPlugIn extends SimplePlugIn {
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
    resultSets = (IncrementalSubscription) subscribe(resultSetSeeker);
    alerts = (IncrementalSubscription) subscribe(alertSeeker);
  }

  public void execute () {
    if (resultSets.hasChanged()) {
      // update alerts associated with changed result set(s)
      for (Enumeration e = resultSets.getChangedList(); e.hasMoreElements(); ) {
        updateAlerts(((QueryResultAdapter) e.nextElement()).getAlerts());
      }
    }
    if (alerts.hasChanged()) {
      // set initial state of new alert(s)
      updateAlerts(alerts.getAddedCollection().iterator());
      System.out.println("AlertPlugIn::execute:  Alerts have changed");
    }
  }

  private void updateAlerts(Iterator alerts)
  {
    while (alerts.hasNext())
    {
      Alert a = (Alert) alerts.next();
      if (a.update())
        publishChange(a);
    }
  }
}