package org.cougaar.lib.aggagent.test;

public class NumberCycle {
  private int length = 0;
  private int value = 0;

  public NumberCycle (int l) {
    length = l;
  }

  public NumberCycle (int l, int v) {
    length = l;
    value = v;
  }

  public boolean increment () {
    return (++value) < length;
  }

  public String toString () {
    return "(" + value + "/" + length + ")";
  }

  public int getLength () {
    return length;
  }

  public int getValue () {
    return value;
  }

  public void setValue (int v) {
    value = v;
  }
}
