
package org.cougaar.lib.aggagent.query;

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
public abstract class Alert {
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
   *  particular, different subclasses of AssessmentResultSet may be supported
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
   *  Handle incoming data and adjust the local state.  This method is declared
   *  public so that scripts can access it.  Otherwise, it is usually invoked
   *  through the update() method.
   */
  public abstract void handleUpdate ();
}