
package org.cougaar.lib.aggagent.script;

import org.python.util.PythonInterpreter;
import org.python.core.Py;
import org.python.core.BytecodeLoader;

/**
 *  The purpose of the NoErrorPython class is to be a directly instantiable
 *  subclass of PythonInterpreter within the COUGAAR framework.  Interaction
 *  between COUGAAR's BootstrapClassLoader and JPython's custom ClassLoader
 *  turns out to be fatal (for JPython) unless this class is used.
 *  <br><br>
 *  Note:  This implementation is just a workaround for a problem existing in
 *  the BootstrapClassLoader.  The best solution is to fix the latter.
 */
public class NoErrorPython extends PythonInterpreter {
  /**
   *  Create a new
   */
  public NoErrorPython () {
    Py.getSystemState().setClassLoader(BytecodeLoader.class.getClassLoader());
  }
}
