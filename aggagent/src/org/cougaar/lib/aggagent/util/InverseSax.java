
package org.cougaar.lib.aggagent.util;

import org.w3c.dom.*;

/**
 *  InverseSax is a class that acts as the reverse of a SAX parser.  In other
 *  words, it creates the text of an XML document by accepting notification
 *  of parts of the XML structure through method calls.  Those parts include
 *  opening and closing tags, attributes, and text.  In the attributes and
 *  text, encoding of special characters is handled automatically.
 */
public class InverseSax {
  private static class NameNode {
    public String tag = null;
    public NameNode next = null;

    public NameNode (String t, NameNode n) {
      tag = t;
      next = n;
    }
  }

  private static byte EMPTY = 0;
  private static byte IN_TAG = 1;
  private static byte IN_ELEMENT = 2;
  private static byte DONE = 3;

  private byte state = EMPTY;

  private StringBuffer buf = new StringBuffer();
  private NameNode nameStack = null;
  private boolean lenientMode = false;

  private void pushName (String name) {
    nameStack = new NameNode(name, nameStack);
  }

  private boolean nameStackEmpty () {
    return nameStack == null;
  }

  private String popName () {
    NameNode p = nameStack;
    nameStack = p.next;
    return p.tag;
  }

  private void encode (String s) {
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (c == '&')
        buf.append("&amp;");
      else if (c == '<')
        buf.append("&lt;");
      else if (c == '>')
        buf.append("&gt;");
      else if (c == '\'')
        buf.append("&apos;");
      else if (c == '"')
        buf.append("&quot;");
      else
        buf.append(c);
    }
  }

  public void setLenientMode (boolean b) {
    lenientMode = b;
  }

  // Allow upper- and lower-case letters and underscores.
  // Currently, the colon is not allowed--we don't use namespaces.
  private static boolean validateInitial (char c) {
    return
      ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_'
      /* || c == ':' */;
  }

  // Any initial is allowed here, as well as digits, hyphens, and periods.
  private static boolean validateNameChar (char c) {
    return
      validateInitial(c) || ('0' <= c && c <= '9') || c == '-' || c == '.';
  }

  private boolean validateName (String s) {
    if (s == null || s.length() == 0) {
      return false;
    }
    else if (!lenientMode) {
      char[] chars = s.toCharArray();
      if (!validateInitial(chars[0]))
        return false;
      else
        for (int i = 1; i < chars.length; i++)
          if (!validateNameChar(chars[i]))
            return false;
    }
    return true;
  }

  /**
   *  Return this XML document generator to its pristine state, abandoning any
   *  work previously in progress.
   */
  public void reset () {
    buf = new StringBuffer();
    nameStack = null;
    state = EMPTY;
  }

  /**
   *  Add a new element to the document.  This can be the document root or a
   *  child of another element.  After the root element has been closed, no
   *  more elements may be added, and attempting to do so will result in an
   *  IllegalStateException.  This method also verifies that the tag name is
   *  valid (see above).
   */
  public void addElement (String tag) {
    if (state == DONE)
      throw new IllegalStateException("end of document--can't add elements");
    if (!validateName(tag))
      throw new IllegalArgumentException("illegal tag name:  " + tag);
    if (state == IN_TAG)
      buf.append(">");
    buf.append("<");
    buf.append(tag);
    pushName(tag);
    state = IN_TAG;
  }

  /**
   *  Convenience method for adding an element with text but no attributes or
   *  child elements.
   */
  public void addTextElement (String tag, String text) {
    addElement(tag);
    addText(text);
    endElement();
  }

  /**
   *  Add an attribute to the current XML element.  This method is only valid
   *  after creating an element and before adding other contents, such as text
   *  or child elements.  Use of this method at any other time will raise an
   *  IllegalStateException.  Special characters within the attribute value are
   *  automatically replaced with the appropriate character entities.  This
   *  method also verifies that the tag name is valid (see above).
   */
  public void addAttribute (String name, String value) {
    if (state != IN_TAG)
      throw new IllegalStateException("attributes belong inside an XML tag");
    if (!validateName(name))
      throw new IllegalArgumentException("illegal attribute name:  " + name);
    buf.append(" ");
    buf.append(name);
    buf.append("=\"");
    encode(value);
    buf.append("\"");
  }

  /**
   *  Add text content to the current XML element.  This method is valid any
   *  time after the root element is opened but before it is closed.  This
   *  method may be called multiple times within a single element, but the
   *  effect is the same as calling it once with the concatenation of the text
   *  of the many calls (in the same order).
   */
  public void addText (String text) {
    if (state == EMPTY || state == DONE)
      throw new IllegalStateException("text belongs inside an XML element");
    if (state == IN_TAG)
      buf.append(">");
    encode(text);
    state = IN_ELEMENT;
  }

  /**
   *  Close the current element.  Every tag must be closed explicitly by a
   *  call to this method (or endDocument, which calls this method).
   */
  public void endElement () {
    if (state == EMPTY || state == DONE)
      throw new IllegalStateException("can't close element--none is current");
    String tag = popName();
    if (state == IN_TAG) {
      buf.append("/>");
    }
    else {
      buf.append("</");
      buf.append(tag);
      buf.append(">");
    }
    if (nameStackEmpty())
      state = DONE;
    else
      state = IN_ELEMENT;
  }

  /**
   *  This method probably shouldn't be used under normal conditions.  However,
   *  in case an error or some other unexpected condition is encountered while
   *  creating the XML document, this method can be used to end the document
   *  gracefully.  Following any call to this method, toString() is guaranteed
   *  to return either the text of a well-formed XML document or the empty
   *  String (and the latter only if no elements were added).
   *  <br><br>
   *  After this method is called, no more content may be added, even if the
   *  document is empty.
   */
  public void endDocument () {
    while (!nameStackEmpty())
      endElement();
    state = DONE;
  }

  /**
   *  Return the text of the XML document.
   */
  public String toString () {
    return buf.toString();
  }

  // - - - - - - - Testing Harness - - - - - - - - - - - - - - - - - - - - - - -

  public static void main (String[] argv) {
    InverseSax doc = new InverseSax();
    doc.addElement("bla.bla");
    doc.addAttribute("type", "bl<a>h");
    doc.addAttribute("bla.id", "sc&um");
    doc.addText("SomeText");
    for (int i = 0; i < 5; i++) {
      doc.addElement("yargh");
      doc.addAttribute("value", "high");
      doc.addText("<" + i + ">");
      doc.endElement();
    }
    doc.endElement();

    System.out.println(doc.toString());
    System.out.println();

    try {
      Element elt = XmlUtils.parse(doc.toString());
      recursivePrint(elt);
    }
    catch (Exception bugger_off) { }
  }

  private static void recursivePrint (Element elt) {
    System.out.print("{node(" + elt.getNodeName() + ")[");
    NamedNodeMap nnm = elt.getAttributes();
    for (int i = 0; i < nnm.getLength(); i++) {
      Node att = nnm.item(i);
      System.out.print(att.getNodeName() + "=" + att.getNodeValue() + ";");
    }
    System.out.print("]");
    NodeList nl = elt.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE)
        recursivePrint((Element) child);
      else if (child.getNodeType() == Node.TEXT_NODE)
        System.out.print("\"" + child.getNodeValue() + "\"");
    }
    System.out.print("}");
  }
}