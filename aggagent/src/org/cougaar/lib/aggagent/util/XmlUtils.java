package org.cougaar.lib.aggagent.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtils {
  /**
   * when debug is set to true, all XML is printed to standard out before it
   * is parsed.
   */
  public static boolean debug = false;

  public static int getChildInt (Element elt, String tag)
      throws NumberFormatException
  {
    return Integer.parseInt(getChildText(elt, tag));
  }

  public static Element getChildElement (Element elt, String tag) {
    NodeList nl = elt.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(tag))
        return (Element) n;
    }
    return null;
  }

  public static String getChildText (Element elt, String tag) {
    return getElementText(getChildElement(elt, tag));
  }

  public static String getElementText (Element elt) {
    if (elt == null)
      return null;

    StringBuffer buf = new StringBuffer();
    NodeList nl = elt.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.TEXT_NODE)
        buf.append(n.getNodeValue());
    }
    return buf.toString();
  }

  public static Element parse (String s)
    throws IOException, SAXException {
    if (debug)
    {
      System.out.println("------------------ XML to parse ------------------");
      System.out.println(s);
      System.out.println("--------------------------------------------------");
    }
    InputSource in = new InputSource(new StringReader(s));
    return parse(in);
  }

  public static Element parse (InputStream s)
    throws IOException, SAXException {
    if (debug)
    {
      // redirect to string based reader for debug
      String in = readToString(s);
      return parse(in);
    }
    InputSource in = new InputSource(s);
    return parse(in);
  }

  public static Element parse(InputSource in)
    throws IOException, SAXException {
    DOMParser p = new DOMParser();
    try
    {
      p.parse(in);
    } catch(SAXException e)
    {
      // See if anything is recoverable
      if ((p.getDocument() == null) ||
          (p.getDocument().getDocumentElement() == null))
        throw e;

      // find and return exception element
      NodeList nl =
        p.getDocument().getDocumentElement().getElementsByTagName("exception");
      if (nl.getLength() > 0)
        return (Element)nl.item(0);
      else
        throw e;
    }
    return p.getDocument().getDocumentElement();
  }

  /**
   * Post request to URL represented by passed string.  Return parsed XML
   * response.  Currently just catches exceptions and prints message to
   * standard out (returning null).
   */
  public static Element requestXML(String urlString, String request)
  {
    Element parsedResponse = null;
    InputStream is = null;

    try {
      is = sendRequest(urlString, request);
      parsedResponse = parse(is);
    }
    catch (MalformedURLException mfe) {
      System.out.println("Failed to send request:  bad URL<br>");
      System.out.println("\"" + urlString + "\"");
    }
    catch (IOException ioe) {
      System.out.println("Failed to send request:  io Exception");
    }
    catch (SAXException se) {
      System.out.println("Failed to parse response:  sax Exception");
      se.printStackTrace();
    }

    return parsedResponse;
  }

  /**
   * Post request to URL represented by passed string.  Return response as
   * string.  Currently just catches exceptions and prints message to
   * standard out (returning null).
   */
  public static String requestString(String urlString, String request)
  {
    String response = null;

    try {
      InputStream is = sendRequest(urlString, request);
      response = readToString(is);
    }
    catch (MalformedURLException mfe) {
      System.out.println("Failed to send request:  bad URL<br>");
      System.out.println("\"" + urlString + "\"");
    }
    catch (IOException ioe) {
      System.out.println("Failed to send request:  io Exception");
    }

    return response;
  }

  /**
   * Post request to URL represented by passed string.  Return response as
   * input stream.
   */
  public static InputStream sendRequest(String urlString, String request)
    throws MalformedURLException, IOException
  {
    // open connection
    URL url = new URL(urlString);
    URLConnection uc = url.openConnection();
    uc.setDoInput(true);
    uc.setDoOutput(true);
    OutputStream os = uc.getOutputStream();
    PrintStream servicePrint = new PrintStream(os);

    // send request
    servicePrint.println(request);

    return uc.getInputStream();
  }

  public static String readToString(InputStream is) throws IOException
  {
    StringBuffer buf = new StringBuffer();
    BufferedReader bufr =
      new BufferedReader(new InputStreamReader(is));
    String line = null;
    while ((line = bufr.readLine()) != null) {
      buf.append(line);
      buf.append("\n");
    }

    return buf.toString();
  }

  public static String replaceIllegalChars (String s) {
    StringBuffer buf = new StringBuffer();
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
    return buf.toString();
  }

  public static void sendException(String queryId, String clusterId,
                                   Exception e, PrintStream out)
  {
    out.flush();
    out.print("<exception cluster_id=\"");
    out.print(clusterId);
    out.print("\" query_id=\"");
    out.print(queryId);
    out.println("\">");
    StringWriter exception = new StringWriter();
    e.printStackTrace(new PrintWriter(exception));
    String encodedException = replaceIllegalChars(exception.toString());
    out.println(encodedException);
    out.println("</exception>");
  }

  public static void sendException(String clusterId, Exception e,
                                   PrintStream out)
  {
    out.flush();
    out.print("<exception cluster_id=\"");
    out.print(clusterId);
    out.println("\">");
    StringWriter exception = new StringWriter();
    e.printStackTrace(new PrintWriter(exception));
    String encodedException = replaceIllegalChars(exception.toString());
    out.println(encodedException);
    out.println("</exception>");
  }

  public static void appendAttribute (String n, String v, StringBuffer buf) {
    buf.append(" ");
    buf.append(n);
    buf.append("=\"");
    buf.append(replaceIllegalChars(v));
    buf.append("\"");
  }

  public static void appendCloseTag (String n, StringBuffer buf) {
    buf.append("</");
    buf.append(n);
    buf.append(">\n");
  }

  public static void appendOpenTag (String n, StringBuffer buf) {
    appendOpenTag(n, buf, true);
  }

  public static void appendOpenTag (String n, StringBuffer buf, boolean cr) {
    buf.append("<");
    buf.append(n);
    buf.append(">");
    if (cr)
      buf.append("\n");
  }

  public static void appendTextElement (String n, String t, StringBuffer buf) {
    appendOpenTag(n, buf, false);
    buf.append(replaceIllegalChars(t));
    appendCloseTag(n, buf);
  }
}
