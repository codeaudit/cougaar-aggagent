
package   org.cougaar.lib.aggagent.dictionary;


import java.util.*;

import org.cougaar.lib.aggagent.dictionary.GenericLogic;
import org.cougaar.lib.planserver.HttpInput;


//
// Base Class For Dictionaries.
//
public class DictionaryBase
{
      public final static String NOVALUE = "NOVALUE";

      public static Map parseURLArgs(HttpInput hi )
      {
           Map hash = Collections.synchronizedMap(new HashMap());

           Vector v = hi.getURLParameters();
           Iterator it = v.iterator();
           while(it.hasNext()){
               String arg =     (String)it.next();
               String keyword = null;
               String value   = null;
               int idx;
               if( (idx=arg.indexOf("=")) >= 0 ) {
                   keyword = arg.substring(0,idx);
                   value= arg.substring(idx+1);
               } else {
                   keyword = arg;
               }
               //System.out.println("[GenericReaderWriterEngine.parseURLArgs] > " + keyword + "," + value);
               if( value != null) hash.put(keyword, value);
               else hash.put(keyword,NOVALUE);
           }
           return hash;
      }


      public static void printURLArgs(Map map){
           int count_keys_wout_values =0;
           Iterator it = map.keySet().iterator();
           while(it.hasNext()){
              String key = (String)it.next();
              String val = (String)map.get(key);
              System.out.println("[GenericReaderWriterEngine.printURLArgs()] > " + key + ", " + val);
           }
      }
}