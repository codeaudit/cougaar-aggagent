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
package org.cougaar.lib.aggagent.query;

import java.io.Serializable;
import java.util.Vector;

/**
 * Used by objects to provide an implementation of an update observable.
 * Assists in managment of update listeners.  Objects can either extend or
 * delegate to this class.
 */
public class UpdateObservable implements Serializable {
  private Vector updateListeners = null;

  /**
   * Default constructor
   */
  public UpdateObservable()
  {
    updateListeners = new Vector();
  }

  /**
   * Add an update listener to observe this object
   */
  public void addUpdateListener(UpdateListener ul)
  {
    updateListeners.add(ul);
  }

  /**
   * Remove an update listener such that it no longer gets notified of changes
   * to this object
   */
  public void removeUpdateListener(UpdateListener ul)
  {
    updateListeners.remove(ul);
  }

  /**
   * Send event to all update listeners indicating that object has been added
   * to the log plan.
   *
   * @param sourceObject object that has been added
   */
  public void fireObjectAdded(Object sourceObject)
  {
    for (int i = 0;  i < updateListeners.size(); i++)
    {
      UpdateListener updateListener =
        (UpdateListener)updateListeners.elementAt(i);
      updateListener.objectAdded(sourceObject);
    }
  }

  /**
   * Send event to all update listeners indicating that object has been changed
   * on the log plan.
   *
   * @param sourceObject object that has been changed
   */
  public void fireObjectChanged(Object sourceObject)
  {
    for (int i = 0;  i < updateListeners.size(); i++)
    {
      UpdateListener updateListener =
        (UpdateListener)updateListeners.elementAt(i);
      updateListener.objectChanged(sourceObject);
    }
  }

  /**
   * Send event to all update listeners indicating that object has been removed
   * from the log plan.
   *
   * @param sourceObject object that has been removed
   */
  public void fireObjectRemoved(Object sourceObject)
  {
    for (int i = 0;  i < updateListeners.size(); i++)
    {
      UpdateListener updateListener =
        (UpdateListener)updateListeners.elementAt(i);
      updateListener.objectRemoved(sourceObject);
    }
  }
}
