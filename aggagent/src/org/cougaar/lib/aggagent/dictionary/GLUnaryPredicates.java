package   org.cougaar.lib.aggagent.dictionary;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.lib.aggagent.ldm.PlanObject;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.planning.ldm.plan.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class GLUnaryPredicates
{

      public static UnaryPredicate getInstanceOfPredicate(final String pred_test_klass_name ) {
         UnaryPredicate pred = null;
         try{
             final Class klass = Class.forName(pred_test_klass_name);

             pred = new UnaryPredicateGLWrapper() {

                   //////////////////////////////////////
                   public boolean execute(Object o) {
                     //System.out.println("Klass=" + klass);
                      if (klass.isInstance(o)) {
                         return true;
                      }
                      return false;
                   }
                   //////////////////////////////////////
                    public String getAnnotation() {
                       return
                          "\tpublic boolean execute(Object o) {\n"
                          + "\t    if (" + pred_test_klass_name + ".isInstance(o)) {\n"
                          + "\t       return true;\n"
                          + "\t     }\n"
                          + "\t     return false;\n"
                          + "\t  }\n";
                    }
                 };
         } catch (Exception ex ) {
             ex.printStackTrace();
         }

         return pred;
       }

    //   public static UnaryPredicate getInstanceOfPredicate_workedok(final String verb ) {
    public static UnaryPredicate getTaskWithVerbPredicate(final String verb ) {
         UnaryPredicate pred = null;
         try{

             pred = new UnaryPredicateGLWrapper() {
                   //////////////////////////////////////
                   public boolean execute(Object o) {
                      if (o instanceof Task) {
			                  Task t=(Task)o;
                        System.out.println("getTaskWithVerbPredicate verb=" + verb);
                        Verb target = Verb.getVerb(verb);
                        if( target == null) return false;
			                  else if (t.getVerb().equals(target)) {
			                    return true;
			                  }
                      }
                      return false;
                   }
                   //////////////////////////////////////
                    public String getAnnotation() {
                       return
                          "\tpublic boolean execute(Object o) {\n"
                          + "\t    if (o instanceof Task && Task has verb of "+verb+") {\n"
                          + "\t       return true;\n"
                          + "\t     }\n"
                          + "\t     return false;\n"
                          + "\t  }\n";
                    }
                 };
         } catch (RuntimeException ex ) {
             ex.printStackTrace();
         }

         return pred;
       }


       /**
         * Predicate is true if existance of node tag name in PlanObject DOM
         * Note that this predicate will test all nodes, not just top-level ones
         * So if use this Predicate need to be sensitive to false-positives... eg.
         * finding a "Task" tag of a data field vs. "Task" element
        **/
       public static UnaryPredicate getDOMNodeNamePredicate(final String nodetagname ) {

               return new UnaryPredicateGLWrapper() {

                    //////////////////////////////////////
                    public boolean execute (Object obj ) {
                          if( obj instanceof PlanObject ) {
                               Document doc = ((PlanObject)obj).getDocument();
                               int len = doc.getElementsByTagName(nodetagname).getLength();
                               return (len > 0);
                          }
                          return false;
                    }
                    //////////////////////////////////////

                    public String getAnnotation() {
                       return
                       "\tpublic boolean execute (Object obj ) {\n"
                       + "\t  if( obj instanceof PlanObject ) {\n"
                       + "\t        Document doc = ((PlanObject)obj).getDocument();\n"
                       + "\t        int len = doc.getElementsByTagName(" + nodetagname + ").getLength();\n"
                       + "\t        return (len > 0);\n"
                       + "\t   }\n"
                       + "\t   return false;\n"
                       + "\t}\n";
                    }
               };
       }
}
