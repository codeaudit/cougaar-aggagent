package org.cougaar.lib.aggagent.util;

public class Parameter {
  public String name = null;
  public String value = null;

  public Parameter (String n, String v) {
    name = n;
    value = v;
  }

  public String toString () {
    StringBuffer buf = new StringBuffer();
    if (name == null) {
      buf.append("[no name]");
    }
    else {
      buf.append("<");
      buf.append(name);
      buf.append(">");
    }
    buf.append(" = ");
    if (value == null) {
      buf.append("[no value]");
    }
    else {
      buf.append("<");
      buf.append(value);
      buf.append(">");
    }
    return buf.toString();
  }
}
