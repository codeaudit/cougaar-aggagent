package org.cougaar.lib.aggagent.dictionary;

import java.util.*;
import java.io.Serializable;
import java.io.OutputStream;
import org.cougaar.util.UnaryPredicate;
import java.util.Collection;
import java.util.List;

import org.cougaar.lib.planserver.HttpInput;


public interface GenericLogic
{
     public final String collectionType_ADD    = "ADD";
     public final String collectionType_REMOVE = "REMOVE";
     public final String collectionType_CHANGE = "CHANGE";

     public void init(Object key, Map params);

     public Object getParam(Object key);

     // Key to the GenericQuery instance, eg. name
     public Object getKey();

     // Generic PSP uses this predicate to subscribe to objects
     public UnaryPredicate getPredicate();

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // Query instances is responsible for accumulating state
     public void execute( Collection matches, final String collectionType );
}