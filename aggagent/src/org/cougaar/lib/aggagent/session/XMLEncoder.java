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