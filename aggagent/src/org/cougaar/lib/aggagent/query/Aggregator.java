/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.lib.aggagent.query;

import java.io.Serializable;
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
public interface Aggregator extends Serializable {
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
