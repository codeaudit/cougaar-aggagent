package org.cougaar.lib.aggagent.session;

import java.io.*;

/**
 *  IncrementFormat is the interface implemented by Objects that convert local
 *  subscription data for transportation to other agents (aggregation agents,
 *  generally).  Information obtained through the SubscriptionAccess interface
 *  can be translated in an arbitrary manner into an UpdateDelta, which is
 *  then shipped off as an XML document.
 *  <br><br>
 *  For many purposes, it suffices to use XmlIncrement (q.v.) with a suitable
 *  implementation of XMLEncoder, which is easier than creating a custom-built
 *  IncrementFormat from scratch.
 */
public interface IncrementFormat {
  /**
   *  Encode subscription data contained in the provided SubscriptionAccess as
   *  ResultSetDataAtoms and insert them as content.  into the provided
   *  UpdateDelta.  Please note that the implementation is responsible for
   *  calling setReplacement() with the appropriate value as part of this
   *  operation.
   */
  public void encode (UpdateDelta out, SubscriptionAccess sacc);
}
