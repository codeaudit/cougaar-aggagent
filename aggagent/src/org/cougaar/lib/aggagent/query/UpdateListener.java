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
