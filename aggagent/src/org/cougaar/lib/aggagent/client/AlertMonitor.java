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
package org.cougaar.lib.aggagent.client;

import org.cougaar.lib.aggagent.query.AlertDescriptor;
import org.w3c.dom.Element;

  /**
   * Provides support for event driven monitoring of active Alert(s) on the
   * aggregation agent.
   *
   * Maintains a collection of monitored alerts and keeps them updated
   * based on changes on the aggregation agent's blackboard.  To react to these
   * changes either:
   * <UL>
   * <LI>add update listener(s) to this monitor class and receive events for
   *     changes to all monitored alerts or</LI>
   * <LI>add update listener(s) to 'live' alerts provided by this monitor and
   *     receive events only for those objects</LI>
   * </UL>
   */
  public class AlertMonitor extends Monitor
  {
    public AlertMonitor(String serverURL, int updateMethod)
    {
      super(serverURL, "alert", updateMethod);
    }

    /**
     * Monitor an alert managed by the aggregation agent.  Returns a 'live'
     * alert descriptor for a given persistent query. Update listeners can be
     * added to this live object to react to changes to that object.  If
     * monitor is not set to monitor-all-objects, this alert is added to
     * this monitor's set of monitored objects.
     *
     * @param queryId   id of query result adapter on aggregation agent that is
     *                  maintaining this alert.
     * @param alertName name of alert to monitor
     *
     * @return a live alert descriptor that is actively being updated to match
     *         a subject alert on the aggregation agent.
     */
    public AlertDescriptor monitorAlert(String queryId, String alertName)
    {
      AlertDescriptor ad = new AlertDescriptor();
      ad.setQueryId(queryId);
      ad.setName(alertName);
      AlertIdentifier ai = new AlertIdentifier(queryId, alertName);
      AlertDescriptor monitoredAlert =
        (AlertDescriptor)monitorObject(ai, ad);
      return monitoredAlert;
    }

    /**
     * Remove this alert from the set of alerts being monitored.
     * This method has a negligible effect if monitor-all is turned on
     * (old live alert object will die, but new one will take it's place
     *  if that alert is still on the log plan).
     *
     * @param queryId   id of query result adapter on aggregation agent that is
     *                  maintaining this alert.
     * @param alertName name of alert to monitor
     *
     * @return previously live alert descriptor that was removed.
     */
    public AlertDescriptor stopMonitoringAlert(String queryId,String alertName)
    {
      AlertIdentifier ai = new AlertIdentifier(queryId, alertName);
      AlertDescriptor removedAlert = (AlertDescriptor)stopMonitoringObject(ai);
      return removedAlert;
    }

     /**
     * Returns true if an alert matching the given identifiers is currently
     * being updated by this monitor.
     *
     * @param queryId   id of query result adapter on aggregation agent that is
     *                  maintaining this alert.
     * @param alertName name of alert to monitor
     *
     * @return true if an alert matching the given identifiers is currently
     *         being updated by this monitor.
     */
    public boolean isMonitoring(String queryId, String alertName)
    {
      AlertIdentifier ai = new AlertIdentifier(queryId, alertName);
      return isMonitoring(ai);
    }

    /**
     * Provides a xml representation of a given alert identifier.
     *
     * @param identifier an object that uniquely identifies an alert on the
     *                   aggregation agent.
     *
     * @return a xml representation of given alert identifier.
     */
    protected String createIdTag(Object identifier)
    {
      AlertIdentifier ai = (AlertIdentifier)identifier;
      return "<alert query_id=\"" + ai.queryId +
             "\" alert_name=\"" + ai.alertName + "\"/>";
    }

    /**
     * Called when a update event (either add or change) is reported by the
     * aggregation agent to an alert described by the given xml element
     * tree.
     *
     * @param monitoredElement xml element tree that describes the updated
     *                         alert.
     *
     * @return a live alert descriptor updated based on the given xml
     */
    protected Object update(Element monitoredElement)
    {
      AlertDescriptor newAd = new AlertDescriptor(monitoredElement);
      AlertIdentifier ai =
        new AlertIdentifier(newAd.getQueryId(), newAd.getName());
      AlertDescriptor monitoredAlert =
        (AlertDescriptor)monitorObject(ai, newAd);
      monitoredAlert.setAlerted(newAd.isAlerted());
      return monitoredAlert;
    }

    /**
     * Called when a remove event is reported by the aggregation agent to an
     * alert described by the given xml element tree.
     *
     * @param monitoredElement xml element tree that describes the removed
     *                         alert.
     *
     * @return previously live alert descriptor that was removed.
     */
    protected Object remove(Element monitoredElement)
    {
      AlertDescriptor ad = new AlertDescriptor(monitoredElement);
      AlertDescriptor removedAlert =
        stopMonitoringAlert(ad.getQueryId(), ad.getName());
      removedAlert.fireObjectRemoved();
      return removedAlert;
    }

    /**
     * Used to uniquely identify an alert.  Can be used as a hashtable key.
     */
    private static class AlertIdentifier
    {
      public String queryId = null;
      public String alertName = null;
      public AlertIdentifier(String queryId, String alertName)
      {
        this.queryId = queryId;
        this.alertName = alertName;
      }
      public boolean equals(Object o)
      {
        if (o instanceof AlertIdentifier)
        {
          AlertIdentifier ai = (AlertIdentifier)o;
          return queryId.equals(ai.queryId) && alertName.equals(ai.alertName);
        }
        return false;
      }
      public int hashCode()
      {
        return queryId.hashCode() + alertName.hashCode();
      }
    }
  }
