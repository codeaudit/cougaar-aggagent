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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.cougaar.lib.aggagent.script.PythAggregator;
import org.cougaar.lib.aggagent.script.PythAlert;
import org.cougaar.lib.aggagent.script.PythIncrementFormat;
import org.cougaar.lib.aggagent.script.PythMelder;
import org.cougaar.lib.aggagent.script.PythUnaryPredicate;
import org.cougaar.lib.aggagent.script.PythXMLEncoder;
import org.cougaar.lib.aggagent.script.SilkAggregator;
import org.cougaar.lib.aggagent.script.SilkAlert;
import org.cougaar.lib.aggagent.script.SilkIncrementFormat;
import org.cougaar.lib.aggagent.script.SilkMelder;
import org.cougaar.lib.aggagent.script.SilkUnaryPredicate;
import org.cougaar.lib.aggagent.script.SilkXMLEncoder;
import org.cougaar.lib.aggagent.session.IncrementFormat;
import org.cougaar.lib.aggagent.session.XMLEncoder;
import org.cougaar.lib.aggagent.session.XmlIncrement;
import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.lib.aggagent.util.Enum.AggType;
import org.cougaar.lib.aggagent.util.Enum.Language;
import org.cougaar.lib.aggagent.util.Enum.ScriptType;
import org.cougaar.lib.aggagent.util.Enum.XmlFormat;
import org.cougaar.util.UnaryPredicate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *  An instance of this class may be used to represent a script, including the
 *  code itself plus a variety of information concerning the intended usage of
 *  the script.
 */
public class ScriptSpec implements Serializable {
  private static String CLASS_TAG = "class";
  private static String PARAM_TAG = "param";
  private static String LANGUAGE_ATT = "language";
  private static String TYPE_ATT = "type";
  private static String AGG_IDS_ATT = "aggIds";
  private static String NAME_ATT = "name";

  private static Class[] STRING_PARAM = new Class[] {String.class};

  private ScriptType type = null;
  private Language lang = null;

  // only used when the script is for XML encoding
  private XmlFormat format = null;

  // only used when the script is intended for data set aggregation
  private AggType aggType = null;
  private List aggIds = null;

  private String text = null;
  private Map params = new HashMap();

  /**
   *  Tell the language in which this script is written.  See Enum.Language
   *  for details.
   */
  public Language getLanguage () {
    return lang;
  }

  /**
   *  Tell the basic purpose of the script.  See Enum.ScriptType for details.
   */
  public ScriptType getType () {
    return type;
  }

  /**
   *  Retrieve the type of XML encoding that this script provides.  See
   *  Enum.XmlFormat for details.  This value is only meaningful if the script
   *  type is Enum.ScriptType.INCREMENT_FORMAT, and should be ignored otherwise.
   */
  public XmlFormat getFormat () {
    return format;
  }

  /**
   *  Retrieve the type of aggregation that this script provides.  See
   *  Enum.AggType for details.  This value is only meaningful if the script
   *  type is Enum.ScriptType.AGGREGATOR, and should be ignored otherwise.
   */
  public AggType getAggType () {
    return aggType;
  }

  /**
   *  Supply the caller with the text of the script.  In the case of a Java
   *  representation, the class name is returned.
   */
  public String getText () {
    return text;
  }

  /**
   *  Create a new ScriptSpec for the provided script with the purpose and
   *  language specified.  Everything else is left as its default value.
   */
  public ScriptSpec (ScriptType t, Language l, String s) {
    type = t;
    lang = l;
    text = s;
  }

  /**
   *  Create a new ScriptSpec for an IncrementFormat, with the language and
   *  XmlFormat specified.
   */
  public ScriptSpec (Language l, XmlFormat f, String s) {
    this(ScriptType.INCREMENT_FORMAT, l, s);
    format = f;
  }

  /**
   *  Create a new ScriptSpec for an Aggregator, with the aggregation type,
   *  collation IDs, and script language specified.
   */
  public ScriptSpec (Language l, AggType agg, String s, String ids) {
    this(ScriptType.AGGREGATOR, l, s);
    aggType = agg;
    aggIds = parseAggIds(ids);
  }

  /**
   *  Create a new ScriptSpec for a Java implementation.  Here, the purpose
   *  of the script, the class name, and a parameter map for the Java class are
   *  supplied by the caller.
   */
  public ScriptSpec (ScriptType t, String s, Map p) {
    this(t, Language.JAVA, s);
    if (p != null)
      params.putAll(p);
  }

  /**
   *  Create a new ScriptSpec for an IncrementFormat in Java.  The XmlFormat,
   *  class name, and parameter list are provided by the caller.
   */
  public ScriptSpec (XmlFormat f, String s, Map p) {
    this(Language.JAVA, f, s);
    if (p != null)
      params.putAll(p);
  }

  /**
   *  Create a new ScriptSpec for an Aggregator in Java.  The caller supplies
   *  the AggType, class name, collation IDs, and a parameter map for the Java
   *  class.
   */
  public ScriptSpec (AggType agg, String s, String ids, Map p) {
    type = ScriptType.AGGREGATOR;
    lang = Language.JAVA;
    aggType = agg;
    text = s;
    aggIds = parseAggIds(ids);
    if (p != null)
      params.putAll(p);
  }

  /**
   *  Reconstitute a ScriptSpec that has been converted to XML.  In effect,
   *  this constructor is the inverse of the toXml() method of this class.
   */
  public ScriptSpec (Element root) {
    type = ScriptType.fromString(root.getNodeName());
    lang = Language.fromString(root.getAttribute(LANGUAGE_ATT));
    if (type == ScriptType.INCREMENT_FORMAT)
      format = XmlFormat.fromString(root.getAttribute(TYPE_ATT));
    if (type == ScriptType.AGGREGATOR) {
      aggType = AggType.fromString(root.getAttribute(TYPE_ATT));
      aggIds = parseAggIds(root.getAttribute(AGG_IDS_ATT));
    }

    if (lang == Language.JAVA)
      parseJavaSpec(root);
    else
      parseScriptSpec(root);
  }

  private static List parseAggIds (String s) {
    if (s == null)
      return null;

    List ret = new LinkedList();
    StringTokenizer tok = new StringTokenizer(s, " ,;\t\r\n");
    while (tok.hasMoreTokens())
      ret.add(tok.nextToken());

    return ret;
  }

  private static String encodeAggIds (List l) {
    if (l == null)
      return null;

    StringBuffer buf = new StringBuffer();
    Iterator i = l.iterator();
    if (i.hasNext()) {
      buf.append(i.next());
      while (i.hasNext()) {
        buf.append(" ");
        buf.append(i.next());
      }
    }
    return buf.toString();
  }

  private void parseJavaSpec (Element elt) {
    NodeList nl = elt.getElementsByTagName(CLASS_TAG);
    if (nl.getLength() > 0)
      text = XmlUtils.getElementText((Element) nl.item(0)).trim();

    nl = elt.getElementsByTagName(PARAM_TAG);
    for (int i = 0; i < nl.getLength(); i++) {
      Element p = (Element) nl.item(i);
      String name = p.getAttribute(NAME_ATT);
      String val = XmlUtils.getElementText(p);
      params.put(name, val);
    }
  }

  private void parseScriptSpec (Element elt) {
    text = XmlUtils.getElementText(elt);
  }

  /**
   *  Convert this ScriptSpec to XML for transmission over a network.  The
   *  parsed XML document can then be used to reconstruct the ScriptSpec using
   *  the constructor that takes an Element as its argument.
   */
  public String toXml () {
    InverseSax doc = new InverseSax();
    includeXml(doc);
    return doc.toString();
  }

  public void includeXml (InverseSax doc) {
    doc.addElement(type.toString());
    doc.addAttribute(LANGUAGE_ATT, lang.toString());
    if (format != null)
      doc.addAttribute(TYPE_ATT, format.toString());
    if (aggType != null)
      doc.addAttribute(TYPE_ATT, aggType.toString());
    if (aggIds != null)
      doc.addAttribute(AGG_IDS_ATT, encodeAggIds(aggIds));

    if (lang == Language.JAVA)
      includeJavaXml(doc);
    else
      doc.addText(text);

    doc.endElement();
  }

  private void includeJavaXml (InverseSax doc) {
    doc.addTextElement(CLASS_TAG, text);
    for (Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry p = (Map.Entry) i.next();
      doc.addEltAttText(
        PARAM_TAG, NAME_ATT, p.getKey().toString(), p.getValue().toString());
    }
  }

  /**
   *  Retrieve the list of collation IDs for an Aggregator script.
   */
  public String getAggIdString () {
    if (aggIds != null)
      return encodeAggIds(aggIds);
    return null;
  }

  private static String propertySetterName (String property) {
    if (property == null || property.length() == 0)
      throw new IllegalArgumentException();

    StringBuffer buf = new StringBuffer("set");
    buf.append(Character.toUpperCase(property.charAt(0)));
    buf.append(property.substring(1));
    return buf.toString();
  }

  private void setBeanProperties (Object o) throws Exception {
    Class c = o.getClass();
    for (Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry p = (Map.Entry) i.next();
      String name = propertySetterName((String) p.getKey());
      Method m = c.getMethod(name, STRING_PARAM);
      m.invoke(o, new Object[] {p.getValue()});
    }
  }

  private Object makeBean () throws Exception {
    Object bean = null;
    if (text != null) {
      bean = Class.forName(text).newInstance();
      setBeanProperties(bean);
    }
    return bean;
  }

  private void checkTypeMatch (ScriptType makeType) throws Exception {
    if (makeType != type)
      throw new Exception(
        "Cannot make " + makeType + " from a " + type + " script");
  }

  /**
   *  Create a UnaryPredicate from this ScriptSpec, if appropriate.  If the
   *  language spec is not one of those recognized, then this method will
   *  return null.  If the script is not a UnaryPredicate script, then an
   *  Exception will be raised.
   */
  public UnaryPredicate toUnaryPredicate () throws Exception {
    checkTypeMatch(ScriptType.UNARY_PREDICATE);
    if (lang == Language.JPYTHON)
      return PythUnaryPredicate.predicateFromScript(text);
    else if (lang == Language.SILK)
      return new SilkUnaryPredicate(text);
    else if (lang == Language.JAVA)
      return (UnaryPredicate) makeBean();
    return null;
  }

  /**
   *  Create an Alert from this ScriptSpec, if appropriate.  If the language
   *  spec is not one of those recognized, then this method will return null.
   *  If the script is not an Alert script, then an Exception will be raised.
   */
  public Alert toAlert () throws Exception {
    checkTypeMatch(ScriptType.ALERT);
    if (lang == Language.JPYTHON)
      return PythAlert.parseAlert(text);
    else if (lang == Language.SILK)
      return new SilkAlert(text);
    else if (lang == Language.JAVA)
      return (Alert) makeBean();
    return null;
  }

  private IncrementFormat toScriptedFormat () throws Exception {
    if (lang == Language.JPYTHON)
      return PythIncrementFormat.formatFromScript(text);
    else if (lang == Language.SILK)
      return new SilkIncrementFormat(text);
    else if (lang == Language.JAVA)
      return (IncrementFormat) makeBean();
    return null;
  }

  private XMLEncoder toXMLEncoder () throws Exception {
    if (lang == Language.JPYTHON)
      return PythXMLEncoder.encoderFromScript(text);
    else if (lang == Language.SILK)
      return new SilkXMLEncoder(text);
    else if (lang == Language.JAVA)
      return (XMLEncoder) makeBean();
    return null;
  }

  /**
   *  Create an IncrementFormat from this ScriptSpec, if appropriate.  If the
   *  language spec is not one of those recognized, then this method will
   *  return null.  If the script is not an IncrementFormat script, then an
   *  Exception will be raised.
   */
  public IncrementFormat toIncrementFormat () throws Exception {
    checkTypeMatch(ScriptType.INCREMENT_FORMAT);
    if (format == XmlFormat.INCREMENT)
      return toScriptedFormat();
    else if (format == XmlFormat.XMLENCODER)
      return new XmlIncrement(toXMLEncoder());
    return null;
  }

  private Aggregator toScriptedAggregator () throws Exception {
    if (lang == Language.JPYTHON)
      return PythAggregator.aggregatorFromScript(text);
    else if (lang == Language.SILK)
      return new SilkAggregator(text);
    else if (lang == Language.JAVA)
      return (Aggregator) makeBean();
    return null;
  }

  private DataAtomMelder toDataAtomMelder () throws Exception {
    if (lang == Language.JPYTHON)
      return PythMelder.melderFromScript(text);
    else if (lang == Language.SILK)
      return new SilkMelder(text);
    else if (lang == Language.JAVA)
      return (DataAtomMelder) makeBean();
    return null;
  }

  /**
   *  Create an Aggregator from this ScriptSpec, if appropriate.  If the
   *  language spec is not one of those recognized, then this method will
   *  return null.  If the script is not an Aggregator script, then an
   *  Exception will be raised.
   */
  public Aggregator toAggregator () throws Exception {
    checkTypeMatch(ScriptType.AGGREGATOR);
    if (aggType == AggType.AGGREGATOR)
      return toScriptedAggregator();
    else if (aggType == AggType.MELDER)
      return new BatchAggregator(aggIds, toDataAtomMelder());
    return null;
  }

  /**
   *  Convert this ScriptSpec to a Java Object of the appropriate type.
   */
  public Object toObject ()  throws Exception {
    if (type == ScriptType.UNARY_PREDICATE)
      return toUnaryPredicate();
    else if (type == ScriptType.INCREMENT_FORMAT)
      return toIncrementFormat();
    else if (type == ScriptType.ALERT)
      return toAlert();
    else if (type == ScriptType.AGGREGATOR)
      return toAggregator();
    return null;
  }

  /**
   *  Create a UnaryPredicate directly from an XML representation.  If the
   *  document does not specify a UnaryPredicate, an Exception may be raised.
   */
  public static UnaryPredicate makeUnaryPredicate (Element elt)
      throws Exception
  {
    return new ScriptSpec(elt).toUnaryPredicate();
  }

  /**
   *  Create an IncrementFormat directly from an XML representation.  If the
   *  document does not specify an IncrementFormat, an Exception may be raised.
   */
  public static IncrementFormat makeIncrementFormat (Element elt)
      throws Exception
  {
    return new ScriptSpec(elt).toIncrementFormat();
  }

  /**
   *  Create an Alert directly from an XML representation.  If the document
   *  does not specify an Alert, an Exception may be raised.
   */
  public static Alert makeAlert (Element elt) throws Exception {
    return new ScriptSpec(elt).toAlert();
  }

  /**
   *  Create an Aggregator directly from an XML representation.  If the
   *  document does not specify an Aggregator, an Exception may be raised.
   */
  public static Aggregator makeAggregator (Element elt) throws Exception {
    return new ScriptSpec(elt).toAggregator();
  }

  /**
   *  Create an Object of the appropriate type directly from an XML
   *  representation.
   */
  public static Object makeObject (Element elt) throws Exception {
    return new ScriptSpec(elt).toObject();
  }
}
