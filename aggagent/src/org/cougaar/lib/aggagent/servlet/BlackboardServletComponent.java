/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
package org.cougaar.lib.aggagent.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.*;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import org.cougaar.core.servlet.*;
import org.cougaar.lib.aggagent.session.SessionManager;
import org.cougaar.util.UnaryPredicate;

/**
 *  The Blackboard Servlet Component provides services required for both
 *  Servlet and full blackboard service access.
 */
public abstract class BlackboardServletComponent extends ComponentPlugin
{
  /** correspondence between Servlets and subscriptions */
  private Hashtable subscribers = new Hashtable();

  /**
   * This is the path for my Servlet, relative to the
   * Agent's URLEncoded name.
   */
  protected String myPath = null;

  protected Servlet myServlet = null;

  /**
   * Save our service broker during initialization.
   */
  private ServiceBroker serviceBroker = null;

  /**
   * To launch our Servlet we will use the
   * "<code>ServletService</code>" Servlet Registration Service,
   * which is obtained from the <tt>serviceBroker</tt>.
   * <p>
   * This is used during "load()" and "unload()".
   */
  private ServletService servletService = null;

  /**
   * Needed since this servlet needs to be able to acquire a list of all
   * available agents.
   */
  protected NamingService naming = null;

  /** Holds value of property loggingService. */
  protected LoggingService log;
  
  /**
   * Constructor.
   */
  public BlackboardServletComponent()
  {
    super();
  }

  /**
    * Subscriptions are generally created dynamically based on servlet requests
    * in the aggregator code-base.
    */
  public void setupSubscriptions() {}

  /**
   * Used to inform subscription listeners (which were dynamically created in
   * response to servlet requests) that their subscription changed.
   */
  public void execute()
  {
    synchronized (subscribers) {
      Enumeration subscriptions = subscribers.keys();
      while (subscriptions.hasMoreElements()) {
        Subscription subscription = (Subscription)subscriptions.nextElement();
        if (subscription.hasChanged()) {
          SubscriptionListener subscriptionListener =
            (SubscriptionListener)subscribers.get(subscription);
          // remove a listener if it no longer exists
          if (subscriptionListener == null) {
            subscribers.remove(subscription);
          } else {
            subscriptionListener.subscriptionChanged(subscription);
          }
        }
      }
    }
  }

  /**
   * Subscription monitoring support for use by servlets
   */
  protected SubscriptionMonitorSupport createSubscriptionSupport()
  {
    SubscriptionMonitorSupport sms = new SubscriptionMonitorSupport() {
        public void setSubscriptionListener(
          Subscription subscription, SubscriptionListener subscriptionListener)
        {
          synchronized(subscribers)
          {
            subscribers.put(subscription, subscriptionListener);
          }
        }

        public void removeSubscriptionListener(Subscription subscription)
        {
          synchronized(subscribers)
          {
            subscribers.remove(subscription);
          }
        }
      };
    return sms;
  }

  /**
   * Capture servlet path here.
   */
  public void setParameter(Object o)
  {
    // expecting a List of [String]
    if (!(o instanceof List)) {
      throw new IllegalArgumentException(
        "Expecting a List parameter, not : "+
        ((o != null) ? o.getClass().getName() : "null"));
    }
    List l = (List)o;
    if (l.size() != 1) {
      throw new IllegalArgumentException(
          "Expecting a List with one element,"+
          " \"path\", not " + l.size());
    }
    Object o1 = l.get(0);
    if (!(o1 instanceof String)) {
      throw new IllegalArgumentException(
          "Expecting one String, not ("+o1+")");
    }

    // save the servlet path
    this.myPath = (String) o1;
  }

  /**
   * Save our ServiceBroker during initialization.
   * <p>
   * This method is called when this class is created.
   */
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    this.serviceBroker = bs.getServiceBroker();
  }

  /**
   * When this class is created the "load()" method will
   * be called, at which time we'll register our Servlet.
   */
  public void load() {
    super.load();

    // get the servlet service
    servletService = (ServletService)
      serviceBroker.getService(
                    this,
                    ServletService.class,
                    null);
    if (servletService == null) {
      throw new RuntimeException(
          "Unable to obtain ServletService");
    }

    // register our servlet
    try {
      servletService.register(myPath, myServlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to register servlet with path \""+
          myPath+"\": "+e.getMessage());
    }

    // get the naming service (for "listAgentNames")
    naming = (NamingService)
      serviceBroker.getService(
          this,
          NamingService.class,
          null);
    if (naming == null) {
      throw new RuntimeException(
          "Unable to obtain naming service");
    }
  }

  /**
   * When our class is unloaded we must release our service.
   * <p>
   * This will automatically unregister our Servlet.
   */
  public void unload() {
    // release naming service
    if (naming != null) {
      serviceBroker.releaseService(
          this, NamingService.class, naming);
    }

    // release our servlet service
    if (servletService != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
    }
    super.unload();
  }

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {
    return getClass().getName()+"("+myPath+")";
  }
  
  /** Getter for property loggingService.
   * @return Value of property loggingService.
   */
  public LoggingService getLoggingService() {
    return this.log;
  }
  
  /** Setter for property loggingService.
   * @param loggingService New value of property loggingService.
   */
  public void setLoggingService(LoggingService loggingService) {
    this.log = loggingService;
  }
  
}
