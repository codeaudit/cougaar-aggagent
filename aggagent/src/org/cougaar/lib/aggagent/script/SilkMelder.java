
package org.cougaar.lib.aggagent.script;

import java.util.List;

import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.query.CompoundKey;
import org.cougaar.lib.aggagent.query.DataAtomMelder;

public class SilkMelder implements DataAtomMelder {
  private Procedure silkProcedure;

  public SilkMelder (String silkScript) {
    silkProcedure = (Procedure) SI.eval(silkScript);
  }

  public void meld (List idNames, CompoundKey id, List atoms, List output) {
    silkProcedure.apply(new Object[] {idNames, id, atoms, output});
  }
}
