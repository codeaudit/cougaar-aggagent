
package org.cougaar.lib.aggagent.test;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.PlanServiceUtilities;

import org.cougaar.lib.aggagent.util.AdvancedHttpInput;

public class OverloadedPSP
    extends PSP_BaseAdapter
    implements PlanServiceProvider
{
  private Object lock = new Object();
  private int requests = 0;

  private void updateActivityLog () {
    synchronized (lock) {
      requests++;
    }
  }

  public void execute (PrintStream out, HttpInput in, PlanServiceContext psc,
      PlanServiceUtilities psu)
  {
    AdvancedHttpInput ahi = new AdvancedHttpInput(in);
    if (ahi.hasKeyword("SUMMARY")) {
      out.println("<html><head></head><body>");
      out.println("OverloadedPSP Summary: ");
      out.println("<ul><li>requests = " + requests + "</li></ul>");
      out.println("</body></html>");
    }
    else {
      // record request
      updateActivityLog();

      // parse the request as XML
      parsePost(in);

      // acknowledge receipt
      out.print("Thanks.");
    }

    out.flush();
  }

  private Element parsePost (HttpInput in) {
    try {
      CharArrayReader car = new CharArrayReader(in.getBodyAsCharArray());
      DOMParser p = new DOMParser();
      p.parse(new InputSource(car));
      return p.getDocument().getDocumentElement();
    }
    catch (Exception eek) {
      eek.printStackTrace();
    }

    return null;
  }

  public String getDTD () { return null; }
  public boolean returnsHTML () { return false; }
  public boolean returnsXML () { return false; }
  public boolean test (HttpInput p0, PlanServiceContext p1) { return false; }
}