package org.cougaar.lib.aggagent.query;

import org.w3c.dom.*;

import java.util.*;

/**
 *  This adapter contains a query and links to some associated structures.
 */
public class QueryResultAdapter
{
    private static int uniqueIdCounter = 0;
    private String id = null;
    private AggregationQuery aQuery = null;
    private AggregationResultSet aResultSet = null;
    private LinkedList alerts = new LinkedList();

    /**
     *  Create a QueryResultAdapter to contain a particular query.  At the
     *  current time, only one type of result set is supported, so it is
     *  automatically constructed and installed here.
     */
    public QueryResultAdapter(AggregationQuery q)
    {
      id = String.valueOf(uniqueIdCounter++);
      aQuery = q;
      setResultSet(new AggregationResultSet());
    }

    /**
     * Create a QueryResultAdapter based on xml
     */
    public QueryResultAdapter(Element qraRoot)
    {
      id = qraRoot.getAttribute("id");
      aQuery = new AggregationQuery(
        (Element)qraRoot.getElementsByTagName("query").item(0));
      setResultSet(new AggregationResultSet((Element)
                   qraRoot.getElementsByTagName("result_set").item(0)));
      NodeList alerts = qraRoot.getElementsByTagName("alert");
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
      this.id = id;
      aQuery = q;
      setResultSet(new AggregationResultSet());
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

    public void setResultSet(AggregationResultSet aResultSet)
    {
      this.aResultSet = aResultSet;
      aResultSet.setQueryAdapter(this);
    }

    public AggregationResultSet getResultSet()
    {
      return aResultSet;
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

    public String toXML()
    {
      StringBuffer s = new StringBuffer("<query_result_adapter");
      s.append(" id=\"" + id + "\">\n");
      s.append(aQuery.toXML());
      s.append(aResultSet.toXML());
      for (Iterator i = alerts.iterator(); i.hasNext(); )
      {
        AlertDescriptor ad = new AlertDescriptor((Alert)i.next());
        s.append(ad.toXML());
      }
      s.append("</query_result_adapter>");
      System.out.println(s);

      return s.toString();
    }

}