package org.cougaar.lib.aggagent.ldm;



import java.util.Collection;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.cougaar.domain.planning.ldm.plan.Plan;

/**
  *  Blackboard Wrapper for XML DATA (in DOM Document form)
  **/
public class PlanObject implements Plan
{
     private Document myDocument = null;
     //private DocumentSignature myDocSig = null;

     public PlanObject( Document d ){
         myDocument = d;
         //myDocSig = new DocumentSignature(d);
     }

     public Document getDocument( ){
         return myDocument;
     }

     public String XMLize(){
         return null;
     }

     public String getPlanName() { return "AGG"; }
}
