/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

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