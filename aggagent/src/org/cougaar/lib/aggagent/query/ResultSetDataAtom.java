package org.cougaar.lib.aggagent.query;

import org.w3c.dom.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ResultSetDataAtom
{
  public static String DATA_ATOM_TAG = "data_atom";

  private Object lock = new Object();

  // the aggregation of all the identifiers defines how these values
  // should be managed.  Sample identifiers:  "org", "time", "metric", "item"
  // A TreeMap is used here to guarantee that order is preserved
  private Map identifiers = new TreeMap();

  // data payload for this managed set of data
  private Map values = new HashMap();

  public ResultSetDataAtom() {}

  public ResultSetDataAtom(ResultSetDataAtom da)
  {
    identifiers = (Map)((TreeMap)da.identifiers).clone();
    values = (Map)((HashMap)da.values).clone();
  }

  private static Map makeIdMap (List l, CompoundKey k) {
    Map ret = new TreeMap();
    Iterator lit = l.iterator();
    Iterator kit = k.getKeys();
    while (kit.hasNext() && lit.hasNext())
      ret.put(lit.next(), kit.next());

    return ret;
  }

  public ResultSetDataAtom (List l, CompoundKey k) {
    this(l, k, new HashMap());
  }

  public ResultSetDataAtom (List l, CompoundKey k, Map vals) {
    identifiers = makeIdMap(l, k);
    values = vals;
  }

  public ResultSetDataAtom(Element root)
  {
    NodeList nl = root.getElementsByTagName("id");
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element e = (Element)nl.item(i);
      addIdentifier(e.getAttribute("name"), e.getAttribute("value"));
    }
    nl = root.getElementsByTagName("value");
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element e = (Element)nl.item(i);
      addValue(e.getAttribute("name"), e.getAttribute("value"));
    }
  }

  /**
   * used to gain access to data payload (or to split the atom for storage)
   */
  public Map getValueMap()
  {
    return values;
  }

  /**
   * used to set data payload (or to recombine the atom for retrieval)
   */
  public void setValueMap(Map newMap)
  {
    values = newMap;
  }

  public void addValue(Object name, Object value)
  {
    synchronized (lock) {
      values.put(name, value);
    }
  }

  public Object removeValue(Object name)
  {
    synchronized (lock) {
      return values.remove(name);
    }
  }

  public Object getValue(Object name)
  {
    synchronized (lock) {
      return values.get(name);
    }
  }

  public Iterator getValueNames()
  {
    synchronized (lock) {
      return new LinkedList(values.keySet()).iterator();
    }
  }

  public void addIdentifier(Object idName, Object idValue)
  {
    synchronized (lock) {
      identifiers.put(idName, idValue);
    }
  }

  public Object removeIdentifier(Object idName)
  {
    synchronized (lock) {
      return identifiers.remove(idName);
    }
  }

  public Object getIdentifier(Object idName)
  {
    synchronized (lock) {
      return identifiers.get(idName);
    }
  }

  public Iterator getIdentifierNames()
  {
    synchronized (lock) {
      return new LinkedList(identifiers.keySet()).iterator();
    }
  }

  public CompoundKey getKey (List l) {
    return new CompoundKey (l, identifiers);
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer();

    synchronized (lock) {
      s.append("[");
      s.append(createNameValueString(identifiers.entrySet().iterator()));
      s.append(" : ");
      s.append(createNameValueString(values.entrySet().iterator()));
      s.append("]");
    }

    return s.toString();
  }

  public String toXML()
  {
    StringBuffer xml = new StringBuffer();

    synchronized (lock) {
      xml.append("<");
      xml.append(DATA_ATOM_TAG);
      xml.append(">\n");
      xml.append(createNameValueTags("id", identifiers.entrySet().iterator()));
      xml.append(createNameValueTags("value", values.entrySet().iterator()));
      xml.append("</");
      xml.append(DATA_ATOM_TAG);
      xml.append(">\n");
    }

    return xml.toString();
  }

  private String createNameValueString(Iterator i)
  {
    StringBuffer s = new StringBuffer();

    while(i.hasNext())
    {
      Map.Entry me = (Map.Entry)i.next();
      s.append(me.getKey().toString());
      s.append("=");
      s.append(me.getValue().toString());
      if (i.hasNext())
        s.append(", ");
    }

    return s.toString();
  }

  private String createNameValueTags(String tagName, Iterator i)
  {
    StringBuffer s = new StringBuffer();

    while(i.hasNext())
    {
      Map.Entry me = (Map.Entry)i.next();
      s.append("  <");
      s.append(tagName);
      s.append(" name = \"");
      s.append(me.getKey());
      s.append("\" value = \"");
      s.append(me.getValue());
      s.append("\" />\n");
    }

    return s.toString();
  }

  /**
   *  Useful for debugging.
   */
  public void summarize () {
    System.out.println("ResultSetDataAtom:  summary:");
    formatMap("identifiers", identifiers);
    formatMap("values", values);
    System.out.println("ResultSetDataAtom:  done.");
  }

  private static void formatMap (String name, Map m) {
    System.out.print("  -> " + name);
    if (m.size() == 0)
      System.out.print(" <NONE>");
    System.out.println();
    for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry me = (Map.Entry) i.next();
      System.out.println("  - -> " + me.getKey() + " = " + me.getValue());
    }
  }
}
