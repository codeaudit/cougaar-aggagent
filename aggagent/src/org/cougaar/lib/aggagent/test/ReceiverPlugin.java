
package org.cougaar.lib.aggagent.test;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/**
 *  This is an example demonstrating the use of the MessageTransportService.
 *  It registers itself to receive messages under the name "Target-Receiver"
 *  and reports any Messages as they arrive.
 */
public class ReceiverPlugin extends ComponentPlugin {
  protected static MessageAddress targetAddress =
    new ClusterIdentifier("Target-Receiver");

  protected MessageTransportService messenger = null;
  protected MessageTransportClient ear = new MessageEar();

  private class MessageServiceEar implements ServiceRevokedListener {
    public void serviceRevoked (ServiceRevokedEvent evt) {
      System.out.println("MessageTransportService Revoked.  Too bad.");
    }
  }

  private class MessageEar implements MessageTransportClient {
    // Important safety tip:  do not use '/' in the Message address.  The
    // NameSupport implementation uses it as a separator.  Oddly enough, it'll
    // still work, but stack traces will needlessly appear.  It is also
    // important not to use the name of an actual Cluster (in which case the
    // Cluster gets registered, and the MessageEar does not). Otherwise, the
    // choice of name appears to be arbitrary
    private MessageAddress address = getEarAddress();

    public void receiveMessage (Message m) {
      System.out.println("  *\n  *\nReceived Message:  " + m + "\n  *\n  *");
    }

    public MessageAddress getMessageAddress () {
      // return getBindingSite().getAgentIdentifier();
      return address;
    }
  }

  protected MessageAddress getEarAddress () {
    return targetAddress;
  }

  public void load() {
    super.load();

    // Traditionally, the call to ServiceBroker::getService uses "this" as its
    // first argument, but that only works if "this" implements the interface
    // MessageTransportClient.  Here, the inner class instance is proffered as
    // the requestor.  Note that this does not actually register the client to
    // receive event notifications (see below).
    messenger = (MessageTransportService) getServiceBroker().getService(
      ear, MessageTransportService.class, new MessageServiceEar());

    if (messenger != null)
      // One must explicitly register the MessageTransportClient; otherwise,
      // notification of messages will not be received (even though it was
      // passed in to the call to ServiceBroker::getService
      messenger.registerClient(ear);
    else
      System.out.println(
        "  X\n  X\nMessageTransportService not granted.  Too bad.\n  X\n  X");
  }

  public void unload() {
    super.unload();
    if (messenger != null)
      getServiceBroker().releaseService(
        ear, MessageTransportService.class, messenger);
  }

  public void setupSubscriptions () {
  }

  public void execute () {
  }
}
