package org.cougaar.lib.aggagent.test;

import java.util.*;

import org.cougaar.core.plugin.SimplePlugIn;

public class EffortWaster extends SimplePlugIn {
  // Parameters of performance for the EffortWaster PlugIn.  The variables are
  // initialized with their default values.
  private int maxCycles = 5;
  private long pauseInterval = 5000;
  private long initialWait = 10000;

  private Vector cycles = new Vector();
  private int nextCycle = 1;

  public void setMaxCycles (String v) {
    try {
      maxCycles = Integer.parseInt(v);
    }
    catch (NumberFormatException nfe) {
      System.out.println(
        "EffortWaster::setMaxCycles:  bad number format \"" + v + "\"");
    }
  }

  public void setPauseInterval (String v) {
    try {
      pauseInterval = Integer.parseInt(v);
    }
    catch (NumberFormatException nfe) {
      System.out.println(
        "EffortWaster::setPauseInterval:  bad number format \"" + v + "\"");
    }
  }

  public void setInitialWait (String v) {
    try {
      initialWait = Long.parseLong(v);
    }
    catch (NumberFormatException nfe) {
      System.out.println(
        "EffortWaster::setInitialWait:  bad number format \"" + v + "\"");
    }
  }

  public void setupSubscriptions () {
    Pair p = new Pair();
    for (Iterator i = getParameters().iterator(); i.hasNext(); ) {
      p.load(i.next());
      if ("maxCycles".equals(p.name))
        setMaxCycles(p.value);
      else if ("pauseInterval".equals(p.name))
        setPauseInterval(p.value);
      else
        System.out.println(
          "EffortWaster:  unrecognized parameter \"" + p.name + "\"");
    }

    wakeAfter(initialWait);
  }

  private static class Pair {
    public String name = null;
    public String value = null;

    public void load (Object o) {
      name = null;
      value = null;
      if (o != null) {
        String s = o.toString();
        int i = s.indexOf('=');
        if (i == -1) {
          name = s;
        }
        else {
          name = s.substring(0, i);
          value = s.substring(i + 1);
        }
      }
    }
  }

  public void execute () {
    Vector additions = new Vector();
    if (cycles.size() < maxCycles) {
      NumberCycle nc = new NumberCycle(nextCycle++);
      additions.add(nc);
      publishAdd(nc);
    }

    Vector removals = new Vector();
    for (Iterator i = cycles.iterator(); i.hasNext(); ) {
      NumberCycle nc = (NumberCycle) i.next();
      if (nc.increment()) {
        publishChange(nc);
      }
      else {
        publishRemove(nc);
        removals.add(nc);
      }
    }
    cycles.removeAll(removals);
    cycles.addAll(additions);

    wakeAfter(pauseInterval);
  }
}
