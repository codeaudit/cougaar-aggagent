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

import org.w3c.dom.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.cougaar.lib.aggagent.session.UpdateDelta;
import org.cougaar.lib.aggagent.session.XmlTransferable;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  A Repository for results being returned by Clusters for the associated
 *  AggregationQuery.
 */
public class AggregationResultSet implements XmlTransferable, Serializable {
  public static String RESULT_SET_TAG = "result_set";
  public static String QUERY_ID_ATT = "query_id";

  private static String EXCEPTION_TAG = "resultset_exception";
  private static String CLUSTER_ID_ATT = "clusterId";
  private static String CLUSTER_TAG = "cluster";
  private static String ID_ATT = "id";

  private static String CLUSTER_IDENTIFIER = "cluster";
  private static String AGGREGATED_IDENTIFIER = "aggregated";

  private Object lock = new Serializable(){};

  private QueryResultAdapter query = null;
  private List idNames = new LinkedList();
  private boolean firstUpdate = true;
  private Map clusterTable = new HashMap();
  private Map exceptionMap = new HashMap();
  private Set respondingClusters = new HashSet();
  private UpdateObservable updateObservable = new UpdateObservable();
  private List resultSetChangeListeners = new LinkedList();

  /**
   * Default Constructor
   */
  public AggregationResultSet() {}

  /**
   *  Create Result Set from xml.
   */
  public AggregationResultSet(Element root)
  {
    NodeList nl = root.getElementsByTagName(EXCEPTION_TAG);
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element exceptionElement = (Element)nl.item(i);
      String clusterId = exceptionElement.getAttribute(CLUSTER_ID_ATT);
      String exceptionDescription = XmlUtils.getElementText(exceptionElement);
      exceptionMap.put(clusterId, exceptionDescription);
    }

    nl = root.getElementsByTagName(CLUSTER_TAG);
    for (int i = 0; i < nl.getLength(); i++) {
      Element cluster = (Element) nl.item(i);
      String cid = cluster.getAttribute(ID_ATT);
      createAtomsByAgent(cid, cluster);
    }
  }

  private void createAtomsByAgent (String agentId, Element root) {
    NodeList nl = root.getElementsByTagName(ResultSetDataAtom.DATA_ATOM_TAG);
    for (int i = 0; i < nl.getLength(); i++)
      update(agentId, new ResultSetDataAtom((Element) nl.item(i)));
  }

  /**
   *  Specify the query (etc.) for this result set.
   */
  public void setQueryAdapter (QueryResultAdapter s) {
    query = s;
  }

  /**
   *  Provide access to this result set's QueryResultAdapter
   */
  public QueryResultAdapter getQueryAdapter () {
    return query;
  }

  /**
   * Set an exception message for a cluster that occured when attempting
   * to update this result set (or setup query).
   */
  public void setException(String clusterId, String exceptionMessage)
  {
    exceptionMap.put(clusterId, exceptionMessage);
  }

  /**
   * Return a map of exception descriptions thrown by source clusters when
   * attempting to update this result set. Map keys are clusterId strings.
   * Map values are exception description strings.
   */
  public Map getExceptionMap()
  {
    return exceptionMap;
  }

  /**
   * Return a string summary of exception descriptions thrown by source
   * clusters when attempting to update this result set.
   */
  public String getExceptionSummary()
  {
    StringBuffer s = new StringBuffer();
    for (Iterator i = exceptionMap.values().iterator(); i.hasNext();)
    {
      s.append(i.next().toString()+"\n");
      s.append("-----------------------------\n");
    }
    return s.toString();
  }

  /**
   * Returns true if an exception was thrown by a source cluster when
   * attempting to run the query for this result set.
   */
  public boolean exceptionThrown()
  {
    return exceptionMap.size() > 0;
  }

  /**
   *  Update this AggregationResultSet by inserting a new data atom into the
   *  table.  The provided clusterId identifies the cluster of origin of the
   *  datum.
   */
  private void update (String clusterId, ResultSetDataAtom atom) {
    if (firstUpdate) {
      firstUpdate = false;
      for (Iterator i = atom.getIdentifierNames(); i.hasNext(); )
        idNames.add(i.next());
    }

    Map data = (Map) clusterTable.get(clusterId);
    if (data == null)
      clusterTable.put(clusterId, data = new HashMap());

    data.put(atom.getKey(idNames), atom.getValueMap());

    synchronized (respondingClusters)
    {
      respondingClusters.add(clusterId);
    }
  }

  /**
   *  Remove a ResultSetDataAtom from the result set.
   */
  private void remove (String clusterId, ResultSetDataAtom atom) {
    Map data = (Map) clusterTable.get(clusterId);
    if (data != null)
      data.remove(atom.getKey(idNames));
  }

  /**
   *  Update this AggregationResultSet by inserting a series of data purported
   *  to come from the specified cluster.  The data are presented in XML format
   *  and must be parsed into individual ResultSetDataAtoms.
   */
  private void update (String agentId, Collection atoms) {
    for (Iterator i = atoms.iterator(); i.hasNext(); )
      update(agentId, (ResultSetDataAtom) i.next());
  }

  /**
   *  Remove a series of data from this result set.
   */
  private void remove (String agentId, Collection atoms) {
    for (Iterator i = atoms.iterator(); i.hasNext(); )
      remove(agentId, (ResultSetDataAtom) i.next());
  }

  private void removeAll (String agentId) {
    Map table = (Map) clusterTable.get(agentId);
    if (table != null)
      table.clear();
  }

  public void incrementalUpdate (UpdateDelta delta) {
    String agentId = delta.getAgentId();

    synchronized (respondingClusters)
    {
      respondingClusters.add(agentId);
    }

    // update result set based on incremental change xml
    synchronized (lock) {
      if (delta.isErrorReport()) {
        setException(delta.getAgentId(), delta.getErrorReport());
      }
      else if (delta.isReplacement()) {
        removeAll(agentId);
        update(agentId, delta.getReplacementList());
      }
      else {
        update(agentId, delta.getAddedList());
        update(agentId, delta.getChangedList());
        remove(agentId, delta.getRemovedList());
      }
    }
  }

  private void removeAllAtoms () {
    clusterTable.clear();
  }

  public void replaceAggregated (List atoms) {
    synchronized (lock) {
      removeAllAtoms();
      for (Iterator i = atoms.iterator(); i.hasNext(); )
        update(AGGREGATED_IDENTIFIER, (ResultSetDataAtom) i.next());
    }
    fireObjectChanged();
  }

  /**
   * Update this result set to match passed in result set
   */
  public void update(AggregationResultSet rs)
  {
    this.idNames = rs.idNames;
    this.firstUpdate = rs.firstUpdate;
    this.clusterTable = rs.clusterTable;
    this.exceptionMap = rs.exceptionMap;

    fireObjectChanged();
  }

  /**
   * Add an update listener to observe this object
   */
  public void addUpdateListener(UpdateListener ul)
  {
    updateObservable.addUpdateListener(ul);
  }

  /**
   * Remove an update listener such that it no longer gets notified of changes
   * to this object
   */
  public void removeUpdateListener(UpdateListener ul)
  {
    updateObservable.removeUpdateListener(ul);
  }

  /**
   * Send event to all update listeners indicating that object has been added
   * to the log plan.
   *
   * @param sourceObject object that has been added
   */
  public void fireObjectAdded()
  {
    updateObservable.fireObjectAdded(this);
  }

  /**
   * Send event to all update listeners indicating that object has been removed
   * from the log plan.
   *
   * @param sourceObject object that has been removed
   */
  public void fireObjectRemoved()
  {
    updateObservable.fireObjectRemoved(this);
  }

  /**
   * Send event to all update listeners indicating that object has been changed
   * on the log plan.
   *
   * @param sourceObject object that has been changed
   */
  private void fireObjectChanged()
  {
    updateObservable.fireObjectChanged(this);
  }

  public Iterator getAllAtoms () {
    List l = new LinkedList();
    synchronized (lock) {
      for (Iterator c = clusterTable.entrySet().iterator(); c.hasNext(); ) {
        Map.Entry cluster = (Map.Entry) c.next();
        Object name = cluster.getKey();
        Map atoms = (Map) cluster.getValue();
        for (Iterator v = atoms.entrySet().iterator(); v.hasNext(); ) {
          Map.Entry pair = (Map.Entry) v.next();
          ResultSetDataAtom a = new ResultSetDataAtom(
            idNames, (CompoundKey) pair.getKey(), (Map) pair.getValue());
          a.addIdentifier(CLUSTER_IDENTIFIER, name);
          l.add(a);
        }
      }
    }
    return l.iterator();
  }

  public String toXml () {
    InverseSax doc = new InverseSax();
    includeXml(doc);
    return doc.toString();
  }

  public void includeXml (InverseSax doc) {
    doc.addElement(RESULT_SET_TAG);
    if (query != null)
      doc.addAttribute(QUERY_ID_ATT, query.getID());

    synchronized (lock) {
      for (Iterator i = exceptionMap.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry) i.next();
        doc.addEltAttText(EXCEPTION_TAG, CLUSTER_ID_ATT,
          entry.getKey().toString(), entry.getValue().toString());
      }

      for (Iterator i = clusterTable.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry table = (Map.Entry) i.next();
        doc.addElement(CLUSTER_TAG);
        doc.addAttribute(ID_ATT, table.getKey().toString());
        for (Iterator j = ((Map) table.getValue()).entrySet().iterator();
            j.hasNext(); )
        {
          Map.Entry entry = (Map.Entry) j.next();
          (new ResultSetDataAtom(idNames, (CompoundKey) entry.getKey(),
            (Map) entry.getValue())).includeXml(doc);
        }
        doc.endElement();
      }
    }

    doc.endElement();
  }

  public Set getRespondingClusters() {
    // pass a copy back (iteration needs to be synchronized with adds)
    Set responded = null;
    synchronized (respondingClusters)
    {
      responded = new HashSet();
      Iterator iter = respondingClusters.iterator();
      while (iter.hasNext())
        responded.add(iter.next());
    }

    return responded;
  }
}
