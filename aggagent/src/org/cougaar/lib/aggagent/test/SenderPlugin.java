
package org.cougaar.lib.aggagent.test;

import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;

/**
 *  This is an example demonstrating the use of the MessageTransportService.
 *  It complements the ReceiverPlugin by sending messages to the address under
 *  which the ReceiverPlugin registers itself.
 *  <br><br>
 *  Incidentally, this class also demonstrates the usage of the AlarmService
 *  interface, which is registered in superclass ComponentPlugin.  Note the use
 *  of an Alarm implementation.
 */
public class SenderPlugin extends ReceiverPlugin {
  protected static MessageAddress senderAddress =
    new ClusterIdentifier("Source-Receiver");

  private class SendAfterDelay implements Alarm {
    private long detonate = -1;
    private boolean expired = false;

    public SendAfterDelay () {
      detonate = 3000l + System.currentTimeMillis();
    }

    public long getExpirationTime () {
      return detonate;
    }

    public void expire () {
      if (!expired)
        sendMessage();
      expired = true;
    }

    public boolean hasExpired () {
      return expired;
    }

    public boolean cancel () {
      if (!expired)
        return expired = true;
      return false;
    }
  }

  protected MessageAddress getEarAddress () {
    return senderAddress;
  }

  private void delayedSend () {
    alarmService.addRealTimeAlarm(new SendAfterDelay());
  }

  private static class SimpleMessage extends Message {
    public SimpleMessage (MessageAddress self, MessageAddress other) {
      super(self, other);
    }
  }

  private MessageAddress getReceiverIdentifier () {
    return targetAddress;
  }

  private void sendMessage () {
    System.out.println("SenderPlugin::sendMessage ...");
    messenger.sendMessage(new SimpleMessage(
      getBindingSite().getAgentIdentifier(), getReceiverIdentifier()));
    System.out.println("SenderPlugin::sendMessage:  done");
    delayedSend();
  }

  protected void setupSubscriptions () {
    delayedSend();
  }
}