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