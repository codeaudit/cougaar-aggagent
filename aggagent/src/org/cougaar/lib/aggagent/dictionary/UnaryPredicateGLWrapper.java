/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package   org.cougaar.lib.aggagent.dictionary;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.lib.aggagent.ldm.PlanObject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

////////////////////////////////////////////////////////////////////////////
interface UnaryPredicateGLWrapper extends UnaryPredicate {

    /**
      * for diagnostic "reflection" of what UnaryPredicate instnace does --
      * allow to attach arbitrary text
      **/
    public String getAnnotation(); 
}


