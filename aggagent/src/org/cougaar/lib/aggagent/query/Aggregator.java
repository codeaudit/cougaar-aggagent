
package org.cougaar.lib.aggagent.query;

import java.util.Iterator;
import java.util.List;

/**
 *  Objects implementing this interface are capable of transforming an
 *  AggregationResultSet in arbitrary ways.  For most purposes, it is expected
 *  that the BatchAggregator class combined with a DataAtomMelder (q.v.)
 *  implementation will suffice, and will be easier to use.  However, for some
 *  of the more esoteric combinations, this class may be used to exercise
 *  complete control over the calculations.
 */
public interface Aggregator {
  /**
   *  Derive a new set of ResultSetDataAtoms from those contained in the
   *  specified AggregationResultSet.
   *
   *  @param atomIterator an iterator that iterates through raw, unaggregated
   *    data atoms
   *  @param output a List into which the produced ResultSetDataAtoms should be
   *    placed
   */
  public void aggregate (Iterator atomIterator, List output);
}
