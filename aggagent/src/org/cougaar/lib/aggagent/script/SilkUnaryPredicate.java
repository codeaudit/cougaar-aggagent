package org.cougaar.lib.aggagent.script;

import java.io.StringReader;
import org.cougaar.util.UnaryPredicate;
import silk.Procedure;
import silk.SI;

public class SilkUnaryPredicate implements UnaryPredicate {

  private Procedure silkProcedure;

  public SilkUnaryPredicate(String silkScript)
  {
    this.silkProcedure = (Procedure)SI.eval(silkScript);
  }

  public boolean execute(Object o)
  {
    return ((Boolean) silkProcedure.apply(new Object[] {o})).booleanValue();
  }
}
