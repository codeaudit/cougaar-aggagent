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

import java.util.List;

import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.cougaar.lib.aggagent.query.CompoundKey;
import org.cougaar.lib.aggagent.query.DataAtomMelder;

/**
 *  An implementation of DataAtomMelder that derives its functionality from a
 *  script written in the JPython language.  A PythMelder is configured in two
 *  stages:  one is to declare a function or class, and the other is to pass
 *  the function or an instance of the class to the controlling Java context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythMelder implementations is due to JPython's resolute refusal to
 *  allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an DataAtomMelder instance or a function designed to
 *  act as the "aggregate" method of an DataAtomMelder.
 *  <br><br>
 *  This class implements the DataAtomMelder interface, and can be instantiated
 *  by calling the constructor or a static factory method, melderFromScript().
 */
public class PythMelder implements DataAtomMelder {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // DataAtomMelder implementation that uses a JPython script as its aggregate
  // method.
  private static class Func implements DataAtomMelder {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void meld (List idNames, CompoundKey id, List atoms, List output) {
      delegateFunction._jcall(new Object[] {idNames, id, atoms, output});
    }
  }

  // Each instance carrys a delegate DataAtomMelder derived from a script
  private DataAtomMelder delegate = null;

  /**
   *  Create a PythMelder instance by using a script-generated DataAtomMelder
   *  as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythMelder (String script) {
    delegate = melderFromScript(script);
  }

  /**
   *  An implementation of the aggregate method of interface DataAtomMelder. The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param ids a list of names for the identifier fields
   *  @param rs the result set containing the data to be aggregated
   */
  public void meld (List idNames, CompoundKey id, List atoms, List output) {
    delegate.meld(idNames, id, atoms, output);
  }

  /**
   *  Create an DataAtomMelder from a JPython script.  There are two acceptable
   *  modes for the script.  It must produce either a JPython subclass of Java
   *  interface DataAtomMelder or a JPython function that behaves like the method
   *  DataAtomMelder.aggregate().  Either way, the script is required to define the
   *  magic function "instantiate()" to provide the function or DataAtomMelder
   *  instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an DataAtomMelder instance derived from the JPython scripts
   */
  public static DataAtomMelder melderFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(DataAtomMelder.class);
      if (obj instanceof DataAtomMelder)
        return (DataAtomMelder) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an DataAtomMelder");
  }
}
