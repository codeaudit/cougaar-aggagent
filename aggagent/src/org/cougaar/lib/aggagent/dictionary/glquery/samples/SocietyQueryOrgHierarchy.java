package org.cougaar.lib.aggagent.dictionary.glquery.samples;


import java.io.Serializable;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileReader;


import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.AssetGroup;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.RelationshipSchedule;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.domain.glm.ldm.asset.Organization;
import org.cougaar.domain.glm.ldm.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import org.cougaar.lib.aggagent.ldm.PlanObject;



public class SocietyQueryOrgHierarchy extends CustomQueryBaseAdapter
{
     private List myStuff = new ArrayList();

     //
     // Generic PSP calls this method on Collection of objects answering subscription
     // GenericQueryXML accumulates state
     public void execute( Collection matches ) {

           synchronized( matches)
           {
              //Iterator it = matches.iterator();
              Object [] objs = matches.toArray();

              for(int i=0; i< objs.length; i++)
              {
                   Object obj = objs[i];
                   // save it.
                   myStuff.add(obj);
              }
           }
     }

     //
     public void returnVal( OutputStream out ) {
          System.out.println("[SocietyQueryOrgHierarchy] called." );
          PrintWriter pw = new PrintWriter(out);
          pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
          pw.print("<collection>");

          synchronized( myStuff ){

             //Iterator iter =myStuff.iterator();
             Object [] objs = myStuff.toArray();

             for(int i=0; i< objs.length; i++)
             {
                 Asset asst = (Asset)objs[i];
                 //Asset asst = (Asset)iter.next();
                 Organization org = (Organization)asst;

                 String orgName = org.getItemIdentificationPG().getNomenclature();

                 RelationshipSchedule schedule = org.getRelationshipSchedule();

                 Object[] rsobjs = schedule.toArray();

                 for (int j=0; j< rsobjs.length; j++ )
                 {
                      Relationship relationship = (Relationship)rsobjs[j];

                      // We already know it's the SELF org
                      if ((relationship.getRoleA().equals(Constants.Role.SELF)) ||
                         (relationship.getRoleB().equals(Constants.Role.SELF))) {
                           continue;
                      }

                      Object otherObject = schedule.getOther(relationship);
                      String other =
                         (otherObject instanceof Asset) ?
                           ((Asset)otherObject).getItemIdentificationPG().getNomenclature() :
                                otherObject.toString();

                      pw.print("<Cluster ID=\"" + orgName.trim() + "\">");
                      pw.print("<other>"+
                          other.trim()+
                          "</other><relationship>"+
                          schedule.getOtherRole(relationship)+
                          "</relationship>");
                      pw.print("</Cluster>");
                 }
             } // end -for objs
          } // end - synchronized

          pw.print("</collection>");
          pw.flush();
     }
}


