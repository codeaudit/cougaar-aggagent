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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompoundKey implements Serializable {
  private String[] keys = null;
  private String stringVal = null;

  public CompoundKey (String[] k) {
    keys = new String[k.length];
    System.arraycopy(k, 0, keys, 0, k.length);
    makeStringVal();
  }

  public CompoundKey (List l, Map m) {
    keys = new String[l.size()];
    Iterator i = l.iterator();
    for (int j = 0; i.hasNext(); j++)
      keys[j] = m.get(i.next()).toString();

    makeStringVal();
  }

  private void makeStringVal () {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < keys.length; i++) {
      buf.append("#");
      buf.append(keys[i]);
    }
    stringVal = buf.toString();
  }

  public String toString () {
    return stringVal;
  }

  public int hashCode () {
    return stringVal.hashCode();
  }

  public boolean equals (Object obj) {
    CompoundKey other = null;
    if (obj instanceof CompoundKey &&
        (other = (CompoundKey) obj).keys.length == keys.length)
    {
      for (int i = 0; i < keys.length; i++)
        if (!keys[i].equals(other.keys[i]))
          return false;

      return true;
    }
    return false;
  }

  public Iterator getKeys () {
    return new KeyIterator();
  }

  private class KeyIterator implements Iterator {
    private int index = 0;

    public boolean hasNext () {
      return index < keys.length;
    }

    public Object next () {
      return keys[index++];
    }

    public void remove () {
    }
  }
}
