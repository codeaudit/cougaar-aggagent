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
