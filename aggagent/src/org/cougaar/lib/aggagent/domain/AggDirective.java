/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
package org.cougaar.lib.aggagent.domain;

import org.cougaar.planning.ldm.plan.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A special type of directive used by the aggregation function to hold its messages.
 */
public class AggDirective extends DirectiveImpl implements UniqueObject {

  protected AggDirective(XMLMessage mess) {
    setMessage(mess);
  }

  private UID uid;
  private org.cougaar.lib.aggagent.domain.XMLMessage message;

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    this.uid = uid;
  }
  public void setMessage(org.cougaar.lib.aggagent.domain.XMLMessage newMessage) {
    message = newMessage;
    this.setDestination(new ClusterIdentifier(message.getDestination().getAddress()));
    this.setSource(new ClusterIdentifier(message.getSource().getAddress()));
  }
  public org.cougaar.lib.aggagent.domain.XMLMessage getMessage() {
    return message;
  }

}