package org.cougaar.lib.aggagent.ldm;



import java.util.Collection;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.cougaar.domain.planning.ldm.plan.Plan;

/**
  *  Blackboard Wrapper for HTML DATA (in StringBuffer form)
  **/
public class HTMLPlanObject implements Plan
{
     private StringBuffer myHTML = null;
     private DocumentSignature myDocSig = null;

     public HTMLPlanObject( StringBuffer htmlData){
         myHTML = htmlData;
     }

     public StringBuffer getDocument( ){
         return myHTML;
     }

     public String XMLize(){
         return null;
     }

     public String getPlanName() { return "AGG"; }
}