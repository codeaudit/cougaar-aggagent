package org.cougaar.lib.aggagent.client;

import org.w3c.dom.*;
import org.cougaar.lib.aggagent.query.AggregationResultSet;

  /**
   * Provides support for event driven monitoring of active
   * AggregationResultSet(s) on the aggregation agent.
   *
   * Maintains a collection of monitored result sets and keeps them updated
   * based on changes on the aggregation agent's blackboard.  To react to these
   * changes either:
   * <UL>
   * <LI>add update listener(s) to this monitor class and receive events for
   *     changes to all monitored result sets or</LI>
   * <LI>add update listener(s) to 'live' result sets provided by monitor and
   *     receive events only for those objects</LI>
   * </UL>
   */
  public class ResultSetMonitor extends Monitor
  {
    /**
     * Create a new monitor to monitor a set of objects on the aggregation
     * agent.  Each monitor is used to monitor a single type of object
     * (e.g. AlertMonitor, ResultSetMonitor).
     *
     * @param serverURL    aggregation agent cluster's text URL
     * @param updateMethod method used to keep monitored objects updated
     *                     PULL_METHOD - periodically pull incremental updates
     *                                   from passive session on aggregation
     *                                   agent.  Create new connection with
     *                                   each pull.
     *                      KEEP_ALIVE_METHOD - create keep alive session
     *                                   with aggregation agent.  Incremental
     *                                   updates are pushed to the client
     *                                   over this pipe.
     */
    public ResultSetMonitor(String serverURL, int updateMethod)
    {
      super(serverURL, AggregationResultSet.RESULT_SET_TAG, updateMethod);
    }

    /**
     * Monitor a result set managed by the aggregation agent.  Returns a 'live'
     * result set for a given persistent query. Update listeners can be added
     * to this live object to react to changes to that object.  If monitor is
     * not set to monitor-all-objects, this result set is added to this
     * monitor's set of monitored objects.
     *
     * @param queryId id of query result adapter on aggregation agent that is
     *                maintaining this result set.
     *
     * @return a live result set that is actively being updated to match a
     *         subject object on the aggregation agent.
     */
    public AggregationResultSet monitorResultSet(String queryId)
    {
      AggregationResultSet resultSet = new AggregationResultSet();
      AggregationResultSet monitoredResultSet =
        (AggregationResultSet)monitorObject(queryId, resultSet);
      return monitoredResultSet;
    }

    /**
     * Remove this result set from the set of result sets being monitored.
     * This method has a negligible effect if monitor-all is turned on
     * (old live result set object will die, but new one will take it's place
     *  if that object is still on the log plan).
     *
     * @param queryId id of query result adapter on aggregation agent that is
     *                maintaining this result set.
     *
     * @return previously live result set that was removed.
     */
    public AggregationResultSet stopMonitoringResultSet(String queryId)
    {
      return (AggregationResultSet)stopMonitoringObject(queryId);
    }

    /**
     * Provides a xml representation of a given query identifier.
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     *
     * @return a xml representation of given query identifier.
     */
    protected String createIdTag(Object identifier)
    {
      return "<query id=\"" + identifier + "\"/>\n";
    }

    /**
     * Called when a update event (either add or change) is reported by the
     * aggregation agent to a result set described by the given xml element
     * tree.
     *
     * @param monitoredElement xml element tree that describes the updated
     *                         result set.
     *
     * @return a live result set object updated based on the given xml
     */
    protected Object update(Element monitoredElement)
    {
      String queryId =
        monitoredElement.getAttribute(AggregationResultSet.QUERY_ID_ATT);
      AggregationResultSet newRs = new AggregationResultSet(monitoredElement);
      AggregationResultSet monitoredRs =
        (AggregationResultSet)monitorObject(queryId, newRs);
      monitoredRs.update(newRs);
      return monitoredRs;
    }

    /**
     * Called when a remove event is reported by the aggregation agent to a
     * result set described by the given xml element tree.
     *
     * @param monitoredElement xml element tree that describes the removed
     *                         result set.
     *
     * @return previously live result set that was removed.
     */
    protected Object remove(Element monitoredElement)
    {
      String queryId =
        monitoredElement.getAttribute(AggregationResultSet.QUERY_ID_ATT);
      AggregationResultSet removedRS = stopMonitoringResultSet(queryId);
      removedRS.fireObjectRemoved();
      return removedRS;
    }
  }

