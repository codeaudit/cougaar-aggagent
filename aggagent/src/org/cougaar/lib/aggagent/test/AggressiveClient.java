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

package org.cougaar.lib.aggagent.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AggressiveClient implements Runnable {
  private static String DEFAULT_URL = "http://localhost:5555/$Target/test.psp";
  private static int CYCLES_PER_RUN = 250;
  private static int NUMBER_OF_THREADS = 20;

  private Object lock = new Object();
  private String targetUrl = null;
  private int requests = 0;
  private int responses = 0;
  private int badResponses = 0;
  private int errors = 0;
  private int calls = 0;
  private int activeThreads = 0;

  public AggressiveClient (String t) {
    targetUrl = t;
    if (targetUrl == null || targetUrl.length() == 0)
      targetUrl = DEFAULT_URL;
  }

  public AggressiveClient () {
    this(DEFAULT_URL);
  }

  public void start () {
    new Thread(this).start();
  }

  private void report () {
    report(null);
  }

  private void report (String response) {
    StringBuffer buf = new StringBuffer();
    buf.append(
      "calls = " + calls + "; requests = " + requests + "; responses = " +
      responses + " (bad = " + badResponses + "); errors = " + errors);
    if (response != null)
      buf.append("; got \"" + response + "\"");
    System.out.println(buf.toString());
  }

  private void updateActivityLog (String response) {
    synchronized (lock) {
      requests++;
      calls++;
      if (response != null) {
        responses++;
        if (!response.equals("Thanks."))
          badResponses++;
      }
      // report();
    }
  }

  private void updateFailureLog () {
    synchronized (lock) {
      errors++;
      calls++;
      // report();
    }
  }

  private void updateCallLog () {
    synchronized (lock) {
      calls++;
      // report();
    }
  }

  public void run () {
    try {
      URL url = new URL(targetUrl);
      synchronized (lock) {
        activeThreads++;
      }
      for (int i = 0; i < CYCLES_PER_RUN; i++) {
        try {
          URLConnection conn = url.openConnection();
          conn.setDoOutput(true);
          conn.setDoInput(true);

          // transmit the data
          OutputStream out = conn.getOutputStream();
          out.write(REQUEST_BYTES);
          out.close();

          String response = null;

          // read the response
          try {
            InputStream in = conn.getInputStream();
            StringBuffer buf = new StringBuffer();
            int b = -1;
            while ((b = in.read()) != -1)
              buf.append((char) b);

            in.close();
            response = buf.toString();
          }
          catch (Exception bogus) {
            bogus.printStackTrace();
          }

          updateActivityLog(response);
          continue;
        }
        catch (Exception failure) {
          updateFailureLog();
          continue;
        }
        catch (Error err) {
          updateCallLog();
          throw err;
        }
      }
      synchronized (lock) {
        activeThreads--;
        if (activeThreads == 0)
          report();
      }
    }
    catch (MalformedURLException mfe) {
    }
  }

  public static void main (String[] argv) {
    String arg = null;
    if (argv.length > 0)
      arg = argv[0];
    scenario_1(arg);
  }

  private static void scenario_2 (String u) {
    for (int i = 0; i < NUMBER_OF_THREADS; i++)
      new AggressiveClient(u).start();
  }

  private static void scenario_1 (String u) {
    AggressiveClient c = new AggressiveClient(u);
    for (int i = 0; i < NUMBER_OF_THREADS; i++)
      c.start();
  }

  private static String STANDARD_REQUEST =
    "<sentence>" +
      "<subject>" +
        "<article>the</article>" +
        "<noun>ants</noun>" +
        "<prep_phrase>" +
          "<prep>in</prep>" +
          "<object>" +
            "<proper_noun>France</proper_noun>" +
          "</object>" +
        "</prep_phrase>" +
      "</subject>" +
      "<predicate>" +
        "<verb>live</verb>" +
        "<adverb>mainly</adverb>" +
        "<prep_phrase>" +
          "<prep>on</prep>" +
          "<object>" +
            "<article>the</article>" +
            "<noun>plants</noun>" +
          "</object>" +
        "</prep_phrase>" +
      "</predicate>" +
    "</sentence>";
  private static byte[] REQUEST_BYTES = STANDARD_REQUEST.getBytes();
}
