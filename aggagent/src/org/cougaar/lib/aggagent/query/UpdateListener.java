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

/**
 * Used to listen for incremental update events on observable query objects
 * (this currently includes AggregationResultSet and AlertDescriptor).
 */
public interface UpdateListener
{
  /**
   * Object was added to the aggregation agent's log plan
   *
   * @param sourceObject object that was added
   */
  public void objectAdded(Object sourceObject);

  /**
   * Object was changed on the aggregation agent's log plan.  The object will
   * reflect the change.
   *
   * @param sourceObject object that was changed
   */
  public void objectChanged(Object sourceObject);

  /**
   * Object was removed from the aggregation agent's log plan.
   *
   * @param sourceObject object that was removed
   */
  public void objectRemoved(Object sourceObject);
}
