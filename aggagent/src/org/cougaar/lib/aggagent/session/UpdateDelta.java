
package org.cougaar.lib.aggagent.session;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.query.CompoundKey;
import org.cougaar.lib.aggagent.query.ResultSetDataAtom;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  The UpdateDelta class represents the communication strategy used for
 *  transfering data between the society agents and an aggregation agent.
 *  The typical usage of this class is as a container for ResultSetDataAtoms,
 *  which are loaded into the UpdateDelta by a society agent.  The delta is
 *  then transfered as XML to the aggregation agent, where it is reconstituted
 *  and applied to the appropriate result set.
 *  <br><br>
 *  An alternate usage is as a container for more generic XmlTransferable
 *  objects such as queries and result sets.  Though such an UpdateDelta cannot
 *  (at this time) be reconstituted automatically, this strategy is used in
 *  communications between UI clients and the aggregation agent.
 *  <br><br>
 *  An UpdateDelta can be used in either of two modes:  increment or
 *  replacement.  The former reflects the familiar behavior of the
 *  IncrementalSubscription class (with "added", "changed", and "removed"
 *  lists), while the latter produces a new result set in its entirety.  The
 *  mode may be set at any time without losing data, but a good practice is to
 *  set the mode once before any content elements are added.
 */
public class UpdateDelta {
  private static String AGENT_ID = "agent_id";
  private static String QUERY_ID = "query_id";
  private static String SESSION_ID = "session_id";
  private static String ADDED_TAG = "added";
  private static String CHANGED_TAG = "changed";
  private static String REMOVED_TAG = "removed";
  private static String REPLACEMENT_TAG = "replacement";
  private static String ERROR_TAG = "error";

  public static String UPDATE_TAG = "update";

  // for script error reporting
  private String errorReport = null;

  // By default, replacementMode is set to false.  The addedList also serves as
  // the replacementList, and hence should be used in either mode.  Both
  // changedList and removedList are used only when replacementMode is false.
  private boolean replacementMode = false;
  private List addedList = new LinkedList();
  private List changedList = new LinkedList();
  private List removedList = new LinkedList();

  private String cougaarAgentId = null;
  private String queryId = null;
  private String sessionKey = null;

  /**
   *  Create a new UpdateDelta, presumably for transport to a remote location.
   *  The three String identifiers indicate which COUGAAR agent, query, and
   *  local session are associated with this UpdateDelta.  Content must be
   *  added before this delta will be useful.
   *  <br><br>
   *  <b>Note:  The setReplacement() method must be called before content can
   *  safely be added.</b>  The boolean argument tells whether to create added,
   *  changed, and removed lists, or simply a replacement list.
   */
  public UpdateDelta (String agent, String query, String key) {
    cougaarAgentId = agent;
    queryId = query;
    sessionKey = key;
  }

  /**
   *  Create an UpdateDelta from XML representation.  Presumably, this is
   *  received from a remote location, complete with identifiers and content
   *  data elements.
   */
  public UpdateDelta (Element root) {
    this(root.getAttribute(AGENT_ID), root.getAttribute(QUERY_ID),
      root.getAttribute(SESSION_ID));

    NodeList nl = root.getElementsByTagName(ERROR_TAG);
    if (nl.getLength() > 0) {
      errorReport = XmlUtils.getElementText((Element) nl.item(0));
    }
    else {
      nl = root.getElementsByTagName(REPLACEMENT_TAG);
      if (nl.getLength() > 0) {
        setReplacement(true);
        loadAtoms(addedList, nl);
      }
      else {
        setReplacement(false);
        loadAtoms(addedList, root.getElementsByTagName(ADDED_TAG));
        loadAtoms(changedList, root.getElementsByTagName(CHANGED_TAG));
        loadAtoms(removedList, root.getElementsByTagName(REMOVED_TAG));
      }
    }
  }

  // This operation presumes that elements are ResultSetDataAtoms, which may
  // not be true.  Other implementations of XmlTransferable exist.
  private static void loadAtoms (List l, NodeList bunches) {
    if (bunches.getLength() == 0)
      return;

    // presume that there is at most one atom list of each variety
    NodeList atoms = ((Element) bunches.item(0)).getElementsByTagName(
      ResultSetDataAtom.DATA_ATOM_TAG);
    for (int i = 0; i < atoms.getLength(); i++)
      l.add(new ResultSetDataAtom((Element) atoms.item(i)));
  }

  public String getAgentId () {
    return cougaarAgentId;
  }

  public String getQueryId () {
    return queryId;
  }

  public List getAddedList () {
    return addedList;
  }

  public List getChangedList () {
    return changedList;
  }

  public List getRemovedList () {
    return removedList;
  }

  public List getReplacementList () {
    return addedList;
  }

  public void clearContents () {
    addedList.clear();
    changedList.clear();
    removedList.clear();
  }

  public boolean isErrorReport () {
    return errorReport != null;
  }

  public String getErrorReport () {
    return errorReport;
  }

  public void setErrorReport (Throwable t) {
    // capture the error as a String
    StringWriter w = new StringWriter();
    PrintWriter p = new PrintWriter(w);
    t.printStackTrace(p);
    p.flush();
    errorReport = w.toString();

    // clear out partial data
    clearContents();
  }

  public boolean isReplacement () {
    return replacementMode;
  }

  /**
   *  Set the "replacement mode" true or false.  If true, the UpdateDelta
   *  contains three lists of data elements after the fashion of an
   *  IncrementalSubscription; i.e., added, changed, and removed elements.  If
   *  false, the UpdateDelta contains a single list which should be interpreted
   *  as a replacement for all previously collected data elements.  This
   *  distinction is only meaningful for persistent queries.
   */
  public void setReplacement (boolean b) {
    replacementMode = b;
  }

  /**
   *  Convert this UpdateDelta to an XML format.  Depending on whether this is
   *  a replacement or not, this will, respectively, produce a list of elements
   *  intended to replace an existing result set or a list of modifications to
   *  be applied to an existing result set.  Data elements must implement the
   *  XmlTransferable interface.
   */
  public String toXml () {
    InverseSax doc = new InverseSax();
    includeXml(doc);
    return doc.toString();
  }

  public void includeXml (InverseSax doc) {
    doc.addElement(UPDATE_TAG);
    doc.addAttribute(SESSION_ID, sessionKey);
    doc.addAttribute(QUERY_ID, queryId);
    doc.addAttribute(AGENT_ID, cougaarAgentId);
    if (isErrorReport()) {
      doc.addTextElement(ERROR_TAG, errorReport);
    }
    else if (isReplacement()) {
      sendBunch(addedList, REPLACEMENT_TAG, doc);
    }
    else {
      sendBunch(addedList, ADDED_TAG, doc);
      sendBunch(changedList, CHANGED_TAG, doc);
      sendBunch(removedList, REMOVED_TAG, doc);
    }
    doc.endElement();
  }

  private static void sendBunch (List c, String type, InverseSax doc) {
    if (c != null) {
      doc.addElement(type);
      for (Iterator i = c.iterator(); i.hasNext(); )
        ((XmlTransferable) i.next()).includeXml(doc);
      doc.endElement();
    }
  }

  // - - - - - - - Testing Code - - - - - - - - - - - - - - - - - - - - - - - -

  private void summarize () {
    System.out.println("UpdateDelta:  (" + cougaarAgentId + ", " + queryId +
      ", " + sessionKey + ")");
    System.out.println("  - is errorReport:  " + isErrorReport());
    System.out.println("  - is replacement:  " + isReplacement());
    if (isErrorReport()) {
      System.out.println("<<");
      System.out.println(errorReport);
      System.out.println(">>");
    }
    else if (isReplacement()) {
      summarizeList("replacement", addedList);
    }
    else {
      summarizeList("added", addedList);
      summarizeList("changed", changedList);
      summarizeList("removed", removedList);
    }
  }

  private static void summarizeList (String name, List atoms) {
    System.out.print("  - ");
    if (atoms == null)
      System.out.println("no " + name + " list found");
    else if (atoms.size() == 0)
      System.out.println(name + " list is empty");
    else
      System.out.println(name + " list contains " + atoms.size() + " atom" +
        (atoms.size() == 1 ? "" : "s"));
  }

  public static void main (String[] argv) {
    testUpdateDelta(getIncrementalDelta());
    testUpdateDelta(getReplacementDelta());
    testUpdateDelta(getErrorDelta());
  }

  private static void testUpdateDelta (UpdateDelta ud) {
    System.out.println("<< testing UpdateDelta >>");
    ud.summarize();

    System.out.println("Generating XML:");
    System.out.print(ud.toXml());

    System.out.println("Parsing XML:");
    try {
      UpdateDelta ud2 = new UpdateDelta(XmlUtils.parse(ud.toXml()));
      ud2.summarize();

      System.out.println("Regenerating XML:");
      System.out.print(ud2.toXml());
    }
    catch (Exception eek) {
      System.out.println("  - Failed (" + eek + ")");
    }
    System.out.println("<< done >>");
  }

  private static int atom_serial_counter = 0;
  private static List atom_ids = new LinkedList();
  static {
    atom_ids.add("serial");
  }

  private static ResultSetDataAtom getRandomAtom () {
    ResultSetDataAtom ret = new ResultSetDataAtom(atom_ids,
      new CompoundKey(new String[] {String.valueOf(atom_serial_counter++)}));
    ret.addValue("random", String.valueOf(Math.random()));
    return ret;
  }

  private static UpdateDelta getIncrementalDelta () {
    UpdateDelta ud = new UpdateDelta("bla-agent", "bla-query", "bla-session");
    ud.setReplacement(false);

    ud.getAddedList().add(getRandomAtom());
    ud.getAddedList().add(getRandomAtom());
    ud.getAddedList().add(getRandomAtom());
    ud.getAddedList().add(getRandomAtom());
    ud.getChangedList().add(getRandomAtom());
    ud.getRemovedList().add(getRandomAtom());
    ud.getRemovedList().add(getRandomAtom());
    ud.getRemovedList().add(getRandomAtom());

    return ud;
  }

  private static UpdateDelta getReplacementDelta () {
    UpdateDelta ud = new UpdateDelta("bla-agent", "bla-query", "bla-session");
    ud.setReplacement(true);

    ud.getReplacementList().add(getRandomAtom());
    ud.getReplacementList().add(getRandomAtom());

    return ud;
  }

  private static UpdateDelta getErrorDelta () {
    UpdateDelta ud = new UpdateDelta("bla-agent", "bla-query", "bla-session");
    ud.setErrorReport(new Exception("bla-error"));
    return ud;
  }
}