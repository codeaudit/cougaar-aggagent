
package org.cougaar.lib.aggagent.script;

import java.util.Iterator;
import java.util.List;

import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.query.Aggregator;

public class SilkAggregator implements Aggregator {
  private Procedure silkProcedure;

  public SilkAggregator (String silkScript) {
    silkProcedure = (Procedure) SI.eval(silkScript);
  }

  public void aggregate (Iterator dataAtoms, List output) {
    silkProcedure.apply(new Object[] {dataAtoms, output});
  }
}
