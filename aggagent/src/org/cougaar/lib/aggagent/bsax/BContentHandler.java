/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
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