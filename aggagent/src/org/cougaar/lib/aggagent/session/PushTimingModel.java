
package org.cougaar.lib.aggagent.session;

import java.util.Timer;
import java.util.TimerTask;

/**
 *  The PushTimingModel class implements the timing model used by PushSessions
 *  to regulate the number of connections they make to report new developments
 *  to a client.  The current implementation guarantees that the associated
 *  PushSession spends at least a specified period of time idle between
 *  submitting consecutive updates.  Of course, when no new information is
 *  available, no update is sent.
 *  <br><br>
 *  The effect is accomplished through the use of a Timer and an inner class
 *  that subclasses TimerTask.
 *  <br><br>
 *  Possible development in the future may call for different timing models.
 *  In that event, the generic interface of this class should be factored out
 *  and each specific subtype of SessionManager can generate appropriate
 *  implementations of PushTimingModel.
 */
public class PushTimingModel {
  private static int IDLE = 0;
  private static int WAITING = 1;
  private static int RUNNING = 2;
  private static int AGAIN = 3;
  private static int DEAD = 4;

  private int state = IDLE;

  private Object lock = new Object();

  private Timer scheduler = null;
  private TimerTask task = null;
  private PushSession session = null;
  private long execInterval = 0;
  private long nextExec = System.currentTimeMillis();

  /**
   *  Create a new PushTimingModel.  By default, the execution interval is
   *  zero indicating that no delay is enforced in posting updates to the
   *  client.
   */
  public PushTimingModel (Timer t, PushSession s) {
    scheduler = t;
    session = s;
  }

  /**
   *  Create a new PushTimingModel.  The specified interval (in milliseconds)
   *  is guaranteed to elapse between updates posted to the client.
   */
  public PushTimingModel (Timer t, PushSession s, long n) {
    this(t, s);
    execInterval = n;
  }

  /**
   *  Schedule the PushSession to send an update in accordance with this timing
   *  model.
   */
  public void schedule () {
    synchronized (lock) {
      if (state == IDLE) {
        state = WAITING;
        task = new PushTask();
        scheduler.schedule(
          task, Math.max(0, nextExec - System.currentTimeMillis()));
      }
      else if (state == RUNNING) {
        state = AGAIN;
      }
    }
  }

  /**
   *  Change the minimum interval between updates for this timing model.  This
   *  method is specific to this model and should not be considered part of the
   *  generic interface.
   */
  public void setExecInterval (long newInterval) {
    if (newInterval < 0)
      throw new IllegalArgumentException("negative intervals are not allowed");
    if (newInterval == execInterval)
      return;

    synchronized (lock) {
      if (state == IDLE) {
        nextExec += newInterval - execInterval;
        execInterval = newInterval;
      }
      else if (state == WAITING) {
        task.cancel();
        state = IDLE;
        nextExec += newInterval - execInterval;
        schedule();
      }
      else if (state == RUNNING || state == AGAIN) {
        execInterval = newInterval;
      }
    }
  }

  /**
   *  Cancel this PushTimingModel.  After this method is called, the instance
   *  cancels any scheduled behavior and becomes useless.
   */
  public void cancel () {
    synchronized (lock) {
      state = DEAD;
    }
    if (task != null)
      task.cancel();
  }

  // A TimerTask subclass that allows scheduling via a Timer
  private class PushTask extends TimerTask {
    public void run () {
      synchronized (lock) {
        state = RUNNING;
      }

      session.pushUpdate();

      synchronized (lock) {
        task = null;
        nextExec = System.currentTimeMillis() + execInterval;
        if (state == RUNNING) {
          state = IDLE;
        }
        else if (state == AGAIN) {
          state = IDLE;
          schedule();
        }
      }
    }
  }
}