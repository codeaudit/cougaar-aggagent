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
