
package org.cougaar.lib.aggagent.test;

import java.io.*;
import java.util.*;

import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.KeepAlive;
import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.UISubscriber;

/**
 *  This keep-alive PSP generates lines of output at random intervals ad
 *  infinitum.  If the connection is closed by the client, then it will
 *  eventually desist.  The output lines are in the form of
 *  <br><br>
 *  This is message --#--
 *  <br><br>
 *  where "#" steps sequentially through the nonnegative integers.
 */
public class KeepAliveServer extends PSP_BaseAdapter
    implements PlanServiceProvider, KeepAlive
{
  /**
   *  Run a keep-alive session.  This method will continue to send lines of
   *  output to the provided PrintStream until an error is detected.
   */
  public void execute (PrintStream out, HttpInput in, PlanServiceContext psc,
      PlanServiceUtilities psu)
      throws Exception
  {
    System.out.println("KeepAliveServer::execute:  started");
    try {
      int counter = 0;
      while (!out.checkError()) {
        System.out.println("KeepAliveServer::execute:  top of while loop");
        long i = 1000l * randomNumber();
        System.out.println("  -> Sleeping for " + i);
        Thread.sleep(i);
        System.out.println("  -> Sending message #" + counter);

        out.println("This is message --" + counter++ + "--");
        out.flush();
      }
    }
    catch (Exception done_in) {
      System.out.println("KeepAliveServer::execute:  aborted!");
    }
    System.out.println("KeepAliveServer::execute:  leaving");
  }

  /**
   *  Generate a random number between 1 and 15 (inclusive) with lower numbers
   *  being more probable.
   */
  public long randomNumber () {
    double r = Math.random();
    return 1 + (long) Math.floor(15 * r * r * r * r);
  }

  /**
   *  We don't need no stinkin' ACK message.
   */
  public String getConnectionACKMessage () {
    return null;
  }

  /**
   *  And we don't want your stinkin' ACK message, either.
   */
  public void setConnectionACKMessage (String s) {
  }

  // More PSP methods that are basically useless, and are not implemented in
  // any meaningful sense.
  public String getDTD () { return null; }
  public boolean returnsHTML () { return false; }
  public boolean returnsXML () { return false; }
  public boolean test (HttpInput p0, PlanServiceContext p1) { return false; }
}
