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
