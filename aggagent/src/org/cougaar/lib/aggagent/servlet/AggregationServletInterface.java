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
package org.cougaar.lib.aggagent.servlet;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.AggregationResultSet;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.util.UnaryPredicate;

public abstract class AggregationServletInterface
{
  private BlackboardService blackboard;
  private SubscriptionMonitorSupport subscriptionMonitorSupport;
  private long timeoutDefault = 0;  // this can be overridden by argument to waitForAndReturnResults
  private UIDService uidService = null;
  
  public AggregationServletInterface (
                        BlackboardService blackboard,
                        SubscriptionMonitorSupport subscriptionMonitorSupport,
                        UIDService uidService)
  {
    this.blackboard = blackboard;
    this.subscriptionMonitorSupport = subscriptionMonitorSupport;
    this.uidService = uidService;
    String timeoutStr = System.getProperty("org.cougaar.lib.aggagent.timeout");
    if (timeoutStr != null) {
      try {
        timeoutDefault = Long.parseLong(timeoutStr);
      } catch (NumberFormatException nfe) {
        System.err.println("WARNING: Received invalid number for org.cougaar.lib.aggagent.timeout: " + timeoutStr);
        timeoutDefault = 0;
      }
    }
  }

  protected UIDService getUIDService() {
    return uidService;
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
    waitForAndReturnResults(queryId, out, xml, timeoutDefault);
  }
  
  protected void waitForAndReturnResults(String queryId,
                                         PrintWriter out, boolean xml,
                                         final long timeout)
  {
    class ChangeListener implements SubscriptionListener
    {
      public QueryResultAdapter changedQra = null;

      public synchronized void waitForChange () {
          try {
            this.wait(timeout);
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
            if (changedQra.allClustersResponded()) {
              //System.out.println("All clusters checked in");
              this.notify();              
            } else {
              //System.out.println("Still waiting for some clusters");
            }
          }
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

    // It's possible that we never heard any updates on the QRA.  If so, retrieve it
    // from the Blackboard now.
    if (cl.changedQra == null) {
        cl.changedQra = (QueryResultAdapter) ((IncrementalSubscription)s).first();
    }

    // Set any unresponding agents as exceptions on the result set
    Set responded = cl.changedQra.getRawResultSet().getRespondingClusters();
    Enumeration en = cl.changedQra.getQuery().getSourceClusters();
    AggregationResultSet results = cl.changedQra.getResultSet();
    while (en.hasMoreElements()) {
      String clusterID = (String) en.nextElement();
      if (!responded.contains(clusterID))
        results.setException(clusterID, "Agent " + clusterID + " did not respond to query before timeout occurred");
    }
    
    unsubscribe(s);
    if (xml)
    {
      out.println(results.toXml());
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

  // Update the query on the blackboard using the given Agg Query
  protected void updateQuery (QueryResultAdapter q, AggregationQuery newQuery)
  {
    q.updateClusters(newQuery.getSourceClustersVector());
    try {
      blackboard.openTransaction();
      blackboard.publishChange(q);
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

  // find the query matching the specified ID and remove it from the logplan
  protected void findAndUpdateQuery (String queryId, AggregationQuery aq)
  {
    // find query adapter on log plan
    blackboard.openTransaction();
    Collection qs = blackboard.query(new QuerySeeker(queryId));
    blackboard.closeTransaction();
    Iterator qi = qs.iterator();

    // remove all queries matching query id
    while (qi.hasNext()) {
      updateQuery((QueryResultAdapter) qi.next(), aq);
    }
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
      blackboard.closeTransactionDontReset();
    }
  }

  protected void publishChange(Object o)
  {
    try {
      blackboard.openTransaction();
      blackboard.publishChange(o);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  protected void publishRemove(Object o)
  {
    try {
      blackboard.openTransaction();
      blackboard.publishRemove(o);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }

  private void unsubscribe(Subscription subscription)
  {
    try {
      blackboard.openTransaction();
      blackboard.unsubscribe(subscription);
    } finally {
      blackboard.closeTransactionDontReset();
    }
  }
}
