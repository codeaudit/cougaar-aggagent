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

import java.util.Collection;

import org.cougaar.lib.aggagent.session.XMLEncoder;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 *  An implementation of XMLEncoder that derives its functionality from
 *  a script written in the JPython language.  A PythXMLEncoder is configured
 *  in two stages:  one is to declare a function or class, and the other is to
 *  pass the function or an instance of the class to the controlling Java
 *  context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythXMLEncoder implementations is due to JPython's resolute refusal to
 *  allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an XMLEncoder instance or a function designed to act as
 *  the "encode" method of an XMLEncoder.
 *  <br><br>
 *  This class implements the XMLEncoder interface, and can be instantiated by
 *  calling the constructor or a static factory method, encoderFromScript().
 */
public class PythXMLEncoder implements XMLEncoder {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // Implementation of an XMLEncoder that uses a JPython function as a
  // delegate for the encode(Object, PrintStream) method.  Arguments are
  // coerced into the JPython context, where the function is executed.
  private static class Func implements XMLEncoder {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void encode (Object o, Collection out) {
      delegateFunction._jcall(new Object[] {o, out});
    }
  }

  // Each instance carrys a delegate XMLEncoder derived from a script
  private XMLEncoder delegate = null;

  /**
   *  Create a PythUnaryPredicate instance by using a script-generated
   *  UnaryPredicate as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythXMLEncoder (String script) {
    delegate = encoderFromScript(script);
  }

  /**
   *  An implementation of the encode method of interface XMLEncoder.  The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param o an object to be encoded
   *  @param out a Collection to which results of this operation are added
   */
  public void encode (Object o, Collection out) {
    delegate.encode(o, out);
  }

  /**
   *  Create an XMLEncoder from a JPython script.  There are two acceptable
   *  modes for the script.  Either it must produce a JPython subclass of Java
   *  interface XMLEncoder, or it must produce a JPython function that behaves
   *  like the method XMLEncoder.encode (i.e., takes two arguments and treats
   *  the second one like a PrintStream).  Either way, the script is required
   *  to define the magic function "instantiate()" to provide the function or
   *  encoder instance to the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an XMLEncoder instance derived from the JPython scripts
   */
  public static XMLEncoder encoderFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(XMLEncoder.class);
      if (obj instanceof XMLEncoder)
        return (XMLEncoder) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an XMLEncoder");
  }
}
