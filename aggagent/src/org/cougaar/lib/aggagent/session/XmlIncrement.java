package org.cougaar.lib.aggagent.session;

import java.io.*;
import java.util.*;

import org.cougaar.lib.aggagent.plugin.Const;
import org.cougaar.lib.aggagent.query.UpdateDelta;

public class XmlIncrement implements IncrementFormat {
  private XMLEncoder xmlEncoder = null;

  public XmlIncrement(XMLEncoder coder) {
    if (coder == null)
      throw new IllegalArgumentException("cannot accept a null XMLEncoder");

    xmlEncoder = coder;
  }

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
