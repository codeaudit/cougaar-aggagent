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

import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.query.QueryResultAdapter;

import org.cougaar.lib.aggagent.query.*;

/**
 *  An implementation of the Alert class that derives its functionality from a
 *  script written in the JPython language.  A PythAlert is configured in two
 *  stages:  one is to declare a function or class, and the other is to pass
 *  the function or an instance of the class to the controlling Java context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythAlert implementations is due to JPython's resolute refusal to allow
 *  an expression to declare a new function or class (or, indeed, any multiline
 *  construct).  One possibility is to use a "magic" function through which the
 *  Java and JPython contexts can communicate.  For the sake of uniformity,
 *  this option is used here.  The magic function is "instantiate", which the
 *  script should define in the global context as a no-arg function that
 *  returns either an Alert instance or a function designed to act as the
 *  abstract "handleUpdate" method of an Alert.
 *  <br><br>
 *  This class contains a static factory method, parseAlert(), for generating
 *  Alert instances.
 */
public abstract class PythAlert {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // Implementation of an Alert that uses a JPython function as a delegate for
  // the handleUpdate() method.  Arguments are coerced into the JPython
  // context, where the function is evaluated.  The returned value, if any, is
  // ignored.
  private static class Func extends Alert {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void handleUpdate () {
      delegateFunction._jcall(new Object[] {this});
    }
  }

  /**
   *  Create an Alert from a JPython script.  There are two acceptable modes
   *  for the script.  It must produce either an instance of a JPython subclass
   *  of Java class Alert or a JPython function that behaves like the method
   *  Alert.handleUpdate (i.e., takes one argument (that being the Alert's own
   *  self), returns no result, and updates the Alert's state to reflect the
   *  current result set data).  Either way, the script is required to define
   *  the magic function "instantiate()" to provide the function or predicate
   *  instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an Alert instance derived from the JPython scripts
   */
  public static Alert parseAlert (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(Alert.class);
      if (obj instanceof Alert)
        return (Alert) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an Alert");
  }


// - - - - - - - Testing code below this point - - - - - - - - - - - - - - - - -

  private static String ALERT_FUNCTION =
    "def handle (self):\n" +
    "  qra = self.getQueryAdapter()\n" +
    "  self.setAlerted(qra.getResultSet().getAllAtoms().hasNext())\n" +
    "def instantiate ():\n" +
    "  return handle\n";

  private static String ALERT_CLASS =
    "from org.cougaar.lib.aggagent.query import Alert\n" +
    "class Handler (Alert):\n" +
    "  def handleUpdate (self):\n" +
    "    qra = self.getQueryAdapter()\n" +
    "    self.setAlerted(qra.getResultSet().getAllAtoms().hasNext())\n" +
    "def instantiate ():\n" +
    "  return Handler()\n";

  public static void main (String[] argv) {
    Alert ale_1 = PythAlert.parseAlert(ALERT_FUNCTION);
    Alert ale_2 = PythAlert.parseAlert(ALERT_CLASS);
    QueryResultAdapter qra = new QueryResultAdapter((AggregationQuery) null);
    qra.addAlert(ale_1);
    qra.addAlert(ale_2);
    test(ale_1, "Test 1 (ale_1)", false);
    test(ale_2, "Test 1 (ale_2)", false);

    ResultSetDataAtom atom = new ResultSetDataAtom();
    atom.addIdentifier("name", "random value");
    atom.addValue("number", "5");
    // qra.getResultSet().update(atom);

    ale_1.update();
    test(ale_1, "Test 2 (ale_1)", true);
    ale_2.update();
    test(ale_2, "Test 2 (ale_2)", true);
  }

  private static void test (Alert a, String msg, boolean answer) {
    System.out.println(msg + ":  " + (a.isAlerted() == answer ? "Success" : "Failure"));
  }
}
