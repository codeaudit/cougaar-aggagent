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

import org.cougaar.util.UnaryPredicate;
import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 *  An implementation of UnaryPredicate that derives its functionality from
 *  a script written in the JPython language.  A PythUnaryPredicate is
 *  configured in two stages:  one is to declare a function or class, and the
 *  other is to pass the function or an instance of the class to the
 *  controlling Java context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythUnaryPredicate implementations is due to JPython's resolute refusal
 *  to allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either a UnaryPredicate instance or a function designed to act
 *  as the "execute" method of a UnaryPredicate.
 *  <br><br>
 *  This class implements the UnaryPredicate interface, and can be instantiated
 *  by calling the constructor or a static factory method,
 *  predicateFromScript().
 */
public class PythUnaryPredicate implements UnaryPredicate {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // Implementation of a UnaryPredicate that uses a JPython function as a
  // delegate for the execute(Object) method.  Arguments are coerced into the
  // JPython context, where the function is evaluated, and the return type is
  // interpreted as a boolean.
  private static class Func implements UnaryPredicate {
    private PyFunction delegateFunction = null;

    // Interpret the value returned by a JPython function call as a Java
    // boolean.  PyIntegers whose value is not zero are interpreted as "true",
    // while anything else is presumed to represent "false".
    private static boolean booleanVal (PyObject p) {
      if (p instanceof PyInteger)
        return ((PyInteger) p).getValue() != 0;
      return false;
    }

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public boolean execute (Object o) {
      return booleanVal(delegateFunction._jcall(new Object[] {o}));
    }
  }

  // Each instance carrys a delegate UnaryPredicate derived from a script
  private UnaryPredicate delegate = null;

  /**
   *  Create a PythUnaryPredicate instance by using a script-generated
   *  UnaryPredicate as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythUnaryPredicate (String script) {
    delegate = predicateFromScript(script);
  }

  /**
   *  An implementation of the execute method of interface UnaryPredicate.  The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param o an object to be tested
   *  @return true if and only if the object matches the predicate
   */
  public boolean execute (Object o) {
    return delegate.execute(o);
  }

  /**
   *  Create a UnaryPredicate from a JPython script.  There are two acceptable
   *  modes for the script.  Either it must produce a JPython subclass of Java
   *  interface UnaryPredicate, or it must produce a JPython function that
   *  behaves like the method UnaryPredicate.execute (i.e., takes one argument
   *  and returns a JPython-style boolean result (which is really a number
   *  where zero denotes "false" and any other value denotes "true")).  Either
   *  way, the script is required to define the magic function "instantiate()"
   *  to provide the function or predicate instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return a UnaryPredicate instance derived from the JPython scripts
   */
  public static UnaryPredicate predicateFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(UnaryPredicate.class);
      if (obj instanceof UnaryPredicate)
        return (UnaryPredicate) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or a UnaryPredicate");
  }


// - - - - - - - Testing code below this point - - - - - - - - - - - - - - - - -

  private static String PREDICATE_FOR_PUP =
    "from org.cougaar.util import UnaryPredicate\n" +
    "class PupFind (UnaryPredicate):\n" +
    "  def execute (self, obj):\n" +
    "    return isinstance(obj, UnaryPredicate)\n" +
    "def instantiate ():\n" +
    "  return PupFind()\n";

  private static String FUNCTION_FOR_PUP =
    "from org.cougaar.util import UnaryPredicate\n" +
    "def execute (obj):\n" +
    "  return isinstance(obj, UnaryPredicate)\n" +
    "def instantiate ():\n" +
    "  return execute\n";

  public static void main (String[] argv) {
    UnaryPredicate inst = new PythUnaryPredicate(PREDICATE_FOR_PUP);
    UnaryPredicate func = new PythUnaryPredicate(FUNCTION_FOR_PUP);
    Object o = new Object();
    System.out.println("Trial 1:  " + inst.execute(inst) + "  " + func.execute(inst));
    System.out.println("Trial 2:  " + inst.execute(func) + "  " + func.execute(func));
    System.out.println("Trial 3:  " + inst.execute(o) + "  " + func.execute(o));
  }
}
