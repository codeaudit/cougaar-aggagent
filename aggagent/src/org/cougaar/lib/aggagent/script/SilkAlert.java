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

package org.cougaar.lib.aggagent.script;

import org.cougaar.lib.aggagent.query.AggregationQuery;
import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;

import silk.Procedure;
import silk.SI;

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
