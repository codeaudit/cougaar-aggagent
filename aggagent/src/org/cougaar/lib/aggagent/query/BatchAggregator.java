/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

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
