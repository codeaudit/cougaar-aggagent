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
package org.cougaar.lib.aggagent.query;

import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.script.PythAlert;
import org.cougaar.lib.aggagent.script.SilkAlert;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.aggagent.util.InverseSax;

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
   *
   * @param sourceObject object that has been added
   */
  public void fireObjectAdded()
  {
    updateObservable.fireObjectAdded(this);
  }

  /**
   * Send event to all update listeners indicating that object has been removed
   * from the log plan.
   *
   * @param sourceObject object that has been removed
   */
  public void fireObjectRemoved()
  {
    updateObservable.fireObjectRemoved(this);
  }

  /**
   * Send event to all update listeners indicating that object has been changed
   * on the log plan.
   *
   * @param sourceObject object that has been changed
   */
  private void fireObjectChanged()
  {
    updateObservable.fireObjectChanged(this);
  }
}
