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
package org.cougaar.lib.aggagent.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  The XmlUtils class is the home of a suite of static convenience methods for
 *  handling XML documents.  Support for parsing, traversing, and generating
 *  these documents may be found here.
 */
public class XmlUtils {
  /**
   * when debug is set to true, all XML is printed to standard out before it
   * is parsed.
   */
  public static boolean debug = false;

  /**
   *  This convenience method searches for a child Element and attempts to
   *  parse its content text as an integer.  An Exception will be raised if
   *  no such Element exists, or else one exists, but its contents cannot be
   *  intrepreted as an integer.  If the Element has more than one child with
   *  the given name, then the first one is used (even if the contents are
   *  invalid).
   */
  public static int getChildInt (Element elt, String tag)
      throws NumberFormatException
  {
    return Integer.parseInt(getChildText(elt, tag));
  }

  /**
   *  Search for an Element of the given name that is a child of the specified
   *  Element.  If any such Elements exist, the first is returned; otherwise,
   *  the method returns null.  Note:  the search will return only direct
   *  children, as opposed to higher-order descendants (e.g., grandchildren).
   */
  public static Element getChildElement (Element elt, String tag) {
    NodeList nl = elt.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(tag))
        return (Element) n;
    }
    return null;
  }

  /**
   *  This convenience method extracts the text content of a child Element
   *  whose name is specified by the caller.  Assuming such an Element exists,
   *  its contents are handled as in getElementText (q.v.).  If there are many
   *  child Elements matching the description, the first one is used.  If there
   *  are no such Elements, then this method returns null.
   */
  public static String getChildText (Element elt, String tag) {
    return getElementText(getChildElement(elt, tag));
  }

  /**
   *  This convenience method extracts the text content of an XML element
   *  (including white spaces).  If the Element has child Elements, they are
   *  ignored, and the intervening Text nodes are concatenated into the result.
   *  This method will return null if and only if it is passed a null Element.
   */
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

  /**
   *  Parse an XML document and return the "document element", which is the
   *  root of the XML structure as specified in the text of the document.  For
   *  this method, the text must come from a String.
   */
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

  /**
   *  Parse an XML document and return the "document element", which is the
   *  root of the XML structure as specified in the text of the document.  For
   *  this method, the text must come from an InputStream.
   */
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

  /**
   *  Parse an XML document and return the "document element", which is the
   *  root of the XML structure as specified in the text of the document.  By
   *  and large, this method will be called by other parse methods after
   *  converting the source of the XML text into an InputSource.
   */
  public static Element parse (InputSource in) throws IOException, SAXException
  {
    // We believe that all the XML will be well-formed, so this method should
    // always succeed.
    DOMParser p = new DOMParser();
    p.parse(in);
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
      if (debug)
      {
        System.out.println("requestString:");
        System.out.println("     url: " + urlString);
        System.out.println("     request: " + request);
      }
      InputStream is = sendRequest(urlString, request);
      response = readToString(is);
      if (debug)
      {
        System.out.println("     response: " + response);
      }
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
    ((HttpURLConnection)uc).setRequestMethod("PUT");
    uc.setDoInput(true);
    uc.setDoOutput(true);
    OutputStream os = uc.getOutputStream();
    PrintStream servicePrint = new PrintStream(os);

    // send request
    if (request != null)
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
}