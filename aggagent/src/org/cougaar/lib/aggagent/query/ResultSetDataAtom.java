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
