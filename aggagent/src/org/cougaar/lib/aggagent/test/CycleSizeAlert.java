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

package org.cougaar.lib.aggagent.test;

import java.util.Iterator;

import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.query.ScriptSpec;
import org.cougaar.lib.aggagent.util.Enum.Language;
import org.cougaar.lib.aggagent.util.Enum.QueryType;
import org.cougaar.lib.aggagent.util.Enum.ScriptType;
import org.cougaar.lib.aggagent.util.Enum.XmlFormat;
import org.cougaar.util.UnaryPredicate;

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
   *  SILK scripts into an AggregationQuery instance.
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
    public boolean execute(Object o) {
      if (o instanceof Alert) {
//        Alert a = (Alert) o;
//        return a.isAlerted();
        return true;
      }
      return false;
    }
  }
  private static ActiveAlertSeeker activeAlerts = new ActiveAlertSeeker();

  public static UnaryPredicate getActiveAlertSeeker () {
    return activeAlerts;
  }
}
