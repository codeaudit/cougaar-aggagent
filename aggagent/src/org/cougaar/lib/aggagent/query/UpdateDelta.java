
package org.cougaar.lib.aggagent.query;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.*;

import org.cougaar.lib.aggagent.util.XmlUtils;

public class UpdateDelta {
  private static String AGENT_ID = "agent_id";
  private static String QUERY_ID = "query_id";
  private static String SESSION_ID = "session_id";
  private static String ADDED_TAG = "added";
  private static String CHANGED_TAG = "changed";
  private static String REMOVED_TAG = "removed";
  private static String REPLACEMENT_TAG = "replacement";

  public static String UPDATE_TAG = "update";

  private List addedList = null;
  private List changedList = null;
  private List removedList = null;
  private List replacementList = null;

  private String cougaarAgentId = null;
  private String queryId = null;
  private String sessionKey = null;

  public UpdateDelta (String agent, String query, String key) {
    cougaarAgentId = agent;
    queryId = query;
    sessionKey = key;
  }

  public UpdateDelta (Element root) {
    this(root.getAttribute(AGENT_ID), root.getAttribute(QUERY_ID),
      root.getAttribute(SESSION_ID));

    NodeList nl = root.getElementsByTagName(REPLACEMENT_TAG);
    if (nl.getLength() > 0) {
      setReplacement(true);
      loadAtoms(replacementList, nl);
    }
    else {
      setReplacement(false);
      loadAtoms(addedList, root.getElementsByTagName(ADDED_TAG));
      loadAtoms(changedList, root.getElementsByTagName(CHANGED_TAG));
      loadAtoms(removedList, root.getElementsByTagName(REMOVED_TAG));
    }
  }

  private static void loadAtoms (List l, NodeList bunches) {
    if (bunches.getLength() == 0)
      return;

    // presume that there is at most one atom list of each variety
    NodeList atoms = ((Element) bunches.item(0)).getElementsByTagName(
      ResultSetDataAtom.DATA_ATOM_TAG);
    for (int i = 0; i < atoms.getLength(); i++)
      l.add(new ResultSetDataAtom((Element) atoms.item(i)));
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
    return replacementList;
  }

  public boolean isReplacement () {
    return replacementList != null;
  }

  public void setReplacement (boolean b) {
    if (b) {
      addedList = null;
      changedList = null;
      removedList = null;
      replacementList = new LinkedList();
    }
    else {
      addedList = new LinkedList();
      changedList = new LinkedList();
      removedList = new LinkedList();
      replacementList = null;
    }
  }

  public String toXml () {
    StringBuffer buf = new StringBuffer();
    appendHeader(buf);
    if (isReplacement()) {
      sendBunch(replacementList, REPLACEMENT_TAG, buf);
    }
    else {
      sendBunch(addedList, ADDED_TAG, buf);
      sendBunch(changedList, CHANGED_TAG, buf);
      sendBunch(removedList, REMOVED_TAG, buf);
    }
    appendTrailer(buf);

    return buf.toString();
  }

  private void appendHeader (StringBuffer buf) {
    buf.append("<");
    buf.append(UPDATE_TAG);
    appendAttribute(SESSION_ID, sessionKey, buf);
    appendAttribute(QUERY_ID, queryId, buf);
    appendAttribute(AGENT_ID, cougaarAgentId, buf);
    buf.append(">\n");
  }

  private static void appendTrailer (StringBuffer buf) {
    buf.append("</");
    buf.append(UPDATE_TAG);
    buf.append(">\n");
  }

  private static void appendAttribute (String n, String v, StringBuffer buf) {
    buf.append(" ");
    buf.append(n);
    buf.append("=\"");
    buf.append(v);
    buf.append("\"");
  }

  private static void sendBunch (List c, String type, StringBuffer buf) {
    if (c == null)
      return;

    buf.append("<");
    buf.append(type);
    buf.append(">\n");

    for (Iterator i = c.iterator(); i.hasNext(); )
      buf.append(((ResultSetDataAtom) i.next()).toXML());

    buf.append("</");
    buf.append(type);
    buf.append(">\n");
  }

  // - - - - - - - Testing Code - - - - - - - - - - - - - - - - - - - - - - - -

  private void summarize () {
    System.out.println("UpdateDelta:  (" + cougaarAgentId + ", " + queryId +
      ", " + sessionKey + ")");
    System.out.println("  - is replacement:  " + isReplacement());
    if (isReplacement()) {
      summarizeList("replacement", replacementList);
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
}