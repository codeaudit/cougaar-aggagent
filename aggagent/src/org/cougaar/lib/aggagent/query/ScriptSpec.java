
package org.cougaar.lib.aggagent.query;

import java.lang.reflect.Method;
import java.util.*;

import org.w3c.dom.*;

import org.cougaar.util.UnaryPredicate;

import org.cougaar.lib.aggagent.query.Alert;
import org.cougaar.lib.aggagent.script.*;
import org.cougaar.lib.aggagent.session.*;
import org.cougaar.lib.aggagent.util.Enum.*;
import org.cougaar.lib.aggagent.util.XmlUtils;

/**
 *  An instance of this class may be used to represent a script, including the
 *  code itself plus a variety of information concerning the intended usage of
 *  the script.
 */
public class ScriptSpec {
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

  public Language getLanguage () {
    return lang;
  }

  /**
   *  Tell the basic purpose of the script.
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

  public String getText () {
    return text;
  }

  public ScriptSpec () {
  }

  public ScriptSpec (ScriptType t, Language l, String s) {
    type = t;
    lang = l;
    text = s;
  }

  public ScriptSpec (Language l, XmlFormat f, String s) {
    type = ScriptType.INCREMENT_FORMAT;
    lang = l;
    format = f;
    text = s;
  }

  public ScriptSpec (Language l, AggType agg, String s, String ids) {
    type = ScriptType.AGGREGATOR;
    lang = l;
    aggType = agg;
    text = s;
    aggIds = parseAggIds(ids);
  }

  public ScriptSpec (ScriptType t, String s, Map p) {
    type = t;
    lang = Language.JAVA;
    text = s;
    if (p != null)
      params.putAll(p);
  }

  public ScriptSpec (XmlFormat f, String s, Map p) {
    type = ScriptType.INCREMENT_FORMAT;
    lang = Language.JAVA;
    format = f;
    text = s;
    if (p != null)
      params.putAll(p);
  }

  public ScriptSpec (Element root) {
    type = ScriptType.fromString(root.getNodeName());
    lang = Language.fromString(root.getAttribute("language"));
    if (type == ScriptType.INCREMENT_FORMAT)
      format = XmlFormat.fromString(root.getAttribute("type"));
    if (type == ScriptType.AGGREGATOR) {
      aggType = AggType.fromString(root.getAttribute("type"));
      aggIds = parseAggIds(root.getAttribute("aggIds"));
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
    NodeList nl = elt.getElementsByTagName("class");
    if (nl.getLength() > 0)
      text = XmlUtils.getElementText((Element) nl.item(0)).trim();

    nl = elt.getElementsByTagName("param");
    for (int i = 0; i < nl.getLength(); i++) {
      Element p = (Element) nl.item(i);
      String name = p.getAttribute("name");
      String val = XmlUtils.getElementText(p);
      params.put(name, val);
    }
  }

  private void parseScriptSpec (Element elt) {
    text = XmlUtils.getElementText(elt);
  }

  private void doJavaXml (StringBuffer buf) {
    buf.append("<class>");
    buf.append(XmlUtils.replaceIllegalChars(text));
    buf.append("</class>");
    for (Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry p = (Map.Entry) i.next();
      buf.append("<param name=\"");
      buf.append(XmlUtils.replaceIllegalChars(p.getKey().toString()));
      buf.append("\">");
      buf.append(XmlUtils.replaceIllegalChars(p.getValue().toString()));
      buf.append("</param>");
    }
  }

  public String toXml () {
    StringBuffer buf = new StringBuffer();
    buf.append("<");
    buf.append(type);
    buf.append(" language=\"");
    buf.append(lang);
    buf.append("\"");
    if (format != null) {
      buf.append(" type=\"");
      buf.append(format);
      buf.append("\"");
    }
    if (aggType != null) {
      buf.append(" type=\"");
      buf.append(aggType);
      buf.append("\"");
    }
    if (aggIds != null) {
      buf.append(" aggIds=\"");
      buf.append(encodeAggIds(aggIds));
      buf.append("\"");
    }
    buf.append(">");

    if (lang == Language.JAVA)
      doJavaXml(buf);
    else
      buf.append(XmlUtils.replaceIllegalChars(text));

    buf.append("</");
    buf.append(type);
    buf.append(">");

    return buf.toString();
  }

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

  public UnaryPredicate toUnaryPredicate () throws Exception {
    if (lang == Language.JPYTHON)
      return PythUnaryPredicate.predicateFromScript(text);
    else if (lang == Language.SILK)
      return new SilkUnaryPredicate(text);
    else if (lang == Language.JAVA)
      return (UnaryPredicate) makeBean();
    return null;
  }

  public Alert toAlert () throws Exception {
    if (lang == Language.JPYTHON)
      return PythAlert.parseAlert(text);
    else if (lang == Language.SILK)
      return new SilkAlert(text);
    else if (lang == Language.JAVA)
      return (Alert) makeBean();
    return null;
  }

  public IncrementFormat toScriptedFormat () throws Exception {
    if (lang == Language.JPYTHON)
      return PythIncrementFormat.formatFromScript(text);
    else if (lang == Language.SILK)
      return new SilkIncrementFormat(text);
    else if (lang == Language.JAVA)
      return (IncrementFormat) makeBean();
    return null;
  }

  public XMLEncoder toXMLEncoder () throws Exception {
    if (lang == Language.JPYTHON)
      return PythXMLEncoder.encoderFromScript(text);
    else if (lang == Language.SILK)
      return new SilkXMLEncoder(text);
    else if (lang == Language.JAVA)
      return (XMLEncoder) makeBean();
    return null;
  }

  public IncrementFormat toIncrementFormat () throws Exception {
    if (format == XmlFormat.INCREMENT)
      return toScriptedFormat();
    else if (format == XmlFormat.XMLENCODER)
      return new XmlIncrement(toXMLEncoder());
    return null;
  }

  public Aggregator toScriptedAggregator () throws Exception {
    if (lang == Language.JPYTHON)
      return PythAggregator.aggregatorFromScript(text);
    else if (lang == Language.SILK)
      return new SilkAggregator(text);
    else if (lang == Language.JAVA)
      return (Aggregator) makeBean();
    return null;
  }

  public DataAtomMelder toDataAtomMelder () throws Exception {
    if (lang == Language.JPYTHON)
      return PythMelder.melderFromScript(text);
    else if (lang == Language.SILK)
      return new SilkMelder(text);
    else if (lang == Language.JAVA)
      return (DataAtomMelder) makeBean();
    return null;
  }

  public Aggregator toAggregator () throws Exception {
    if (aggType == AggType.AGGREGATOR)
      return toScriptedAggregator();
    else if (aggType == AggType.MELDER)
      return new BatchAggregator(aggIds, toDataAtomMelder());
    return null;
  }

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

  public static UnaryPredicate makeUnaryPredicate (Element elt)
      throws Exception
  {
    return new ScriptSpec(elt).toUnaryPredicate();
  }

  public static IncrementFormat makeIncrementFormat (Element elt)
      throws Exception
  {
    return new ScriptSpec(elt).toIncrementFormat();
  }

  public static Alert makeAlert (Element elt) throws Exception {
    return new ScriptSpec(elt).toAlert();
  }

  public static Aggregator makeAggregator (Element elt) throws Exception {
    return new ScriptSpec(elt).toAggregator();
  }

  public static Object makeObject (Element elt) throws Exception {
    return new ScriptSpec(elt).toObject();
  }
}
