
package org.cougaar.lib.aggagent.script;

import java.util.List;

import silk.Procedure;
import silk.SI;

import org.cougaar.lib.aggagent.query.Aggregator;
import org.cougaar.lib.aggagent.query.AggregationResultSet;

public class SilkAggregator implements Aggregator {
  private Procedure silkProcedure;

  public SilkAggregator (String silkScript) {
    silkProcedure = (Procedure) SI.eval(silkScript);
  }

  public void aggregate (AggregationResultSet rs, List output) {
    silkProcedure.apply(new Object[] {rs, output});
  }
}
