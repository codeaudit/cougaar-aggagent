
package org.cougaar.lib.aggagent.session;

import org.cougaar.lib.aggagent.util.InverseSax;

/**
 *  This interface should be implemented by any object that can be transferred
 *  as an XML element within an UpdateDelta.  Nominally, only ResultSetDataAtom
 *  is used for communicating between society agents and aggregation agents,
 *  but other classes may use the same infrastructure for transfer to UI
 *  clients.
 */
public interface XmlTransferable {
  /**
   *  Convert this XmlTransferable Object to its XML representation and return
   *  the result as a String.
   */
  public String toXml ();

  /**
   *  Include the XML elements corresponding to this object as part of the
   *  provided document.
   */
  public void includeXml (InverseSax doc);
}