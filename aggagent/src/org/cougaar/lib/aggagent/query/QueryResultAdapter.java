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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.session.XmlTransferable;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *  This adapter contains a query and links to some associated structures.
 */
public class QueryResultAdapter implements XmlTransferable, Serializable, UniqueObject {
    public static String QUERY_RESULT_TAG = "query_result_adapter";
    public static String ID_ATT = "id";

    private static int uniqueIdCounter = 0;
    private String id = null;
    private AggregationQuery aQuery = null;
    private AggregationResultSet rawResultSet = null;
    private List alerts = new LinkedList();
    private Vector addedClusters = new Vector();
    private Vector removedClusters = new Vector();
    private UID uid;

    private Aggregator agg;
    private AggregationResultSet aggResultSet;

    /**
     *  Create a QueryResultAdapter to contain a particular query.  At the
     *  current time, only one type of result set is supported, so it is
     *  automatically constructed and installed here.
     */
    public QueryResultAdapter(AggregationQuery q)
    {
      this(q, (UID) null);
    }
    
    public QueryResultAdapter(AggregationQuery q, UID uid)
    {
      id = String.valueOf(uniqueIdCounter++);
      setQuery(q);
      setResultSet(new AggregationResultSet());
      this.uid = uid;
    }

    /**
     * Create a QueryResultAdapter based on xml
     */
    public QueryResultAdapter(Element qraRoot)
    {
      id = qraRoot.getAttribute(ID_ATT);
      setQuery(new AggregationQuery((Element)
        qraRoot.getElementsByTagName(AggregationQuery.QUERY_TAG).item(0)));
      setResultSet(new AggregationResultSet((Element)
        qraRoot.getElementsByTagName(
          AggregationResultSet.RESULT_SET_TAG).item(0)));
      NodeList alerts = qraRoot.getElementsByTagName(Alert.ALERT_TAG);
      for (int i = 0; i < alerts.getLength(); i++)
      {
        addAlert(new AlertDescriptor((Element)alerts.item(i)));
      }
    }

    /**
     * Create a QueryResultAdapter with the given id.
     */
    public QueryResultAdapter(AggregationQuery q, String id)
    {
      this(q, id, null);
    }
    
    public QueryResultAdapter(AggregationQuery q, String id, UID uid)
    {
      this.id = id;
      this.uid = uid;
      setQuery(q);
      setResultSet(new AggregationResultSet());
    }

    private void setQuery (AggregationQuery q) {
      aQuery = q;
      ScriptSpec aggSpec = aQuery.getAggSpec();
      try {
        if (aggSpec != null) {
          agg = aggSpec.toAggregator();
          setAggResultSet(new AggregationResultSet());
        }
        else {
          agg = null;
        }
      }
      catch (Exception eek) {
        eek.printStackTrace();
      }
    }

    public void updateResults (UpdateDelta delta) {
      rawResultSet.incrementalUpdate(delta);
      aggregate();
    }

    /**
     *  Reconcile new cluster list with current list.  Updates the Agg Query and the result sets
     */
    public void updateClusters(Vector newClusters) {
      Iterator iter = aQuery.getSourceClustersVector().iterator();
      while (iter.hasNext()) {
        String clusterId = (String) iter.next();
        // if it's in the query, but not the new list, remove it from the query.
        // otherwise, remove it from the list of new clusters.  All the clusters remaining
        // in that list will be added to the query at the end.
        if (!newClusters.contains(clusterId))
          removeCluster(clusterId);
        else
          newClusters.remove(clusterId);
      }
      iter = newClusters.iterator();
      while (iter.hasNext()) {
        String clusterId = (String) iter.next();
        addCluster(clusterId);
      }
    }
    
    /**
     *  Add a cluster to the query.
     */
    public void addCluster(String clusterId) {
      synchronized (addedClusters) {
        addedClusters.add(clusterId);
        aQuery.addSourceCluster(clusterId);
      }
    }
    
    /**
     *  Remove a cluster from the query.  Will also clean the cluster out of the result sets.
     */
    public void removeCluster(String clusterId) {
      synchronized (removedClusters) {
        removedClusters.add(clusterId);
        aQuery.removeSourceCluster(clusterId);
        if (rawResultSet != null)
          rawResultSet.removeClusterId(clusterId);
        if (aggResultSet != null)
          aggResultSet.removeClusterId(clusterId);
      }
    }

    /**
     *  Retrieve the Vector of clusters that have been added to this
     *  QRA after it was created.  This will reset the list of added clusters
     *  when it is called
     */
    public Vector getAndResetAddedClusters() {
      Vector retVec = null;
      synchronized (addedClusters) {
        retVec = new Vector(addedClusters);
        addedClusters.clear();
      }      
      return retVec;
    }

    /**
     *  Retrieve the Vector of clusters that have been removed from this
     *  QRA after it was created.  This will reset the list of removed clusters
     *  when it is called
     */
    public Vector getAndResetRemovedClusters() {
      Vector retVec = null;
      synchronized (removedClusters) {
        retVec = new Vector(removedClusters);
        removedClusters.clear();
      }      
      return retVec;
    }
     /**
     *  Use the local Aggregator (if there is one) to derive an aggregated
     *  result set from the raw data supplied by the query.  If no Aggregator
     *  is present, then the call is ignored.
     */
    private void aggregate () {
      if (agg != null) {
        List atoms = new LinkedList();
        try {
          agg.aggregate(rawResultSet.getAllAtoms(), atoms);
        }
        catch (Exception eek) {
          eek.printStackTrace();
        }
        aggResultSet.replaceAggregated(atoms);
      }
    }

    public boolean allClustersResponded() {
      if (rawResultSet == null)
        return false;
      
      Set responded = rawResultSet.getRespondingClusters();

      Enumeration en = getQuery().getSourceClusters();
      while (en.hasMoreElements())
        if (!responded.contains(en.nextElement()))
          return false;

      return true;
    }

    /**
     *  Register an Alert as interested in events on this query.
     */
    public void addAlert (Alert a) {
      synchronized (alerts) {
        alerts.add(a);
      }
      a.setQueryAdapter(this);
    }

    /**
     * Unregister an Alert
     *
     * @return removed alert
     */
    public Alert removeAlert(String alertName)
    {
      synchronized (alerts) {
        for (Iterator i = alerts.iterator(); i.hasNext();)
        {
          Alert alert = (Alert)i.next();
          if (alert.getName().equals(alertName))
          {
            i.remove();
            return alert;
          }
        }
      }

      return null;
    }

    public void removeAlert (Alert a) {
      synchronized (alerts) {
        alerts.remove(a);
      }
    }

    /**
     *  Notify the registered Alerts that new information has become available
     *  for this query.  They will then examine the result set and respond as
     *  they see fit.
     */
    public Iterator getAlerts () {
      LinkedList ll = null;
      synchronized (alerts) {
        ll = new LinkedList(alerts);
      }
      return ll.iterator();
    }

    public AggregationQuery getQuery()
    {
      return aQuery;
    }

    private void setAggResultSet (AggregationResultSet rs) {
      aggResultSet = rs;
      rs.setQueryAdapter(this);
    }

    public void setResultSet (AggregationResultSet rs) {
      rawResultSet = rs;
      rawResultSet.setQueryAdapter(this);
    }

    public AggregationResultSet getResultSet () {
      if (agg != null)
        return aggResultSet;
      return rawResultSet;
    }

    public AggregationResultSet getRawResultSet () {
      return rawResultSet;
    }

    public boolean checkID(String id)
    {
      return this.id.equals(id);
    }

    public String getID()
    {
      return id;
    }

    public String toString()
    {
      return getQuery().getName() + " (" + getID() + ")";
    }

    /**
     *  Convert this QueryResultAdapter to an XML format.  For most purposes,
     *  this means giving a summary of the resident result set.  For a complete
     *  document describing the query, result set, alerts, etc., use method
     *  toWholeXml().
     */
    public String toXml () {
      return getResultSet().toXml();
    }

    public void includeXml (InverseSax doc) {
      getResultSet().includeXml(doc);
    }

    public String toWholeXml () {
      InverseSax doc = new InverseSax();
      includeWholeXml(doc);
      return doc.toString();
    }

    public void includeWholeXml (InverseSax doc) {
      doc.addElement(QUERY_RESULT_TAG);
      doc.addAttribute(ID_ATT, id);
      aQuery.includeXml(doc);
      getResultSet().includeXml(doc);
      for (Iterator i = alerts.iterator(); i.hasNext(); )
        (new AlertDescriptor((Alert) i.next())).includeXml(doc);
      doc.endElement();
    }
    
    /*
    *** UniqueObject interface
     */

    public void setUID(UID uid) {
      throw new RuntimeException("Attempt to change UID");
    }

    public UID getUID() {
      return uid;
    }
}
