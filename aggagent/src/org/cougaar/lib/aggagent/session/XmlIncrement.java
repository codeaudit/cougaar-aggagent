package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.lib.aggagent.session.UpdateDelta;

/**
 *  XmlIncrement is an IncrementFormat implementation that uses the XMLEncoder
 *  (q.v.) interface to encode individual objects found on the blackboard.
 *  Every UpdateDelta formatted by the encode method of this class will be in
 *  increment mode (i.e., with "added", "changed", and "removed" lists).
 */
public class XmlIncrement implements IncrementFormat {
  private XMLEncoder xmlEncoder = null;

  /**
   *  Create a new XmlIncrement.  The provided XMLEncoder implementation sets
   *  the behavior for this instance.
   */
  public XmlIncrement(XMLEncoder coder) {
    if (coder == null)
      throw new IllegalArgumentException("cannot accept a null XMLEncoder");

    xmlEncoder = coder;
  }

  /**
   *  Encode the latest subscription information as an UpdateDelta.  The
   *  Objects found in the "added", "changed", and "removed" lists of the
   *  SubscriptionAccess are each converted to ResultSetDataAtoms, which are
   *  then added to the appropriate list in the UpdateDelta.
   */
  public void encode (UpdateDelta out, SubscriptionAccess sacc) {
    out.setReplacement(false);
    sendBunch(sacc.getAddedCollection(), out.getAddedList());
    sendBunch(sacc.getChangedCollection(), out.getChangedList());
    sendBunch(sacc.getRemovedCollection(), out.getRemovedList());
  }

  private void sendBunch (Collection c, List out) {
    for (Iterator i = c.iterator(); i.hasNext(); )
      xmlEncoder.encode(i.next(), out);
  }
}
