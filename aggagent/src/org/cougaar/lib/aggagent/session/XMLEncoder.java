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
package org.cougaar.lib.aggagent.session;

import java.util.Collection;

/**
 *  The XMLEncoder interface is used by the XmlIncrement (q.v.) interface to
 *  convert blackboard Objects into ResultSetDataAtoms for transfer from one
 *  point to another.  Zero or more atoms may be produced by an XMLEncoder for
 *  each Object passed to its encode method, but the XMLEncoder should not be
 *  concerned about where these atoms go after creation.
 */
public interface XMLEncoder
{
  /**
   *  Encode an Object as zero or more (but typically one) ResultSetDataAtoms.
   *  All such ResultSetDataAtoms should be added to the Collection provided by
   *  the caller.
   */
  public void encode (Object o, Collection c);
}