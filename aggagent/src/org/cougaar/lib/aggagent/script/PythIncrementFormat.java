
package org.cougaar.lib.aggagent.script;

import org.cougaar.lib.planserver.ServerPlugInSupport;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.*;
import org.cougaar.util.*;
import java.util.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.naming.*;
import org.cougaar.lib.planserver.*;
import java.io.*;

import java.io.OutputStream;

import org.python.core.PyFunction;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.SubscriptionAccess;
import org.cougaar.lib.aggagent.session.RemoteBlackboardSubscription;

/**
 *  An implementation of IncrementFormat that derives its functionality from
 *  a script written in the JPython language.  A PythIncrementFormat is
 *  configured in two stages:  one is to declare a function or class, and the
 *  other is to pass the function or an instance of the class to the
 *  controlling Java context.
 *  <br><br>
 *  The necessity of using a two-stage initialization procedure for both types
 *  of PythIncrementFormat implementations is due to JPython's resolute refusal
 *  to allow an expression to declare a new function or class (or, indeed, any
 *  multiline construct).  One possibility is to use a "magic" function through
 *  which the Java and JPython contexts can communicate.  For the sake of
 *  uniformity, this option is used here.  The magic function is "instantiate",
 *  which the script should define in the global context as a no-arg function
 *  that returns either an IncrementFormat instance or a function designed to
 *  act as the "encode" method of an IncrementFormat.
 *  <br><br>
 *  This class implements the IncrementFormat interface, and can be
 *  instantiated by calling the constructor or a static factory method,
 *  formatFromScript().
 */
public class PythIncrementFormat implements IncrementFormat {
  // This is the JPython instruction evaluated to retrieve the product of the
  // script.  The script is responsible for providing the correct behavior to
  // the named function.
  private static String MAGIC_FUNCTION = "instantiate()";

  // IncrementFormat implementation that uses a JPython script as its encode
  // method.
  private static class Func implements IncrementFormat {
    private PyFunction delegateFunction = null;

    public Func (PyFunction f) {
      delegateFunction = f;
    }

    public void encode (OutputStream out, SubscriptionAccess sess, String key,
        String queryId, String clusterId)
    {
      delegateFunction._jcall(
        new Object[] {out, sess, key, queryId, clusterId});
    }
  }

  // Each instance carrys a delegate IncrementFormat derived from a script
  private IncrementFormat delegate = null;

  /**
   *  Create a PythIncrementFormat instance by using a script-generated
   *  IncrementFormat as a delegate.
   *  @param script the JPython script that defines the embodied functionality
   */
  public PythIncrementFormat (String script) {
    delegate = formatFromScript(script);
  }

  /**
   *  An implementation of the encode method of interface IncrementFormat.  The
   *  function is actually delegated to a script-generated implementation,
   *  which is fabricated in the constructor.
   *
   *  @param o an object to be encoded
   *  @param ps the PrintStream to which the XML is sent
   */
  public void encode (OutputStream out, SubscriptionAccess sess, String key,
      String queryId, String clusterId)
  {
    delegate.encode(out, sess, key, queryId, clusterId);
  }

  /**
   *  Create an IncrementFormat from a JPython script.  There are two
   *  acceptable modes for the script.  It must produce either a JPython
   *  subclass of Java interface IncrementFormat or a JPython function that
   *  behaves like the method IncrementFormat.encode (i.e., takes five
   *  arguments and treats them as an OutputStream, a SubscriptionAccess, and
   *  so forth). Either way, the script is required to define the magic
   *  function "instantiate()" to provide the function or formatter instance to
   *  the Java context.
   *
   *  @param script the executable script that declares classes and variables
   *  @return an IncrementFormat instance derived from the JPython scripts
   */
  public static IncrementFormat formatFromScript (String script) {
    PythonInterpreter pi = new NoErrorPython();
    if (script != null)
      pi.exec(script);
    PyObject product = pi.eval(MAGIC_FUNCTION);
    if (product instanceof PyFunction) {
      return new Func((PyFunction) product);
    }
    else {
      Object obj = product.__tojava__(IncrementFormat.class);
      if (obj instanceof IncrementFormat)
        return (IncrementFormat) obj;
    }
    throw new IllegalArgumentException(
      "JPython script did not yield a function or an IncrementFormat");
  }


// - - - - - - - Testing code below this point - - - - - - - - - - - - - - - - -

  private static String CLASS_FORMAT =
    "from org.cougaar.lib.aggagent.session import IncrementFormat\n" +
    "from java.io import PrintStream\n" +
    "class TrivialXmlizer (IncrementFormat):\n" +
    "  def codeList (self, out, name, list):\n" +
    "    out.print('<' + name + '>')\n" +
    "    while (list.hasNext()):\n" +
    "      out.print('<elt>' + list.next() + '</elt>')\n" +
    "    out.print('</' + name + '>')\n" +
    "  def encode (self, out, rs, key, qId, cId):\n" +
    "    ps = PrintStream(out)\n" +
    "    ps.print('<increment key=\"' + key + '\" qId=\"' + qId + '\" cId=\"' + cId + '\">')\n" +
    "    self.codeList(ps, 'added', rs.getAddedCollection().iterator())\n" +
    "    self.codeList(ps, 'changed', rs.getChangedCollection().iterator())\n" +
    "    self.codeList(ps, 'removed', rs.getRemovedCollection().iterator())\n" +
    "    ps.print('</increment>')\n" +
    "    ps.flush()\n" +
    "    return\n" +
    "def instantiate ():\n" +
    "  return TrivialXmlizer()\n";

  private static String FUNCTION_FORMAT =
    "from java.io import PrintStream\n" +
    "def codeList (out, name, list):\n" +
    "  out.print('<' + name + '>')\n" +
    "  while (list.hasNext()):\n" +
    "    out.print('<elt>' + list.next() + '</elt>')\n" +
    "  out.print('</' + name + '>')\n" +
    "def encode (out, rs, key, qId, cId):\n" +
    "  ps = PrintStream(out)\n" +
    "  ps.print('<increment key=\"' + key + '\" qId=\"' + qId + '\" cId=\"' + cId + '\">')\n" +
    "  codeList(ps, 'added', rs.getAddedCollection().iterator())\n" +
    "  codeList(ps, 'changed', rs.getChangedCollection().iterator())\n" +
    "  codeList(ps, 'removed', rs.getRemovedCollection().iterator())\n" +
    "  ps.print('</increment>')\n" +
    "  ps.flush()\n" +
    "  return\n" +
    "def instantiate ():\n" +
    "  return encode\n";

  private static String correctAnswer =
    "<increment key=\"key\" qId=\"queryId\" cId=\"clusterId\">" +
      "<added>" +
        "<elt>add_one</elt>" +
        "<elt>add_two</elt>" +
      "</added>" +
      "<changed>" +
        "<elt>change_three</elt>" +
        "<elt>change_four</elt>" +
      "</changed>" +
      "<removed>" +
        "<elt>remove_five</elt>" +
        "<elt>remove_six</elt>" +
      "</removed>" +
    "</increment>";

  public static void main (String[] argv) {
    IncrementFormat x1 = new PythIncrementFormat(CLASS_FORMAT);
    IncrementFormat x2 = new PythIncrementFormat(FUNCTION_FORMAT);
    ServerPlugInSupport spis = new FakeSPIS();
    RemoteBlackboardSubscription testSubject = new RemoteBlackboardSubscription(null, null);

    testSubject.subscriptionChanged(null);
    test(x1, "x1", testSubject, "key", "queryId", "clusterId", correctAnswer);

    testSubject.subscriptionChanged(null);
    test(x2, "x2", testSubject, "key", "queryId", "clusterId", correctAnswer);
  }

  private static void test (IncrementFormat x, String name,
      RemoteBlackboardSubscription rs, String key, String queryId, String clusterId,
      String out)
  {
    WriterStream ws = new WriterStream();
    x.encode(ws, rs, key, queryId, clusterId);
    String observed = ws.getBuffer().toString();
    if (out.equals(observed)) {
      System.out.println("Test for " + name + ":  Succeeded");
    }
    else {
      System.out.println("Test for " + name + ":  Failed");
      System.out.println("*** observed:");
      System.out.println(observed);
      System.out.println("*** in stead of:");
      System.out.println(out);
      System.out.println("***");
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

  private static class FakeSPIS implements ServerPlugInSupport {
    public void unsubscribeForSubscriber (Subscription p0) { }
    public void publishChangeForSubscriber (Object p0) { }
    public void publishAddForSubscriber (Object p0) { }
    public void publishRemoveForSubscriber (Object p0) { }
    public void openLogPlanTransaction () { }
    public void closeLogPlanTransaction () { }
    public void wakeLogPlan () { }

    public String getClusterIDAsString () { return null; }
    public RootFactory getFactoryForPSP () { return null; }
    public Collection queryForSubscriber (UnaryPredicate p0) { return null; }
    public PlugInDelegate getDirectDelegate () { return null; }
    public NamingService getNamingService () { return null; }

    public Subscription subscribe (UISubscriber p0, UnaryPredicate p1) {
      return new FakeSubscription();
    }
  }

  private static class FakeSubscription extends IncrementalSubscription {
    private Collection fakeAdd = new LinkedList();
    private Collection fakeCha = new LinkedList();
    private Collection fakeRem = new LinkedList();

    public FakeSubscription () {
      super(null, new HashSet());
      fakeAdd.add("add_one");
      fakeAdd.add("add_two");
      fakeCha.add("change_three");
      fakeCha.add("change_four");
      fakeRem.add("remove_five");
      fakeRem.add("remove_six");
    }

    public Collection getAddedCollection () {
      return fakeAdd;
    }

    public Collection getChangedCollection () {
      return fakeCha;
    }

    public Collection getRemovedCollection () {
      return fakeRem;
    }
  }
}
