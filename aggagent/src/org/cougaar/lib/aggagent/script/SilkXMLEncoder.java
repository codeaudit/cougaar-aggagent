package org.cougaar.lib.aggagent.script;

import java.io.StringReader;
import java.util.Collection;
import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.session.XMLEncoder;

public class SilkXMLEncoder implements XMLEncoder
{
  private Procedure silkProcedure;

  public SilkXMLEncoder(String silkScript)
  {
    this.silkProcedure = (Procedure)SI.eval(silkScript);
  }

  public void encode(Object o, Collection out) {
    silkProcedure.apply(new Object[] {o, out});
  }
}
