/*
 * GetTestRelayPredicate.java
 *
 * Created on April 15, 2002, 1:01 PM
 */

package org.cougaar.lib.aggagent.test;

/**
 *
 * @author  wwright
 */
public class GetTestRelayPredicate implements org.cougaar.util.UnaryPredicate {
    
    /** Creates a new instance of GetTestRelayPredicate */
    public GetTestRelayPredicate() {
    }
    
    /** @return true iff the object "passes" the predicate  */
    public boolean execute(Object o) {
        return o instanceof TestRelay;
    }
    
}
