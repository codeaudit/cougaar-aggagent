/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
package org.cougaar.lib.aggagent.plugin;

import java.util.Collections;
import java.util.Set;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * This is an example of a simple relay object.
 */
public class AggRelay
implements Relay.Source, Relay.Target
{
  private MessageAddress source;
  private MessageAddress target;
  private UID uid;

  private XMLMessage content;
  private XMLMessage response;
  

  private transient Set _targets;

  /** Holds value of property local. */
  private boolean local = false;
  
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
      AggRelay ar =  new AggRelay(
          uid, source, null, (XMLMessage)content, null);
      ar.setLocal(false);
      return ar;
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
  
  /** Getter for property local.
   * @return Value of property local.
   */
  public boolean isLocal() {
      return local;
  }
  
  /** Setter for property local.
   * @param local New value of property local.
   */
  public void setLocal(boolean local) {
      this.local = local;
  }
  
}
