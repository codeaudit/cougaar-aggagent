
package org.cougaar.lib.aggagent.script;

import java.io.StringReader;
import org.cougaar.lib.aggagent.query.Alert;
import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.query.*;

/**
 *  A concrete implementation of the Alert class that uses a SILK script as its
 *  handleUpdate method.
 */
public class SilkAlert extends Alert {
  private Procedure handler = null;

  public SilkAlert (String script) {
    handler = (Procedure) SI.eval(script);
  }

  /**
   *  Respond to a change in the result set by calling the SILK script.  Note
   *  that even though this method does not take arguments, the procedure
   *  defined in the script should accept one argument, which is the Alert
   *  Object itself.
   */
  public void handleUpdate () {
    handler.apply(new Object[] {this});
  }

// - - - - - - - Test code below this point - - - - - - - - - - - - - - - - - -

  private static String CODE =
    "(begin " +
      "(lambda (self) " +
        "(define qra (.getQueryAdapter self)) " +
        "(.setAlerted self (.hasNext (.getAllAtoms (.getResultSet qra))))))";

  public static void main (String[] argv) {
    Alert ale = new SilkAlert(CODE);
    QueryResultAdapter qra = new QueryResultAdapter((AggregationQuery) null);
    qra.addAlert(ale);
    test(ale, "Test 1", false);

    ResultSetDataAtom atom = new ResultSetDataAtom();
    atom.addIdentifier("name", "random value");
    atom.addValue("number", "5");
    // qra.getResultSet().update(atom);
    ale.update();
    test(ale, "Test 2", true);
  }

  private static void test (Alert a, String msg, boolean answer) {
    System.out.println(msg + ":  " + (a.isAlerted() == answer ? "Success" : "Failure"));
  }
}
