/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
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
package org.cougaar.lib.aggagent.session;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
