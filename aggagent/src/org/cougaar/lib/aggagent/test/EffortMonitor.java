package org.cougaar.lib.aggagent.test;

import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.util.UnaryPredicate;

public class EffortMonitor extends SimplePlugin {

  private class Seeker implements UnaryPredicate
  {
    public boolean execute(Object obj)
    {
      return true;
    }
  }

  private IncrementalSubscription numbers = null;

  public void setupSubscriptions () {
    numbers = (IncrementalSubscription) subscribe(new Seeker());
  }

  public void execute () {
    System.out.println("EffortMonitor::execute");
    if (!numbers.hasChanged()) {
      return;
    }

    Enumeration e = numbers.getAddedList();
    if (e.hasMoreElements()) {
      System.out.println("  Added:");
      while (e.hasMoreElements())
        System.out.println("    " + e.nextElement());
    }

    e = numbers.getChangedList();
    if (e.hasMoreElements()) {
      System.out.println("  Changed (to):");
      while (e.hasMoreElements())
        System.out.println("    " + e.nextElement());
    }

    e = numbers.getRemovedList();
    if (e.hasMoreElements()) {
      System.out.println("  Removed:");
      while (e.hasMoreElements())
        System.out.println("    " + e.nextElement());
    }
  }
}
