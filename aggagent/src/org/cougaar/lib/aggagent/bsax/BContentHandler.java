package org.cougaar.lib.aggagent.bsax; 

import org.xml.sax.Locator;
import java.lang.String;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import java.util.Stack;
import java.util.Vector;
import java.util.Iterator;

public interface BContentHandler extends ContentHandler {


  public void clear();

  public Vector getRootElements();

  public Vector getALLElements();

 
}