
package org.cougaar.lib.aggagent.query;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompoundKey {
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
