package org.cougaar.lib.aggagent.servlet;

import java.io.PrintWriter;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.SubscriptionWatcher;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.session.RemoteBlackboardSubscription;
import org.cougaar.lib.aggagent.session.SessionManager;

public abstract class AggregationServletInterface
{
  private BlackboardService blackboard;
  private SubscriptionMonitorSupport subscriptionMonitorSupport;

  public AggregationServletInterface (
                        BlackboardService blackboard,
                        SubscriptionMonitorSupport subscriptionMonitorSupport)
  {
    this.blackboard = blackboard;
    this.subscriptionMonitorSupport = subscriptionMonitorSupport;
  }

  public abstract void handleRequest(PrintWriter out,
                                     HttpServletRequest request);

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
                                         PrintWriter out, boolean xml)
  {
    class ChangeListener implements SubscriptionListener
    {
      public QueryResultAdapter changedQra = null;

      public synchronized void waitForChange () {
          try {
            this.wait();
          }
          catch (InterruptedException bla) { }
      }

      public synchronized void subscriptionChanged(Subscription s)
      {
          Enumeration changedList =
            ((IncrementalSubscription)s).getChangedList();
          if (changedList.hasMoreElements())
          {
            changedQra = (QueryResultAdapter)changedList.nextElement();
            if (allClustersResponded(changedQra)) {
              //System.out.println("All clusters checked in");
              this.notify();              
            } else {
              //System.out.println("Still waiting for some clusters");
            }
          }
      }

      private boolean allClustersResponded(QueryResultAdapter qra) {
        Set responded = qra.getRawResultSet().getRespondingClusters();

        Enumeration enum = qra.getQuery().getSourceClusters();
        while (enum.hasMoreElements())
          if (!responded.contains(enum.nextElement()))
            return false;

        return true;
      }

    }

    ChangeListener cl = new ChangeListener();
    UnaryPredicate queryMonitor = new QuerySeeker(queryId);
    Subscription s = null;
    try {
        blackboard.openTransaction();
        s = blackboard.subscribe(queryMonitor);
        subscriptionMonitorSupport.setSubscriptionListener(s, cl);
    } finally {
        synchronized (cl) {  
            // synchronized (cl) is necessary in the rare case that the agg is done 
            // before I get to the waitForChange()
            blackboard.closeTransaction();
            // wait for a publish change event on the query result adapter
            // and then get and return result set
            cl.waitForChange();
        }
    }

    unsubscribe(s);
    if (xml)
    {
      out.println(cl.changedQra.getResultSet().toXml());
    }
    else
    {
      printQueryReportPage(cl.changedQra, out);
    }
    removeQuery(cl.changedQra);
  }

  // Remove the query from the logplan as well as any Alerts that depend on it
  protected void removeQuery (QueryResultAdapter q)
  {
    try {
      blackboard.openTransaction();
      for (Iterator i = q.getAlerts(); i.hasNext(); )
        blackboard.publishRemove(i.next());
      blackboard.publishRemove(q);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      blackboard.closeTransaction();
    }
  }

  /**
   * Get QueryResultAdapter for given query id;
   * return null if query is not found.
   */
  protected QueryResultAdapter findQuery(String queryId)
  {
    Iterator qras = query(new QuerySeeker(queryId)).iterator();
    return qras.hasNext() ? (QueryResultAdapter)qras.next() : null;
  }

  // used to be "removeQuery" --
  // find the query matching the specified ID and remove it from the logplan
  protected void findAndRemoveQuery (String queryId)
  {
    // find query adapter on log plan
    blackboard.openTransaction();
    Collection qs = blackboard.query(new QuerySeeker(queryId));
    blackboard.closeTransaction();
    Iterator qi = qs.iterator();

    // remove all queries matching query id
    while (qi.hasNext())
      removeQuery((QueryResultAdapter) qi.next());
  }

  protected void printQueryReportPage (QueryResultAdapter qra, PrintWriter out){
    out.println("<CENTER>");
    out.println(HTMLPresenter.toHTML(qra));
    out.println("</CENTER>");
  }

  // for access to the blackboard

  protected Collection query(UnaryPredicate predicate)
  {
    Collection ret;
    blackboard.openTransaction();
    ret = blackboard.query(predicate);
    blackboard.closeTransaction();
    return ret;

  }

  protected void publishAdd(Object o)
  {
    try {
      blackboard.openTransaction();
      blackboard.publishAdd(o);
    } finally {
      blackboard.closeTransaction(false);
    }
  }

  protected void publishChange(Object o)
  {
    try {
      blackboard.openTransaction();
      blackboard.publishChange(o);
    } finally {
      blackboard.closeTransaction(false);
    }
  }

  protected void publishRemove(Object o)
  {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(o);
    } finally {
      blackboard.closeTransaction(false);
    }
  }

  private void unsubscribe(Subscription subscription)
  {
    try {
      blackboard.openTransaction();
      blackboard.unsubscribe(subscription);
    } finally {
      blackboard.closeTransaction(false);
    }
  }
}