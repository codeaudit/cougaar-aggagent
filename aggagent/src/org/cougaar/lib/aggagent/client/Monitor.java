package org.cougaar.lib.aggagent.client;

import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimerTask;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.query.UpdateListener;
import org.cougaar.lib.aggagent.query.UpdateObservable;
import org.cougaar.lib.aggagent.util.Const;
import org.cougaar.lib.aggagent.util.XmlUtils;

  /**
   * Abstract base class for result set and alert monitors.  Provides support
   * for both periodic pull monitoring as well as keep alive server push
   * monitoring.
   *
   * Maintains a collection of monitored objects and keeps them updated based
   * on changes on the aggregation agent's blackboard.  To react to these
   * changes either:
   * <UL>
   * <LI>add update listener(s) to the monitor class and receive events for
   *     changes to all monitored objects or</LI>
   * <LI>add update listener(s) to 'live' objects returned by monitor and
   *     receive events only for those objects</LI>
   * </UL>
   */
  abstract class Monitor
  {
    /**
     * PULL_METHOD is an update method in which the client periodically pulls
     * incremental updates from passive session on aggregation agent.  A new
     * connection is created with each pull.
     */
    public static final int PULL_METHOD = 0;

    /**
     * KEEP_ALIVE_METHOD is an update method in which the client creates a keep
     * alive session with aggregation agent.  Incremental updates are pushed to
     * the client over this pipe.
     */
    public static final int KEEP_ALIVE_METHOD = 1;

    private int updateMethod;
    private boolean monitorAllObjects = false;
    private HashMap monitoredObjectMap = new HashMap();
    private String serverURL = null;
    private String monitorTag = null;
    private UpdateObservable updateObservable = new UpdateObservable();

    private String passiveSessionKey = null;
    private TimerTask pullTask = new TimerTask() {
        public synchronized void run()
        {
          if (passiveSessionKey != null)
          {
            String updateURL =
                serverURL + "&REQUEST_UPDATE=1&SESSION_ID=" + passiveSessionKey;
            Element root = XmlUtils.requestXML(updateURL, null);
            updateMonitoredObjects(root);
          }
        }
      };

    private volatile Thread keepAliveThread = null;
    private Runnable keepAliveTask = new Runnable() {
        public void run()
        {
          InputStream i = null;
          String monitorRequest = createMonitorRequest();
          if (monitorRequest == null)
            return;

          // set up keep alive connection
          try {
            URL url = new URL(serverURL + "&KEEP_ALIVE_MONITOR=1");
            URLConnection conn = url.openConnection();
            ((HttpURLConnection)conn).setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // send request
            PrintStream servicePrint = new PrintStream(conn.getOutputStream());
            servicePrint.println(monitorRequest);

            // get updates
            i = conn.getInputStream();
            Thread thisThread = Thread.currentThread();
            int formFeed = (int)'\f';
            while (keepAliveThread == thisThread)
            {
              StringBuffer updateMessage = new StringBuffer();
              int c;
              while (((c = i.read()) != formFeed) && (c != -1) &&
                     (keepAliveThread == thisThread)) {
                updateMessage.append((char)c);
              }

              if (c == 1)
              {
                throw new Exception();
              }

              if (c == formFeed)
              {
                // this reply does nothing.  I wish it worked so that the
                // server could figure out when I'm no longer listening.
                servicePrint.println("Got it.");
                servicePrint.flush();

                if (!updateMessage.toString().
                    equals(Const.KEEP_ALIVE_ACK_MESSAGE))
                {
                  Element root = XmlUtils.parse(updateMessage.toString());
                  updateMonitoredObjects(root);
                }
              }
            }

            // close input stream to shutdown keep alive psp connection.
            i.close();

            // this message does nothing.  See comment above.
            servicePrint.println("CANCEL SESSION");
            servicePrint.flush();
          }
          catch (Exception e) {
            System.out.println("Error reading from keep alive.\n" +
                               "Exiting monitor with request:\n" +
                               monitorRequest);

            // close input stream to shutdown keep alive psp connection.
            if (i != null)
              try {
                i.close();
              } catch (Exception e2) {/* I tried */}

            e.printStackTrace();
          }
        }
      };

    /**
     * Create a new monitor to monitor a set of objects on the aggregation
     * agent.  Each monitor is used to monitor a single type of object
     * (e.g. AlertMonitor, ResultSetMonitor).
     *
     * @param serverURL    aggregation agent cluster's text URL
     * @param monitorTag   magic text string used to tell aggregation PSP what
     *                     type of objects are being monitored.
     *                     (e.g. "alert", "result_set")
     * @param updateMethod method used to keep monitored objects updated
     *                     PULL_METHOD - periodically pull incremental updates
     *                                   from passive session on aggregation
     *                                   agent.  Create new connection with
     *                                   each pull.
     *                      KEEP_ALIVE_METHOD - create keep alive session
     *                                   with aggregation agent.  Incremental
     *                                   updates are pushed to the client
     *                                   over this pipe.
     */
    public Monitor(String serverURL, String monitorTag, int updateMethod)
    {
      this.serverURL = serverURL;
      this.monitorTag = monitorTag;
      this.updateMethod = updateMethod;
    }

    /**
     * Change mode to monitor all objects on the aggregation agent that are of
     * the type that this monitor handles.  Without calling this method, only
     * a defined set of objects are monitored (see monitorObject method).
     */
    public void monitorAllObjects()
    {
      monitorAllObjects = true;
      cancelUpdateSession();
      createUpdateSession();
    }

    /**
     * Add an update listener to observe all monitored objects.  This is
     * roughly equivalent to adding an update listener to each of the
     * currently monitored objects.  But, when monitor all objects is turned
     * on, this can also be used to discover newly added objects on the
     * aggregation agent's blackboard (via objectAdded listener call).
     *
     * @param ul update listener to add to entire monitor.
     */
    public void addUpdateListener(UpdateListener ul)
    {
      updateObservable.addUpdateListener(ul);
    }

    /**
     * Remove an update listener such that it no longer gets notified of
     * changes to monitored objects.
     *
     * @param ul update listener to remove from monitor.
     */
    public void removeUpdateListener(UpdateListener ul)
    {
      updateObservable.removeUpdateListener(ul);
    }

    /**
     * Returns a collection of all 'live' objects currently being updated by
     * this monitor.
     *
     * @return a collection of all 'live' objects currently being updated by
     *         this monitor.
     */
    public Collection getMonitoredObjects()
    {
      return monitoredObjectMap.values();
    }

    /**
     * Returns true if an object matching the given identifier is currently
     * being updated by this monitor.
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     *
     * @return true if an object matching the given identifier is currently
     *         being updated by this monitor.
     */
    public boolean isMonitoring(Object identifier)
    {
      return monitoredObjectMap.containsKey(identifier);
    }

    /**
     * Get the timer task to use to periodically pull incremental updates from
     * the aggregation agent.
     *
     * @return the timer task to use to periodically pull incremental updates
     * from the aggregation agent.  Returns null if monitor is not configured to
     * use the pull update method.
     */
    public TimerTask getPullTask()
    {
      return (updateMethod == PULL_METHOD) ? pullTask : null;
    }

    /**
     * Cancel this monitor and any overhead associated with it.
     *
     * @return true, if successful.
     */
    public boolean cancel()
    {
      if (updateMethod == PULL_METHOD)
      {
        boolean r = pullTask.cancel();
        cancelPassiveSession();
        return r;
      }
      else if (updateMethod == KEEP_ALIVE_METHOD)
      {
        keepAliveThread = null;
        return true;
      }
      return false;
    }

    /**
     * Must be defined by subclasses to provide a xml representation of a
     * given identifier.
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     *
     * @return a xml representation of given identifier.
     */
    protected abstract String createIdTag(Object identifier);

    /**
     * Must be defined by subclasses to define what should be done when an
     * update event (either add or change) is reported by the aggregation agent
     * to a object described by the given xml element tree.
     *
     * @param monitoredElement xml element tree that describes the updated
     *                         monitored object.
     *
     * @return a live object updated based on the given xml
     */
    protected abstract Object update(Element monitoredElement);

    /**
     * Must be defined by subclasses to define what should be done when a
     * remove event is reported by the aggregation agent to a object described
     * by the given xml element tree.
     *
     * @param monitoredElement xml element tree that describes the removed
     *                         monitored object.
     *
     * @return previously live object that was removed.
     */
    protected abstract Object remove(Element monitoredElement);

    /**
     * Monitor a new object.  If object matching identifier is already being
     * monitored, existing live object is returned.  Otherwise, passed in
     * object becomes live.
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     * @param monitoredObj a valid object for this type of monitor.
     *
     * @return a live object that is actively being updated to match a subject
     *         object on the aggregation agent.
     */
    protected Object monitorObject(Object identifier, Object monitoredObj)
    {
      Object existingMonitoredObj = getMonitoredObject(identifier);
      if (existingMonitoredObj == null)
      {
        if (!monitorAllObjects) cancelUpdateSession();
        monitoredObjectMap.put(identifier, monitoredObj);
        existingMonitoredObj = monitoredObj;
        if (!monitorAllObjects) createUpdateSession();
      }
      return existingMonitoredObj;
    }

    /**
     * Remove this object from the set of objects being monitored.  This
     * method has a negligible effect if monitor-all is turned on
     * (old live object will die, but new one will take it's place if that
     *  object is still on the log plan).
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     *
     * @return previously live object that was removed.
     */
    protected Object stopMonitoringObject(Object identifier)
    {
      if (!monitorAllObjects) cancelUpdateSession();
      Object removedObject = monitoredObjectMap.remove(identifier);
      if (!monitorAllObjects) createUpdateSession();
      return removedObject;
    }

    /**
     * Get a specific object being updated by this monitor.
     *
     * @param identifier   an object that uniquely identifies an object on the
     *                     aggregation agent.  Must be able to use this object
     *                     as a hashtable key (i.e. must have proper equals()
     *                     and hashcode() methods).
     *
     * @return a live object that is actively being updated to match a subject
     *         object on the aggregation agent.
     */
    protected Object getMonitoredObject(Object identifier)
    {
      return monitoredObjectMap.get(identifier);
    }

    private void cancelUpdateSession()
    {
      if (updateMethod == PULL_METHOD)
      {
        cancelPassiveSession();
      }
      else
      {
        keepAliveThread = null; // flag thread to exit
      }
    }

    private void createUpdateSession()
    {
      if (updateMethod == PULL_METHOD)
      {
        requestPassiveSession();
        pullTask.run();
      }
      else if (updateMethod == KEEP_ALIVE_METHOD)
      {
        keepAliveThread = new Thread(keepAliveTask);
        keepAliveThread.start();
      }
    }

    private String createMonitorRequest()
    {
      String monitorRequest = null;
      if (!monitoredObjectMap.isEmpty() || monitorAllObjects)
      {
        StringBuffer s = new StringBuffer("<monitor_session type=\"");
        s.append(monitorTag);
        s.append("\">\n");
        if (monitorAllObjects)
        {
          s.append("<monitor_all />\n");
        }
        else
        {
          for (Iterator i=monitoredObjectMap.keySet().iterator(); i.hasNext();)
          {
            s.append(createIdTag(i.next()));
          }
        }
        s.append("</monitor_session>");
        monitorRequest = s.toString();
      }
      return monitorRequest;
    }

    private String requestPassiveSession()
    {
      String passiveSessionRequest = createMonitorRequest();
      if (passiveSessionRequest == null)
        return null;

      // Request a passive session on Aggregation Agent
      String loadedURL = serverURL + "&CREATE_PASSIVE_SESSION=1";
      passiveSessionKey = XmlUtils.requestString(loadedURL,
                                                 passiveSessionRequest);
      return passiveSessionKey;
    }

    private String cancelPassiveSession()
    {
      String response = null;
      if (passiveSessionKey != null)
      {
        // Cancel passive session on Aggregation Agent
        String loadedURL =
            serverURL + "&CANCEL_PASSIVE_SESSION=1&SESSION_ID=" +
            passiveSessionKey;
        response = XmlUtils.requestString(loadedURL, null);
        passiveSessionKey = null;
      }
      return response;
    }

    private void updateMonitoredObjects(Element incrementalUpdate)
    {
      // update result set based on incremental change xml
      NodeList nl = incrementalUpdate.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++)
      {
        Node n = nl.item(i);
        if (n.getNodeType() == Node.ELEMENT_NODE)
        {
          Element child = (Element) n;
          String s = child.getNodeName();
          if (s.equals("added"))
          {
            addAll(child);
          }
          if (s.equals("changed"))
          {
            changeAll(child);
          }
          else if (s.equals("removed"))
          {
            removeAll(child);
          }
        }
      }
    }

    private void addAll(Element monitoredObjectsParent)
    {
      NodeList nl = monitoredObjectsParent.getElementsByTagName(monitorTag);
      for (int i = 0; i < nl.getLength(); i++)
      {
        Object updatedObject = update((Element)nl.item(i));
        updateObservable.fireObjectAdded(updatedObject);
      }
    }

    private void changeAll(Element monitoredObjectsParent)
    {
      NodeList nl = monitoredObjectsParent.getElementsByTagName(monitorTag);
      for (int i = 0; i < nl.getLength(); i++)
      {
        Object updatedObject = update((Element)nl.item(i));
        updateObservable.fireObjectChanged(updatedObject);
      }
    }

    private void removeAll(Element monitoredObjectsParent)
    {
      NodeList nl = monitoredObjectsParent.getElementsByTagName(monitorTag);
      for (int i = 0; i < nl.getLength(); i++)
      {
        Object removedObject = remove((Element)nl.item(i));
        updateObservable.fireObjectRemoved(removedObject);
      }
    }
  }

