
package org.cougaar.lib.aggagent.script;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;

import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.cougaar.lib.aggagent.session.XMLEncoder;

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

    public void encode (Object o, PrintStream ps) {
      delegateFunction._jcall(new Object[] {o, ps});
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
   *  @param ps the PrintStream to which the XML is sent
   */
  public void encode (Object o, PrintStream ps) {
    delegate.encode(o, ps);
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


// - - - - - - - Testing code below this point - - - - - - - - - - - - - - - - -

  private static String CLASS_FOR_XMLIZER =
    "from org.cougaar.lib.aggagent.session import XMLEncoder\n" +
    "class TrivialXmlizer (XMLEncoder):\n" +
    "  def encode (self, x, ps):\n" +
    "    ps.print('<value>')\n" +
    "    ps.print(x.toString())\n" +
    "    ps.print('</value>')\n" +
    "    ps.flush()\n" +
    "    return\n" +
    "def instantiate ():\n" +
    "  return TrivialXmlizer()\n";

  private static String FUNCTION_FOR_XMLIZER =
    "from org.cougaar.lib.aggagent.session import XMLEncoder\n" +
    "def encode (x, ps):\n" +
    "  ps.print('<value>')\n" +
    "  ps.print(x.toString())\n" +
    "  ps.print('</value>')\n" +
    "  ps.flush()\n" +
    "  return\n" +
    "def instantiate ():\n" +
    "  return encode\n";

  public static void main (String[] argv) {
    XMLEncoder x1 = new PythXMLEncoder(CLASS_FOR_XMLIZER);
    XMLEncoder x2 = new PythXMLEncoder(FUNCTION_FOR_XMLIZER);
    Object testSubject = new StringWrapper("Bla bla bla");
    String correctAnswer = "<value>Bla bla bla</value>";
    test(x1, "x1", testSubject, correctAnswer);
    test(x2, "x2", testSubject, correctAnswer);
  }

  private static void test (XMLEncoder x, String name, Object in, String out) {
    WriterStream ws = new WriterStream();
    x.encode(in, new PrintStream(ws));
    System.out.println("Test for " + name + ":  " + out.equals(ws.getBuffer().toString()));
  }

  // This class is much like String, but JPython does not coerce it into a
  // different form
  private static class StringWrapper {
    public String value = null;

    public StringWrapper (String v) {
      value = v;
    }

    public String toString () {
      return value;
    }
  }

  // same-old thing; cloak a Writer instance with an OutputStream interface
  private static class WriterStream extends OutputStream {
    private StringWriter w = new StringWriter();

    public StringBuffer getBuffer () {
      return w.getBuffer();
    }

    public void write (int b) throws IOException {
      w.write(b);
    }
  }
}