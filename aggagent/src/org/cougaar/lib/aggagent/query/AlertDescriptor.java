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

import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.Enum.Language;
import org.cougaar.lib.aggagent.util.Enum.ScriptType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This is a client-side description of an alert.
 */
public class AlertDescriptor extends Alert
{
  private UpdateObservable updateObservable = new UpdateObservable();
  private String queryId = null;

  private ScriptSpec alertSpec = null;

  /**
   * Default constructor
   */
  public AlertDescriptor()
  {
    super();
  }

  public AlertDescriptor(Alert a)
  {
    super();
    setName(a.getName());
    setAlerted(a.isAlerted());
    setQueryId(a.getQueryAdapter().getID());
  }

  public AlertDescriptor(Element alertRoot)
  {
    super();
    setName(alertRoot.getAttribute("name"));
    setAlerted(
      Boolean.valueOf(alertRoot.getAttribute("alerted")).booleanValue());
    setQueryId(alertRoot.getAttribute("query_id"));
    NodeList nl = alertRoot.getElementsByTagName("alert_script");
    if (nl.getLength() > 0)
      alertSpec = new ScriptSpec((Element) nl.item(0));
  }

  public AlertDescriptor (Language language, String script) {
    alertSpec = new ScriptSpec(ScriptType.ALERT, language, script);
  }

  public void setQueryAdapter(QueryResultAdapter qra)
  {
    super.setQueryAdapter(qra);
    queryId = qra.getID();
  }

  public void setQueryId(String queryId)
  {
    this.queryId = queryId;
  }

  public String getQueryId()
  {
    return queryId;
  }

  public void setAlerted (boolean f)
  {
    super.setAlerted(f);
    fireObjectChanged();
  }

  /**
   * Create a functional alert based on this descriptor
   *
   * @return a functional alert based on this descriptor
   */
  public Alert createAlert() throws Exception
  {
    Alert a = alertSpec.toAlert();
    a.setName(getName());

    return a;
  }

  public String getScript () {
    if (alertSpec != null)
      return alertSpec.getText();
    else
      return "";
  }

  public void handleUpdate()
  {
    // never actually used as a functional alert
  }

  protected void includeXmlBody (InverseSax doc) {
    if (alertSpec != null)
      alertSpec.includeXml(doc);
  }

  /**
   * Add an update listener to observe this object
   */
  public void addUpdateListener(UpdateListener ul)
  {
    updateObservable.addUpdateListener(ul);
  }

  /**
   * Remove an update listener such that it no longer gets notified of changes
   * to this object
   */
  public void removeUpdateListener(UpdateListener ul)
  {
    updateObservable.removeUpdateListener(ul);
  }

  /**
   * Send event to all update listeners indicating that object has been added
   * to the log plan.
   */
  public void fireObjectAdded()
  {
    updateObservable.fireObjectAdded(this);
  }

  /**
   * Send event to all update listeners indicating that object has been removed
   * from the log plan.
   */
  public void fireObjectRemoved()
  {
    updateObservable.fireObjectRemoved(this);
  }

  /**
   * Send event to all update listeners indicating that object has been changed
   * on the log plan.
   */
  private void fireObjectChanged()
  {
    updateObservable.fireObjectChanged(this);
  }
}
