/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.lib.aggagent.ldm;

/**
 *  Contain the unparsed text of an XML document as a String.  The purpose of
 *  this class is to encapsulate Strings as they are published on the logplan.
 */
public class XMLPlanObject {
   private String text = null;

   /**
    *  Create a new XMLPlanObject to house the provided XML String
    *  @param xmlData the unparsed XML document
    */
   public XMLPlanObject (String xmlData) {
     text = xmlData;
   }

   /**
    *  Retrieve the contained XML document for the caller.
    *  @return the unparsed text of an XML document
    */
   public String getDocument () {
     return text;
   }
}
