
package org.cougaar.lib.aggagent.script;

import java.util.List;

import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.cougaar.lib.aggagent.query.Aggregator;
import org.cougaar.lib.aggagent.query.AggregationResultSet;

/**
 *  An implementation of Aggregator that derives its functionality from a
 *  script written in the JPython language.  A PythAggregator is configured in
 *  two stages:  one is to declare a function or class, and the other is to
 *  pass the function or an instance of the class to the controlling Java
 *  context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythAggregator implementations is due to JPython's resolute refusal to
 *  allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an Aggregator instance or a function designed to act as
 *  the "aggregate" method of an Aggregator.
 *  <br><br>
 *  This class implements the Aggregator interface, and can be instantiated by
 *  calling the constructor or a static factory method, aggregatorFromScript().
 */
public class PythAggregator implements Aggregator {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // Aggregator implementation that uses a JPython script as its aggregate
  // method.
  private static class Func implements Aggregator {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void aggregate (AggregationResultSet rs, List output) {
      delegateFunction._jcall(new Object[] {rs, output});
    }
  }

  // Each instance carrys a delegate Aggregator derived from a script
  private Aggregator delegate = null;

  /**
   *  Create a PythAggregator instance by using a script-generated Aggregator
   *  as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythAggregator (String script) {
    delegate = aggregatorFromScript(script);
  }

  /**
   *  An implementation of the aggregate method of interface Aggregator. The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param ids a list of names for the identifier fields
   *  @param rs the result set containing the data to be aggregated
   */
  public void aggregate (AggregationResultSet rs, List output) {
    delegate.aggregate(rs, output);
  }

  /**
   *  Create an Aggregator from a JPython script.  There are two acceptable
   *  modes for the script.  It must produce either a JPython subclass of Java
   *  interface Aggregator or a JPython function that behaves like the method
   *  Aggregator.aggregate().  Either way, the script is required to define the
   *  magic function "instantiate()" to provide the function or Aggregator
   *  instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an Aggregator instance derived from the JPython scripts
   */
  public static Aggregator aggregatorFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(Aggregator.class);
      if (obj instanceof Aggregator)
        return (Aggregator) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an Aggregator");
  }
}
