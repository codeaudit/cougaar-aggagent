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
package org.cougaar.lib.aggagent.test;

import java.util.*;

import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.agent.service.alarm.*;

public class EffortWaster extends ComponentPlugin {
  // Parameters of performance for the EffortWaster Plugin.  The variables are
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
  if (!wasAwakened()) {
    return;
  }

    Vector additions = new Vector();
    if (cycles.size() < maxCycles) {
      NumberCycle nc = new NumberCycle(nextCycle++);
      additions.add(nc);
      getBlackboardService().publishAdd(nc);
    }

    Vector removals = new Vector();
    for (Iterator i = cycles.iterator(); i.hasNext(); ) {
      NumberCycle nc = (NumberCycle) i.next();
      if (nc.increment()) {
        getBlackboardService().publishChange(nc);
      }
      else {
        getBlackboardService().publishRemove(nc);
        removals.add(nc);
      }
    }
    cycles.removeAll(removals);
    cycles.addAll(additions);
    wakeAfter(pauseInterval);
  }

  private void wakeAfter(long pauseInterval) {
    long now = System.currentTimeMillis();
    long wakeAt = now + pauseInterval;
    Alarm alarm = new PluginAlarm(now + pauseInterval);
    getAlarmService().addRealTimeAlarm(alarm);
  }

  public class PluginAlarm implements Alarm {
    private long expiresAt;
    private boolean expired = false;
    public PluginAlarm (long expirationTime) {
      expiresAt = expirationTime;
    }
    public long getExpirationTime() { return expiresAt; }
    public synchronized void expire() {
      if (!expired) {
        expired = true;
        getBlackboardService().signalClientActivity();
      }
    }
    public boolean hasExpired() { return expired; }
    public synchronized boolean cancel() {
      boolean was = expired;
      expired=true;
      return was;
    }
    public String toString() {
      return "<PluginAlarm "+expiresAt+
        (expired?"(Expired) ":" ")+
        "for "+EffortWaster.this.toString()+">";
    }
  }

}
