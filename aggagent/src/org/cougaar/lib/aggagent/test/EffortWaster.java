/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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
import java.util.Vector;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.plugin.ComponentPlugin;

public class EffortWaster extends ComponentPlugin {
  // Parameters of performance for the EffortWaster Plugin.  The variables are
  // initialized with their default values.
  private int maxCycles = 5;
  private long pauseInterval = 5000;
  private long initialWait = 10000;

  private Vector cycles = new Vector();
  private int nextCycle = 1;
  private int initialValue = 1;

  public void setMaxCycles (String v) {
    try {
      maxCycles = Integer.parseInt(v);
    }
    catch (NumberFormatException nfe) {
      System.out.println(
        "EffortWaster::setMaxCycles:  bad number format \"" + v + "\"");
    }
  }

  public void setInitialValue (String v) {
    try {
      initialValue = Integer.parseInt(v);
    }
    catch (NumberFormatException nfe) {
      System.out.println(
        "EffortWaster::setInitialValue:  bad number format \"" + v + "\"");
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
    if (getParameters() != null) {
        for (Iterator i = getParameters().iterator(); i.hasNext(); ) {
            p.load(i.next());
            if ("maxCycles".equals(p.name))
                setMaxCycles(p.value);
            else if ("pauseInterval".equals(p.name))
                setPauseInterval(p.value);
            else if ("initialValue".equals(p.name))
                setInitialValue(p.value);
            else
                System.out.println(
                "EffortWaster:  unrecognized parameter \"" + p.name + "\"");
        }
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
      NumberCycle nc = new NumberCycle(nextCycle++, initialValue);
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
    Alarm alarm = new PluginAlarm(wakeAt);
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
        {
          org.cougaar.core.service.BlackboardService bbs = getBlackboardService();
          if (bbs != null) bbs.signalClientActivity();
        }
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
