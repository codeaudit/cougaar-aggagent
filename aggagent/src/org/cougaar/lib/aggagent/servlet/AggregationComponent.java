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
package org.cougaar.lib.aggagent.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.lib.aggagent.session.SessionManager;


/**
 *  The AggregationComponent provides external access (via HTTP) to the
 *  Aggregation functionality.  Currently, it supports both a HTML user
 *  interface with frames and a XML interface for thick clients.
 */
public class AggregationComponent extends BlackboardServletComponent
{
  private Object lock = new Object();
  private WhitePagesService wps = null;
  private AggregationServletInterface htmlInterface = null;
  private AggregationServletInterface xmlInterface = null;

  private UIDService UIDService;

  /**
   * Constructor.
   */
  public AggregationComponent()
  {
    super();
    myServlet = new AggregationServlet();
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  /** Getter for property UIDService.
   * @return Value of property UIDService.
   */
  public UIDService getUIDService() {
      return this.UIDService;
  }
  
  /** Setter for property UIDService.
   * @param UIDService New value of property UIDService.
   */
  public void setUIDService(UIDService UIDService) {
      this.UIDService = UIDService;
  }

  /**
   * When this class is created the "load()" method will
   * be called, at which time we'll register our Servlet.
   */
  public void load() {
    super.load();

    // create interface objects
    SessionManager man = new SessionManager(agentId.toString(), blackboard,
                                            createSubscriptionSupport());
    htmlInterface =
        new AggregationHTMLInterface(blackboard, createSubscriptionSupport(),
                                     myPath, getUIDService());
    xmlInterface =
      new AggregationXMLInterface(blackboard, createSubscriptionSupport(),
                                  agentId.toString(), wps, man, getUIDService());
  }

  /**
   * Here is our inner class that will handle all HTTP and
   * HTTPS service requests for our <tt>myPath</tt>.
   */
  private class AggregationServlet extends HttpServlet
  {
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        handleRequest(request, response);
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        handleRequest(request, response);
    }

    private void handleRequest(HttpServletRequest request,
                               HttpServletResponse response) throws IOException
    {
      PrintWriter out = response.getWriter();

      synchronized (lock)
      {
        if (request.getParameter("THICK_CLIENT") != null)
        {
          xmlInterface.handleRequest(out, request);
        }
        else
        {
          htmlInterface.handleRequest(out, request);
        }
        out.flush();
      }
    }
  }
}
