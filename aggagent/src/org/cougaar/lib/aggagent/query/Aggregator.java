/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
