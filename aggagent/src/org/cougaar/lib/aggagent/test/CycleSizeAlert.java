
package org.cougaar.lib.aggagent.test;

import java.util.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.*;
import org.cougaar.lib.aggagent.util.Enum.*;

/**
 *  For testing Alert functionality with the old NumberCycle test scaffolding.
 *  A CycleSizeAlert is a concrete Alert that scans the result set for values
 *  below a specified threshold and fires whenever one is discovered.  Also
 *  included for convenience are static functions to create default instances,
 *  compatible queries, and a predicate for locating Alerts on the blackboard.
 */
public class CycleSizeAlert extends Alert {
  private static int count = 0;

  private int threshold = 0;

  public CycleSizeAlert (int t) {
    threshold = t;
    setName("Default_" + (count++));
  }

  public void handleUpdate () {
    System.out.println("CycleSizeAlert::handleUpdate:  called");
    Iterator i = getQueryAdapter().getResultSet().getAllAtoms();
    while (i.hasNext())
    {
      ResultSetDataAtom d = (ResultSetDataAtom) i.next();
      try {
        int val = Integer.parseInt(d.getValue("value").toString());
        if (val < threshold) {
          setAlerted(true);
          return;
        }
      }
      catch (Exception eek) { }
    }
    setAlerted(false);
  }

  /**
   *  Because I am supremely lazy, this method automatically feeds suitable
   *  SILK scripts into an AssessmentQuery instance.
   */
  public static AggregationQuery createDefaultQuery () {
    AggregationQuery q =
      new AggregationQuery(QueryType.PERSISTENT);
    q.setName("Test Query");
    q.addSourceCluster("TestSource");

    q.setPredicateSpec(new ScriptSpec(ScriptType.UNARY_PREDICATE, Language.SILK,
      "(begin\n" +
      "  (import \"org.cougaar.lib.aggagent.test.NumberCycle\")\n" +
      "  (lambda (obj)\n" +
      "    (.isInstance NumberCycle.class obj)))"
    ));

    q.setFormatSpec(new ScriptSpec(Language.SILK, XmlFormat.XMLENCODER,
      "(begin\n" +
      "  (import \"org.cougaar.lib.aggagent.query.ResultSetDataAtom\")\n" +
      "  (lambda (nc ps)\n" +
      "    (let ((data_atom (ResultSetDataAtom.)))\n" +
      "      (.addIdentifier data_atom \"length\" (.getLength nc))\n" +
      "      (.addValue      data_atom \"value\"  (.getValue nc))\n" +
      "      (.println ps (.toXML data_atom))\n" +
      "      (.flush ps))))\n"
    ));

    return q;
  }

  /**
   *  Create a "default" instance of CycleSizeAlert.  The default value
   *  threshold is 2.
   */
  public static CycleSizeAlert getDefaultAlert () {
    return new CycleSizeAlert(2);
  }

  // look for alerts on the logplan with this UnaryPredicate class.
  private static class ActiveAlertSeeker implements UnaryPredicate {
    public boolean execute (Object o) {
      if (o instanceof Alert) {
        Alert a = (Alert) o;
        return true;
        // return a.isAlerted();
      }
      return false;
    }
  }
  private static ActiveAlertSeeker activeAlerts = new ActiveAlertSeeker();

  public static UnaryPredicate getActiveAlertSeeker () {
    return activeAlerts;
  }
}