
package org.cougaar.lib.aggagent.query;

import org.w3c.dom.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  A Repository for results being returned by Clusters for the associated
 *  AggregationQuery.
 */
public class AggregationResultSet
{
  private static String CLUSTER_IDENTIFIER = "cluster";

  private Object lock = new Object();

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
    NodeList nl = root.getElementsByTagName("resultset_exception");
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element exceptionElement = (Element)nl.item(i);
      String clusterId = exceptionElement.getAttribute("clusterId");
      String exceptionDescription = XmlUtils.getElementText(exceptionElement);
      exceptionMap.put(clusterId, exceptionDescription);
    }

    nl = root.getElementsByTagName("cluster");
    for (int i = 0; i < nl.getLength(); i++) {
      Element cluster = (Element) nl.item(i);
      String cid = cluster.getAttribute("id");
      update(cid, cluster);
    }
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
      s.append(i.next().toString());
      s.append("-----------------------------");
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

    respondingClusters.add(clusterId);
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
  private void update (String clusterId, Element atoms) {
    NodeList nl = atoms.getElementsByTagName(ResultSetDataAtom.DATA_ATOM_TAG);
    for (int i = 0; i < nl.getLength(); i++)
      update(clusterId, new ResultSetDataAtom((Element) nl.item(i)));
  }

  /**
   *  Remove a series of data from this result set.
   */
  private void remove (String clusterId, Element atoms) {
    NodeList nl = atoms.getElementsByTagName(ResultSetDataAtom.DATA_ATOM_TAG);
    for (int i = 0; i < nl.getLength(); i++)
      remove(clusterId, new ResultSetDataAtom((Element) nl.item(i)));
  }

  public void incrementalUpdate(String clusterId, Element root)
  {
    respondingClusters.add(clusterId);

    if (root == null)
      return;

    if (root.getNodeName().equals("exception"))
    {
      String exceptionMessage = XmlUtils.getElementText(root);
      exceptionMap.put(clusterId, exceptionMessage);
      return;
    }

    // update result set based on incremental change xml
    NodeList nl = root.getChildNodes();
    synchronized (lock) {
      for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          Element child = (Element) n;
          String s = child.getNodeName();
          System.out.println(s);
          if (s.equals("added") || s.equals("changed"))
          {
            update(clusterId, child);
          }
          else if (s.equals("removed"))
          {
            remove(clusterId, child);
          }
        }
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
        update("aggregated", (ResultSetDataAtom) i.next());
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

  public String toXML()
  {
    return toXML("");
  }

  public String toXML (String attribs) {
    StringBuffer s = new StringBuffer("<result_set ");
    s.append(attribs);
    s.append(">\n");
    synchronized (lock) {
      for (Iterator i = exceptionMap.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry) i.next();
        s.append("<resultset_exception clusterId=\"");
        s.append(entry.getKey().toString());
        s.append("\">\n");
        s.append(XmlUtils.replaceIllegalChars(entry.getValue().toString()));
        s.append("\n</resultset_exception>\n");
      }

      for (Iterator i = clusterTable.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry table = (Map.Entry) i.next();
        s.append("<cluster id=\"");
        s.append(table.getKey());
        s.append("\">\n");
        for (Iterator j = ((Map) table.getValue()).entrySet().iterator();
            j.hasNext(); )
        {
          Map.Entry entry = (Map.Entry) j.next();
          s.append(new ResultSetDataAtom(idNames, (CompoundKey) entry.getKey(),
            (Map) entry.getValue()).toXML());
        }
        s.append("</cluster>\n");
      }
    }
    s.append("</result_set>\n");

    return s.toString();
  }

  public Iterator getRespondingClusters() {
    return respondingClusters.iterator();
  }
}
