/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.plugin;

import java.util.Set;
import java.util.Collections;

import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.core.relay.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * This is an example of a simple relay object.
 */
public class AggRelay
implements Relay.Source, Relay.Target, XMLizable
{
  private MessageAddress source;
  private MessageAddress target;
  private UID uid;

  private XMLMessage content;
  private XMLMessage response;

  private transient Set _targets;

  /**
   * @param content initial content
   * @param response initial response
   */
  public AggRelay(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      XMLMessage content,
      XMLMessage response) {
    this.uid = uid;
    this.source = source;
    this.target = target;

    this.content = content;
    this.response = response;

    this._targets = 
     ((target != null) ?
      Collections.singleton(target) :
      Collections.EMPTY_SET);
      
  }

  // UniqueObject interface

  public void setUID(UID uid) {
    throw new RuntimeException("Attempt to change UID");
  }

  public UID getUID() {
    return uid;
  }

  // Source interface

  public Set getTargets() {
    return _targets;
  }

  public Object getContent() {
    return content;
  }


  private static final class SimpleRelayFactory
  implements TargetFactory, java.io.Serializable {

    public static final SimpleRelayFactory INSTANCE = 
      new SimpleRelayFactory();

    private SimpleRelayFactory() {}

    public Relay.Target create(
        UID uid, 
        MessageAddress source, 
        Object content,
        Token token) {
      return new AggRelay(
          uid, source, null, (XMLMessage)content, null);
    }

    private Object readResolve() {
      return INSTANCE;
    }
  };

  
  public TargetFactory getTargetFactory() {
    return SimpleRelayFactory.INSTANCE;
  }

  public int updateResponse(MessageAddress addr, Object o) {
      if (!(o instanceof XMLMessage)) 
          throw new RuntimeException("AggRelay: Response is not an XMLMessage");
      return updateResponse(addr, (XMLMessage)o);
  }
  
  public int updateResponse(
      MessageAddress t, XMLMessage response) {
    // assert response != null
    if (!(response.equals(this.response))) {
      this.response = response;
      return Relay.RESPONSE_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // Target interface

  public MessageAddress getSource() {
    return source;
  }

  public Object getResponse() {
    return response;
  }

  public int updateContent(Object o, Token token) {
      if (!(o instanceof XMLMessage)) 
          throw new RuntimeException("AggRelay: Content is not an XMLMessage");
      return updateContent((XMLMessage)o, token);
  }

  public int updateContent(XMLMessage content, Token token) {
    // assert content != null
    if (!(content.equals(this.content))) {
      this.content = content;
      return Relay.CONTENT_CHANGE;
    }
    return Relay.NO_CHANGE;
  }

  // XMLizable method for UI, other clients
  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof AggRelay)) {
      return false;
    } else {
      UID u = ((AggRelay) o).getUID();
      return uid.equals(u);
    }
  }

  public int hashCode() {
    return uid.hashCode();
  }

  public String toString() {
    return "("+uid+", "+content+", "+response+")";
  }
  
}
