
package org.cougaar.lib.aggagent.query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  An implementation of the Aggregator interface that behavior most likely to
 *  be used by developers.  In particular, a series of keys can be used to
 *  gather atoms into affinity classes, and a DataAtomMelder (q.v.) is used to
 *  meld the affinity classes into atoms.
 */
public class BatchAggregator implements Aggregator {
  private List aggIds = null;
  private DataAtomMelder melder = null;

  /**
   *  Create a new BatchAggregator.  A list of ids is used to collate a result
   *  set, and a DataAtomMelder is used to meld collections of atoms into the
   *  aggregated result set.
   */
  public BatchAggregator (List ids, DataAtomMelder m) {
    aggIds = ids;
    melder = m;
  }

  /**
   *  Transform the raw result set into an aggregated result set.
   */
  public void aggregate (Iterator dataAtoms, List output) {
    Map batches = collate(dataAtoms);
    for (Iterator i = batches.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry e = (Map.Entry) i.next();
      melder.meld(aggIds, (CompoundKey) e.getKey(), (List) e.getValue(), output);
    }
  }

  private Map collate (Iterator atoms) {
    Map ret = new HashMap();
    while (atoms.hasNext()) {
      ResultSetDataAtom a = (ResultSetDataAtom) atoms.next();
      CompoundKey k = a.getKey(aggIds);
      List l = (List) ret.get(k);
      if (l == null)
        ret.put(k, l = new LinkedList());
      l.add(a);
    }
    return ret;
  }
}
