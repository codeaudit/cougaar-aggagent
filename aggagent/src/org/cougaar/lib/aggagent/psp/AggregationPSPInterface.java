package org.cougaar.lib.aggagent.psp;

import java.io.PrintStream;
import java.util.*;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.lib.aggagent.util.AdvancedHttpInput;

public abstract class AggregationPSPInterface
{
  protected static String selfName = "assessment.psp";

  public AggregationPSPInterface () {
  }

  public abstract void handleRequest(PrintStream out, AdvancedHttpInput ahi,
                                     PlanServiceContext psc);

  private static abstract class ClassSeeker implements UnaryPredicate
  {
    private Class targetClass;
    private Collection identifiers = null;

    public ClassSeeker(Class targetClass)
    {
      this.targetClass = targetClass;
    }

    public ClassSeeker(Class targetClass, Object identifier)
    {
      this(targetClass);
      identifiers = new LinkedList();
      identifiers.add(identifier);
    }

    public ClassSeeker(Class targetClass, Collection identifiers)
    {
      this(targetClass);
      this.identifiers = identifiers;
    }

    public boolean execute (Object o)
    {
      if (targetClass.isInstance(o))
      {
        if (identifiers == null)
          return true;

        for (Iterator i = identifiers.iterator(); i.hasNext();)
        {
          if (isMatch(i.next(), o))
            return true;
        }
      }

      return false;
    }

    protected abstract boolean isMatch(Object identifier, Object planObject);
  }

  // used to get all queries, a set of queries or a single, specified query
  protected static class QuerySeeker extends ClassSeeker
  {
    public QuerySeeker()
    {
      super(QueryResultAdapter.class);
    }

    public QuerySeeker(String queryId)
    {
      super(QueryResultAdapter.class, queryId);
    }

    public QuerySeeker(Collection queryIds)
    {
      super(QueryResultAdapter.class, queryIds);
    }

    protected boolean isMatch(Object identifier, Object planObject)
    {
      String queryId = (String)identifier;
      QueryResultAdapter qra = (QueryResultAdapter)planObject;
      return qra.checkID(queryId);
    }
  }

  // used to uniquely identify an alert
  protected static class AlertIdentifier
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
  }

  // look for alerts on the logplan with this UnaryPredicate class.
  protected static class AlertSeeker extends ClassSeeker
  {
    public AlertSeeker()
    {
      super(Alert.class);
    }

    public AlertSeeker(AlertIdentifier ai)
    {
      super(Alert.class, ai);
    }

    public AlertSeeker(Collection c)
    {
      super(Alert.class, c);
    }

    protected boolean isMatch(Object identifier, Object planObject)
    {
      AlertIdentifier ai = (AlertIdentifier)identifier;
      Alert alert = (Alert)planObject;
      return (alert.getQueryAdapter().checkID(ai.queryId) &&
              alert.getName().equals(ai.alertName));
    }
  }

  protected void waitForAndReturnResults(String queryId,
                                       PlanServiceContext psc,
                                       PrintStream out, boolean xml)
  {
    class ChangeListener implements UISubscriber
    {
      public Object localLock = new Object();
      public QueryResultAdapter changedQra = null;

      public void waitForChange () {
        synchronized (localLock) {
          try {
            localLock.wait();
          }
          catch (InterruptedException bla) { }
        }
      }

      public void subscriptionChanged(Subscription s)
      {
        synchronized(localLock)
        {
          Enumeration changedList =
            ((IncrementalSubscription)s).getChangedList();
          if (changedList.hasMoreElements())
          {
            changedQra = (QueryResultAdapter)changedList.nextElement();
            if (allClustersResponded(changedQra)) {
              //System.out.println("All clusters checked in");
              localLock.notify();
            } else {
              //System.out.println("Still waiting for some clusters");
            }
          }
        }
      }

      private boolean allClustersResponded(QueryResultAdapter qra) {
        HashSet responded = new HashSet();

        Iterator iter = qra.getRawResultSet().getRespondingClusters();
        while (iter.hasNext())
          responded.add(iter.next());

        Enumeration enum = qra.getQuery().getSourceClusters();
        while (enum.hasMoreElements())
          if (!responded.contains(enum.nextElement()))
            return false;

        return true;
      }

    }

    ServerPlugInSupport spis = psc.getServerPlugInSupport();

    ChangeListener cl = new ChangeListener();
    UnaryPredicate queryMonitor = new QuerySeeker(queryId);
    Subscription s = spis.subscribe(cl, queryMonitor);

    // wait for a publish change event on the query result adapter
    // and then get and return result set
    cl.waitForChange();
    spis.unsubscribeForSubscriber(s);
    if (xml)
    {
      out.println(cl.changedQra.getResultSet().toXml());
    }
    else
    {
      printQueryReportPage(cl.changedQra, out);
    }
    removeQuery(spis, cl.changedQra);
  }

  // Remove the query from the logplan as well as any Alerts that depend on it
  protected static void removeQuery (
    ServerPlugInSupport spis, QueryResultAdapter q)
  {
    for (Iterator i = q.getAlerts(); i.hasNext(); )
      spis.publishRemoveForSubscriber(i.next());
    spis.publishRemoveForSubscriber(q);
  }

  /**
   * Get QueryResultAdapter for given query id;
   * return null if query is not found.
   */
  protected QueryResultAdapter findQuery(String queryId,PlanServiceContext psc)
  {
    Iterator qras = psc.getServerPlugInSupport().
      queryForSubscriber(new QuerySeeker(queryId)).iterator();

    return qras.hasNext() ? (QueryResultAdapter)qras.next() : null;
  }

  // used to be "removeQuery" --
  // find the query matching the specified ID and remove it from the logplan
  protected void findAndRemoveQuery (String queryId, PlanServiceContext psc)
  {
    ServerPlugInSupport spis = psc.getServerPlugInSupport();

    // find query adapter on log plan
    Collection qs = spis.queryForSubscriber(new QuerySeeker(queryId));
    Iterator qi = qs.iterator();

    // remove all queries matching query id
    while (qi.hasNext())
      removeQuery(spis, (QueryResultAdapter) qi.next());
  }

  protected void printQueryReportPage (QueryResultAdapter qra, PrintStream out){
    out.println("<CENTER>");
    out.println(HTMLPresenter.toHTML(qra));
    out.println("</CENTER>");
  }
}