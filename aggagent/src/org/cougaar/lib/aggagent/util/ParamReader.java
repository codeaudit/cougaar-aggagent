package org.cougaar.lib.aggagent.util;

import java.io.*;
import java.net.*;

public class ParamReader {
  private Reader in = null;
  private State currentState = null;

  private ParamReader (Reader r) {
    in = r;
  }

  private void configStates () {
    BetweenTokens b = new BetweenTokens();
    InName n = new InName();
    BeforeValue f = new BeforeValue();
    InValue v = new InValue();

    b.setTransState(n);
    n.setTransStates(f, b);
    f.setTransStates(v, b);
    v.setEndState(b);

    currentState = b;
  }

  private void configNoParse () {
    BetweenTokens b = new BetweenTokens();
    InValue v = new InToken();

    b.setTransState(v);
    v.setEndState(b);

    currentState = b;
  }

  public static ParamReader parsingReader (Reader r) {
    ParamReader ret = new ParamReader(r);
    ret.configStates();
    return ret;
  }

  public static ParamReader parsingReader (InputStream s) {
    return parsingReader(new InputStreamReader(s));
  }

  public static ParamReader nonParsingReader (Reader r) {
    ParamReader ret = new ParamReader(r);
    ret.configNoParse();
    return ret;
  }

  public static ParamReader nonParsingReader (InputStream s) {
    return nonParsingReader(new InputStreamReader(s));
  }

  public Parameter readParameter () throws IOException {
    if (currentState.eof())
      return null;
    currentState.reset();
    while (!currentState.done())
      currentState = currentState.transition(in.read());

    return currentState.getParameter().convert();
  }

  private static abstract class State {
    protected ParameterBuffer current = null;

    public void setParameter (ParameterBuffer p) {
      current = p;
    }

    public ParameterBuffer getParameter () {
      return current;
    }

    public abstract State transition (int c);

    public void reset () {
    }

    public boolean done () {
      return false;
    }

    public boolean eof () {
      return false;
    }

    public static boolean isSeparator (char c) {
      return c == '&' || c == '?' || c == '\r' || c == '\n';
    }

    public static boolean isEquals (char c) {
      return c == '=';
    }
  }

  private static class BetweenTokens extends State {
    private State nameState = null;
    private State noNameState = null;
    private boolean ready = false;
    private boolean eof = false;

    public void setTransState (State name) {
      nameState = name;
    }

    public void reset () {
      ready = false;
      current = new ParameterBuffer();
    }

    public boolean done () {
      return ready;
    }

    public boolean eof () {
      return eof;
    }

    public State transition (int i) {
      if (i == -1) {
        eof = true;
        ready = true;
        return this;
      }
      char c = (char) i;
      if (isSeparator(c)) {
        return this;
      }
      ready = true;
      current.name = new StringBuffer();
      nameState.setParameter(current);
      return nameState.transition(i);
    }
  }

  private static abstract class SeparatorDelimited extends State {
    private State endState = null;

    public void setEndState (State e) {
      endState = e;
    }

    public State transition (int i) {
      char c = (char) i;
      if (i == -1 || isSeparator(c)) {
        endState.setParameter(current);
        return endState.transition(i);
      }
      return characterCase(c);
    }

    protected abstract State characterCase (char c);
  }

  private static class InName extends SeparatorDelimited {
    private State valueState = null;

    public void setTransStates (State v, State e) {
      setEndState(e);
      valueState = v;
    }

    protected State characterCase (char c) {
      current.name.append(c);
      return this;
    }
  }

  private static class BeforeValue extends SeparatorDelimited {
    private State valueState = null;

    public void setTransStates (State v, State e) {
      setEndState(e);
      valueState = v;
    }

    protected State characterCase (char c) {
      valueState.setParameter(current);
      return valueState.transition((int) c);
    }
  }

  private static class InValue extends SeparatorDelimited {
    protected State characterCase (char c) {
      current.value.append(c);
      return this;
    }
  }

  private static class InToken extends InValue {
    protected State characterCase (char c) {
      if (current.value == null)
        current.value = new StringBuffer();
      return super.characterCase(c);
    }
  }

  private static class ParameterBuffer {
    public StringBuffer name = null;
    public StringBuffer value = null;

    public Parameter convert () {
      if (name == null && value == null)
        return null;
      String n = (name == null ? null : URLDecoder.decode(name.toString()));
      String v = (value == null ? null : URLDecoder.decode(value.toString()));
      return new Parameter(n, v);
    }
  }

  public static void main (String[] argv) {
    String input = "bla=&stuff=rare\r\n=no+dice+bucko&\nyarg&&\r\n";
    ParamReader pr1 = ParamReader.parsingReader(new StringReader(input));
    ParamReader pr2 = ParamReader.nonParsingReader(new StringReader(input));
    Parameter p;
    System.out.println("ParamReader::main:  reading parameters");
    try {
      while ((p = pr1.readParameter()) != null)
        System.out.println(p);

      System.out.println("ParamReader::main:  reading raw parameters");
      while ((p = pr2.readParameter()) != null)
        System.out.println(p);
    }
    catch (IOException ioe) {
      System.out.println(
        "ParamReader::main:  encountered unlikely IOException--" + ioe);
    }
    System.out.println("ParamReader::main:  Done.");
  }
}
