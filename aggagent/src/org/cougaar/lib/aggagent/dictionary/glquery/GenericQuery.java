package org.cougaar.lib.aggagent.dictionary.glquery;

import java.io.Serializable;
import java.io.OutputStream;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.List;

import org.cougaar.lib.aggagent.dictionary.GenericLogic;


/** @param myxsl_alias : (optional).  If specified, look up XSL using this key.
  *                       if null, use default associated with this Entry
  *                       if defined, otherwise no xsl.
  **/
public interface GenericQuery extends GenericLogic, Serializable
{
     //
     // this keys are used to locate input data, configuration which
     // parameterizes this query.
     //
     public final String paramkey_XML_SERVICE = "XML_SERVICE";
     public final String paramkey_PREDICATE = "PREDICATE";
     public final String paramkey_XSL = "XSL";
     public final String paramkey_SAX = "SAX";

     // Obtain parameter data
     public Object getParam(Object key);


     // Returns current snapshot (in XML Document form) of current state of this
     // Query instance -- implicitly flushes state
     //
     public void returnVal( OutputStream out);
}
