package org.cougaar.lib.aggagent.session;

import java.io.PrintStream;

public interface XMLEncoder
{
  public void encode (Object o, PrintStream ps);
}