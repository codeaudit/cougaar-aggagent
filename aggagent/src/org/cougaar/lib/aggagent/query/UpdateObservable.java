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
