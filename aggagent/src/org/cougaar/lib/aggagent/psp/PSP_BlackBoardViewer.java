/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.lib.aggagent.psp;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.apache.xalan.xpath.xml.TreeWalker;
import org.apache.xalan.xpath.xml.FormatterToXML;

import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.PlugInDelegate;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;

import org.cougaar.lib.aggagent.ldm.*;
import org.cougaar.lib.aggagent.Configs;
import org.cougaar.lib.aggagent.xml.XMLParseCommon;
import org.cougaar.lib.aggagent.xml.HTMLize;

/**
 * .<pre>
 * Simple illustration PSP which generates UIs for examining
 * and querying Aggregation Agent.
 * This PSP should only be "plugged into" an Aggregation Agent.
 **/

public class PSP_BlackBoardViewer extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber
{

   private String indent = "...........>" ;
   public UnaryPredicate getAllPredicate() {
         UnaryPredicate pred = new UnaryPredicate() {
                 public boolean execute(Object o) {
                     return true;
                }
         };
         return pred;
   }

  /**
   * A zero-argument constructor is required for dynamically loaded PSPs,
   * required by Class.newInstance()
   **/
  public PSP_BlackBoardViewer()
  {
    super();
  }

  public PSP_BlackBoardViewer(String pkg, String id)
    throws RuntimePSPException
  {
    setResourceLocation(pkg, id);
  }

  public boolean test(HttpInput query_parameters, PlanServiceContext sc)
  {
    super.initializeTest(); // IF subclass off of PSP_BaseAdapter.java
    return false;  // This PSP is only accessed by direct reference.
  }

  // ####################################################################
  public void execute(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu ) throws Exception
  {

      String mode =
         (String)query_parameters.getFirstParameterToken("MODE", '=');

      if( mode == null){
          out.println("<html>");
          out.println("<head>");
          out.println("</head>");
          out.println("<frameset cols=\"220,*\" frameborder=0 framespacing=0 border=0>");
          out.println("<frame src=\"BLACKBOARD.PSP?MODE=LEFT\" name=\"menuex\" scrolling=auto noresize>");
          out.println("<frame src=\"BLACKBOARD.PSP?MODE=RIGHT-DEFAULT\" name=\"dataex\" marginwidth=\"18\" scrolling=auto noresize>");
          out.println("</frameset>");
          out.println("</html>");
      } else if( mode.equals("LEFT") )
      {
          out.println("<html>");
          out.println("<body>");
          //out.println("<P>MENU...</P>");
          out.println(menu(psc.getServerPluginSupport().getClusterIDAsString()));
          out.println("</body>");
          out.println("</html>");

      } else if( mode.equals("RIGHT-DEFAULT") )
      {
          out.println("<html>");
          out.println("<body>");
          //out.println("RIGHT");
          out.println("</body>");
          out.println("</html>");

      } else if( mode.equals("RAW_BLACKBOARD") )
      {
          String ITEM_ID =
              (String)query_parameters.getFirstParameterToken("ITEM_ID", '=');

          out.println("<html>");
          out.println("<body><FONT COLOR=mediumblue>");
          if( ITEM_ID == null) raw_blackboard(out,query_parameters,psc,psu);
          else raw_blackboard_item(out,query_parameters,psc,psu, ITEM_ID);
          out.println("</font></body>");
          out.println("</html>");
      }

  }

  public String menu(String this_cluster_id){
       String str = new String();

       str += "<base TARGET=\"dataex\">";

       str += "<TABLE BORDER=\"0\" WIDTH=\"300\" CELLPADDING=\"2\" CELLSPACING=\"0\"  ALIGN=\"Left\"  VALIGN=\"TOP\">";
	     str += "<TR>";
       str += "<TD BGCOLOR=BLUE HEIGHT=\"22\"><FONT FACE=\"Arial\" SIZE=\"2\" COLOR=\"#FFFFFF\">";
       str += "<B>Agg Agent Blackboard</B></FONT>";
       str += "</TD></TR>";
       str += "<TR><TD BGCOLOR=\"#e9e9e9\"><FONT FACE=\"Arial\"SIZE=\"2\">";
             str += "<A href=\"?FILE?DOCUMENT=connxion.html\"> Connection Manager logs</A></TD></TR>";
       str += "<TR><TD BGCOLOR=\"#e9e9e9\"><FONT FACE=\"Arial\"SIZE=\"2\">";
             str += "<A href= \"BLACKBOARD.PSP?MODE=RAW_BLACKBOARD\">Blackboard</A></TD></TR>";
       str += "</TABLE>";

       return str;
  }

  //########################################################################
  public void raw_blackboard(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu ) throws Exception
  {
    out.println("<HTML><BODY><FONT COLOR=mediumblue>");
    out.println("<CENTER><H3>ClusterID: "
                        + psc.getServerPluginSupport().getClusterIDAsString()
                        + "</H3></CENTER>");

    out.println("<TABLE align=center border=1 cellPadding=1 cellSpacing=1");
    out.println("width=100% bordercolordark=#660000 bordercolorlight=#cc9966>");

    out.println("<TR>");
    out.println("<TD> <FONT color=mediumblue ><B>Blackboard</FONT></B> </TD>");
    out.println("</TR>");

    PlugInDelegate pd = psc.getServerPluginSupport().getDirectDelegate();
    Subscription subscription = psc.getServerPluginSupport().subscribe(this,
        new UnaryPredicate() { public boolean execute(Object o) { return true; }
        });
    Collection container = ((CollectionSubscription)subscription).getCollection();

    synchronized( container ) {

       Object [] objs = container.toArray();
       for( int i=0; i< objs.length; i++)
       {
          out.println("<TR><TD>");
          out.println("<TABLE align=center border=1 cellPadding=1 cellSpacing=1");
          out.println("width=100%>");
          Object obj = objs[i];
          display( obj, out, "");
          out.println("</Table>");
          out.println("</TD></TR>");
       }
    }

    out.println("</TABLE>");
    out.println("</BODY></HTML></FONT>");
  }

  //########################################################################
  public void raw_blackboard_item(
      PrintStream out,
      HttpInput query_parameters,
      PlanServiceContext psc,
      PlanServiceUtilities psu,
      String ITEM_ID) throws Exception
  {
    out.println("<HTML><BODY><FONT COLOR=mediumblue>");
    out.println("<CENTER><H3>"
                        + psc.getServerPluginSupport().getClusterIDAsString()
                        + "</H3></CENTER>");

    out.println("<TABLE align=center border=1 cellPadding=1 cellSpacing=1");
    out.println("width=100% bordercolordark=#660000 bordercolorlight=#cc9966>");

    out.println("<TR>");
    out.println("<TD> <FONT color=mediumblue ><B>Blackboard</FONT></B> </TD>");
    out.println("</TR>");

    PlugInDelegate pd = psc.getServerPluginSupport().getDirectDelegate();
    Subscription subscription = psc.getServerPluginSupport().subscribe(this,
        new UnaryPredicate() { public boolean execute(Object o) { return true; }
        });
    Collection container = ((CollectionSubscription)subscription).getCollection();

    synchronized( container ) {

       int id = Integer.parseInt(ITEM_ID);
       out.println("<TR><TD><FONT color=red >ITEM_ID=" + id + "</font></TD></TR>");

       Object [] objs = container.toArray();
       for( int i=0; i< objs.length; i++)
       {
          Object obj = objs[i];
          if( obj.hashCode() == id )
          {
             out.println("<TR><TD><FONT color=red >" + obj.getClass().getName() + "</FONT></TD></TR>");
             if( obj instanceof PlanObject)
             {
                 PlanObject p = (PlanObject)obj;
                 StringWriter sw = new StringWriter();
                 FormatterToXML fxml = new FormatterToXML(sw);
                 TreeWalker treew = new TreeWalker(fxml);
                 if( p.getDocument() != null)
                 {
                     treew.traverse(p.getDocument());
                     StringBuffer sb = sw.getBuffer();
                     StringBuffer html = HTMLize.layoutXML(sb, new HashMap(), true);
                     out.println("<TR><TD> <FONT color=red >ALL VALUES=</FONT><font SIZE=-2><blockquote><pre>"
                                 + html.toString() + "</pre></blockquote></FONT></TD></TR>");
                 } else
                 {
                     out.println("<TR><TD> <FONT color=red >ALL VALUES=</FONT><font SIZE=-2><blockquote><pre>"
                                + "NULL Document FOUND with PlanObject"
                                + "</pre></blockquote></FONT></TD></TR>");
                     System.err.println("NULL Document FOUND with PlanObject");
                 }
             }
             else if( obj instanceof HTMLPlanObject)
             {
                 HTMLPlanObject p = (HTMLPlanObject)obj;
                 StringBuffer sb = p.getDocument();
                 StringBuffer html = HTMLize.layoutXML(sb, new HashMap(), true);
                 out.println("<TR><TD> <FONT color=red >ALL VALUES=</FONT><font SIZE=-2><blockquote><pre>"
                                 + html.toString() + "</pre></blockquote></FONT></TD></TR>");
             }
             break;
          }
       }
    }
    out.println("</TABLE>");
    out.println("</BODY></HTML></FONT>");
  }


  public void display(Iterator it, PrintStream stream, String prefix){
    while( it.hasNext() ){
       Object obj = it.next();
       display( obj, stream, prefix );
    }
  }

  /**
  **/
  public void display( Object obj, PrintStream stream, String prefix )
  {
     stream.println( "<TR><A HREF=?MODE=RAW_BLACKBOARD?ITEM_ID=" + obj.hashCode() + ">" + prefix + "[" 
                     + "(<font color=blue size=-2>" + obj.toString() + "</font>)"
                     + "]" + "</A></TR>" );

     if( obj instanceof PlanObject ){
         PlanObject p = (PlanObject)obj;
         Document doc = p.getDocument();
         stream.println("<UL>");
         if( doc == null ) {  stream.println("<LI>doc=NULL");  }
         else {
             stream.println("<LI>doc.root.nodename=" + doc.getDocumentElement().getNodeName());
             NodeList nl= doc.getDocumentElement().getChildNodes();
             int size = nl.getLength();
             stream.println("<font color=blue size=-2><UL>");
             for(int i =0; i<size; i++) {
                 Node n = (Node)nl.item(i);
                 stream.println("<LI>node=" + n.getNodeName() );
             }
             stream.println("</UL></font>");
         }
         stream.println("</UL>");
     }
  }

  public String getClass(Object obj){
     String str = new String("{<FONT COLOR=GREEN>");
     Class c[] = obj.getClass().getInterfaces();
     int sz = c.length;
     for(int i = 0; i< sz; i++){
        Class c2 = (Class)c[i];
        str += c2.getName();
        if( (i+1)<sz) str += ",";
     }
     str += "</FONT> (<FONT SIZE=-2>" + obj.getClass().getName() + "<FONT>)";
     return str + "}";
  }


  /**
   * A PSP can output either HTML or XML (for now).  The server
   * should be able to ask and find out what type it is.
   **/
  public boolean returnsXML() {
    return false;
  }

  public boolean returnsHTML() {
    return true;
  }

  /**  Any PlanServiceProvider must be able to provide DTD of its
   *  output IFF it is an XML PSP... ie.  returnsXML() == true;
   *  or return null
   **/
  public String getDTD()  {
    return null;
  }


    public void subscriptionChanged(Subscription subscription) {

    }
}
