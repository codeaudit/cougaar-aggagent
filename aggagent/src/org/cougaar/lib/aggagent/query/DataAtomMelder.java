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
import java.util.List;

/**
 *  Objects implementing this interface embody the nucleus of aggregation
 *  functionality.  That is, they combine multiple data atoms based on the
 *  embodied aggregation rule.  Typically, the result is a single atom, but
 *  the API allows zero or multiple atoms to accomodate a variety of needs.
 */
public interface DataAtomMelder extends Serializable {
  /**
   *  Combine the data atoms in the list provided according to the implemented
   *  aggregation scheme.  Atoms produced in this manner should be appended to
   *  the output list.  Any other modification of the list should be avoided.
   *  The list of idNames and the CompoundKey refer to the aggregation.  If
   *  multiple aggregated atoms are going to be produced, then additional id
   *  keys must be used to distinguish them (otherwise, only one will be kept
   *  in the resultant data set.
   *
   *  @param idNames a list of names, in order of appearance, of the id values
   *    found in the id key
   *  @param id the compound id key values associated with the aggregation
   *  @param atoms a List of ResultSetDataAtoms that should be combined
   *  @param output a List into which the produced ResultSetDataAtoms should be
   *    placed
   */
  public void meld (List idNames, CompoundKey id, List atoms, List output);
}
