/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.lib.aggagent.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cougaar.lib.aggagent.session.XmlTransferable;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
// import org.cougaar.lib.aggagent.util.XmlUtils;

public class ResultSetDataAtom implements XmlTransferable, Serializable {
  public static String DATA_ATOM_TAG = "data_atom";
  private static String ID_TAG = "id";
  private static String VALUE_TAG = "value";
  private static String NAME_ATT = "name";
  private static String VALUE_ATT = "value";

  private Object lock = new Serializable(){};

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
    NodeList nl = root.getElementsByTagName(ID_TAG);
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element e = (Element)nl.item(i);
      addIdentifier(e.getAttribute(NAME_ATT), e.getAttribute(VALUE_ATT));
    }
    nl = root.getElementsByTagName(VALUE_TAG);
    for (int i = 0; i < nl.getLength(); i++)
    {
      Element e = (Element)nl.item(i);
      addValue(e.getAttribute(NAME_ATT), e.getAttribute(VALUE_ATT));
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

  public String toString () {
    return toXml();
  }

  public String toXml () {
    InverseSax doc = new InverseSax();
    includeXml(doc);
    return doc.toString();
  }

  public void includeXml (InverseSax doc) {
    synchronized (lock) {
      doc.addElement(DATA_ATOM_TAG);
      addNameValueTags(ID_TAG, identifiers.entrySet().iterator(), doc);
      addNameValueTags(VALUE_TAG, values.entrySet().iterator(), doc);
      doc.endElement();
    }
  }

  private void addNameValueTags (String tag, Iterator i, InverseSax doc) {
    while (i.hasNext()) {
      Map.Entry me = (Map.Entry) i.next();
      doc.addElement(tag);
      doc.addAttribute(NAME_ATT, me.getKey().toString());
      doc.addAttribute(VALUE_ATT, me.getValue().toString());
      doc.endElement();
    }
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
