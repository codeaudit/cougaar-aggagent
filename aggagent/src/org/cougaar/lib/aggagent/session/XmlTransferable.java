
package org.cougaar.lib.aggagent.session;

/**
 *  This interface should be implemented by any object that can be transferred
 *  as an XML element within an UpdateDelta.  Nominally, only ResultSetDataAtom
 *  is used for communicating between society agents and aggregation agents,
 *  but other classes may use the same infrastructure for transfer to UI
 *  clients.
 */
public interface XmlTransferable {
  public String toXml ();
}