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

package org.cougaar.lib.aggagent.query;

import java.io.Serializable;

import org.cougaar.lib.aggagent.session.XmlTransferable;
import org.cougaar.lib.aggagent.util.InverseSax;

/**
 *  The Alert class is the abstract superclass of all result set monitors.
 *  Each implementation is responsible for maintaining its own status regarding
 *  the corresponding result set.  At first, this will be a simple boolean
 *  status:  either the alert is tripped or not.  However, more complicated
 *  recording apparati may be supported (or even required) in the future.
 *  <br><br>
 *  When new information becomes available, the handleUpdate method should be
 *  called.  Specific implementations will examine the result set and take the
 *  appropriate action, which may include updating the Alert's status and/or
 *  producing events.
 *  <br><br>
 *  At present there is no additional support for Alert activities, so all
 *  necessary work must be done in the Alert implementations.
 */
public abstract class Alert implements XmlTransferable, Serializable {
  public static String ALERT_TAG = "alert";
  private static String NAME_ATT = "name";
  private static String QUERY_ATT = "query_id";
  private static String ALERTED_ATT = "alerted";

  private boolean alerted = false;
  private QueryResultAdapter query = null;
  private String name = null;

  public void setName (String n) {
    name = n;
  }

  public String getName () {
    return name;
  }

  /**
   *  Specify the query (etc.) which the Alert is responsible for monitoring.
   *  In future implementations, this coupling may be handled differently.  In
   *  particular, different subclasses of AggregationResultSet may be supported
   *  with class-specific handlers.  For now, only one type is available, and
   *  hence only one type is expected.
   */
  public void setQueryAdapter (QueryResultAdapter s) {
    query = s;
  }

  /**
   *  Provide access to the QueryResultAdapter monitored by this Alert.
   */
  public QueryResultAdapter getQueryAdapter () {
    return query;
  }

  /**
   *  Provide access to the query ID associated with this alert.  The default
   *  implementation is to obtain this from the resident query, but subclasses
   *  may have different behavior.
   */
  public String getQueryId () {
    return query.getID();
  }

  /**
   *  Set the alerted status of this Alert.  This method is declared
   *  <it>public</it> rather than <it>protected</it> so that scripts can
   *  access it.  In general, however, it should not be called by other
   *  agencies.
   */
  public void setAlerted (boolean f) {
    alerted = f;
  }

  /**
   *  Report the status of this Alert.  Either the alert has been tripped
   *  (return true) or it hasn't (return false).
   */
  public boolean isAlerted () {
    return alerted;
  }

  /**
   *  <p>
   *  Notify the Alert that the relevant result set has been updated.  This
   *  method returns true if the state of the Alert was changed during the
   *  operation and false otherwise.  The bulk of the responsibility is
   *  delegated to the abstract handleUpdate() method.  Concrete Alert classes
   *  should provide the appropriate implementation for that method.
   *  </p><p>
   *  The update() method is intentionally not declared <it>final</it> so that
   *  subclasses can use a different strategy for reporting changes, possibly
   *  relating to class-specific state.
   *  </p>
   */
  public boolean update () {
    boolean oldAlerted = isAlerted();
    handleUpdate();
    return oldAlerted != isAlerted();
  }

  /**
   *  Convert this Alert to an XML format for transfer to clients.
   */
  public String toXml () {
    InverseSax doc = new InverseSax();
    includeXml(doc);
    return doc.toString();
  }

  public void includeXml (InverseSax doc) {
    doc.addElement(ALERT_TAG);
    doc.addAttribute(NAME_ATT, getName());
    doc.addAttribute(QUERY_ATT, getQueryId());
    doc.addAttribute(ALERTED_ATT, String.valueOf(isAlerted()));
    includeXmlBody(doc);
    doc.endElement();
  }

  protected void includeXmlBody (InverseSax doc) {
  }

  /**
   *  Handle incoming data and adjust the local state.  This method is declared
   *  public so that scripts can access it.  Otherwise, it is usually invoked
   *  through the update() method.
   */
  public abstract void handleUpdate ();
}
