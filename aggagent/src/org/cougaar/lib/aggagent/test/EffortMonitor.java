/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.planning.plugin.legacy.SimplePlugin;
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
