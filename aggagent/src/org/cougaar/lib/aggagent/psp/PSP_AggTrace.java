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

/**
 * .<pre>
 * Simple illustration PSP which generates UIs for examining
 * and querying Aggregation Agent.
 * This PSP should only be "plugged into" an Aggregation Agent.
 **/

public class PSP_AggTrace extends PSP_BaseAdapter implements PlanServiceProvider, UISubscriber
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
  public PSP_AggTrace()
  {
    super();
  }

  public PSP_AggTrace(String pkg, String id)
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
          out.println("<frame src=\"TRACE.PSP?MODE=LEFT\" name=\"menuex\" scrolling=auto noresize>");
          out.println("<frame src=\"TRACE.PSP?MODE=RIGHT-DEFAULT\" name=\"dataex\" marginwidth=\"18\" scrolling=auto noresize>");
          out.println("</frameset>");
          out.println("</html>");
      } else if( mode.equals("LEFT") )
      {
          out.println("<html>");
          out.println("<body>");
          //out.println("<P>MENU...</P>");
          out.println(menuApplet(psc.getServerPluginSupport().getClusterIDAsString()));
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

  public String menuApplet(String this_cluster_id){
       String str = new String();
       //str += "<APPLET CODEBASE = \"rtreed\" CODE = \"MSBTree.RTreeApplet.class\"";
       //str += " NAME     = \"TestApplet\" ARCHIVE  = \"RTreed.zip\"";

       str += "<APPLET  CODE = \"MSBTree.RTreeApplet.class\"";
       str += " NAME     = \"TestApplet\" ";
       str += " WIDTH    = 250 HEIGHT   = 425 HSPACE   = 0 VSPACE   = 0 ALIGN    = middle >";
       str += "<PARAM NAME = \"TREE_BORDER\" VALUE = \"SHADOW\">";
       str += "<PARAM NAME = \"TREE_PAGE_BACK_COLOR\" VALUE = \"WHITE\">";
       str += "<PARAM NAME = \"TREE_BACK_IMAGE\" VALUE = \"cougaar_blank_logo.gif\">";
       str += "<PARAM NAME = \"TREE_TIP_DELAY\" VALUE = \"0\">";
       str += "<PARAM NAME = \"TREE_LOADING_IMAGE\" VALUE = \"info.gif\">";

       str += "<PARAM NAME = \"NODE_RAISED\" VALUE = \"Y\">";
       str += "<PARAM NAME = \"SHOW_ROOT\" VALUE = \"Y\">";
       str += "<PARAM NAME = \"NODE_ICON\" VALUE = \"internet.gif\">";

       ///////////////////////////////////////////////////////////

       str += "<PARAM NAME = \"ROOT_TEXT\" VALUE = \"Links\">";
       str += "<PARAM NAME = \"ROOT_EXPANDED_ICON\" VALUE = \"home.gif\">";
       str += "<PARAM NAME = \"ROOT_ICON\" VALUE = \"home.gif\">";


       str += "<PARAM NAME = \"ROOT_1\" VALUE = \"Admin\">";
       str += "<PARAM NAME = \"Admin_TEXT\" VALUE = \"Admin\">";
       str += "<PARAM NAME = \"Admin_ICON\" VALUE = \"index.gif\">";


       str += "<PARAM NAME = \"ADMIN_1\" VALUE= \"Example0\">";
       str += "<PARAM NAME = \"EXAMPLE0_TEXT\" VALUE= \"Connect Log\">";
       str += "<PARAM NAME = \"EXAMPLE0_LINK\" VALUE= \"?FILE?DOCUMENT=connxion.html\">";
       str += "<PARAM NAME = \"EXAMPLE0_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"ADMIN_2\" VALUE= \"Example1\">";
       str += "<PARAM NAME = \"EXAMPLE1_TEXT\" VALUE= \"Connect Config\">";
       //str += "<PARAM NAME = \"EXAMPLE1_LINK\" VALUE= \"?FILE?DOCUMENT=aggregator.configs.xml\">";
       str += "<PARAM NAME = \"EXAMPLE1_LINK\" VALUE= \"CONFIG_READER.PSP?CONFIGFILE=aggregator.configs.xml\">";
       str += "<PARAM NAME = \"EXAMPLE1_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"ADMIN_3\" VALUE= \"Example2\">";
       str += "<PARAM NAME = \"EXAMPLE2_TEXT\" VALUE= \"AGG GU's loaded\">";
       str += "<PARAM NAME = \"EXAMPLE2_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP\">";
       str += "<PARAM NAME = \"EXAMPLE2_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"ADMIN_4\" VALUE= \"Example3\">";
       str += "<PARAM NAME = \"EXAMPLE3_TEXT\" VALUE= \"3ID GU's loaded\">";
       str += "<PARAM NAME = \"EXAMPLE3_LINK\" VALUE= \"http://localhost:5555/$3ID/alpine/demo/GENERIC.PSP\">";
       str += "<PARAM NAME = \"EXAMPLE3_TARGET\" VALUE= \"dataex\">";

       //
       // If this PSP at AGG cluster, show AGG cluster glprimitives xml config file...
       // This PSP can't run effectively on Society clusters so cannot view their glprimitives xml
       //
       if(  this_cluster_id.indexOf(Configs.AGGREGATION_CLUSTER_NAME_PREFIX) > -1 ) {
           str += "<PARAM NAME = \"ADMIN_5\" VALUE= \"Example4\">";
           str += "<PARAM NAME = \"EXAMPLE4_TEXT\" VALUE= \"AGG GL Script\">";
           //str += "<PARAM NAME = \"EXAMPLE2_LINK\" VALUE= \"?FILE?DOCUMENT=glprimitives.agg.cluster.xml\">";
           str += "<PARAM NAME = \"EXAMPLE4_LINK\" VALUE= \"CONFIG_READER.PSP?CONFIGFILE=glprimitives.agg.cluster.xml\">";
           str += "<PARAM NAME = \"EXAMPLE4_TARGET\" VALUE= \"dataex\">";
        }



       //////////////////////////////////////////////////////////////////


       str += "<PARAM NAME = \"ROOT_2\" VALUE = \"BlackboardQueries\">";
       str += "<PARAM NAME = \"BLACKBOARDQUERIES_TEXT\" VALUE = \"BlackboardQueries\">";
       str += "<PARAM NAME = \"BLACKBOARDQUERIES_ICON\" VALUE = \"index.gif\">";


       str += "<PARAM NAME = \"BLACKBOARDQUERIES_1\" VALUE= \"Example10\">";
       str += "<PARAM NAME = \"EXAMPLE10_TEXT\" VALUE= \"Raw-Catalog\">";
       str += "<PARAM NAME = \"EXAMPLE10_LINK\" VALUE= \"TRACE.PSP?MODE=RAW_BLACKBOARD\">";
       str += "<PARAM NAME = \"EXAMPLE10_TARGET\" VALUE= \"dataex\">";

       //str += "<PARAM NAME = \"BLACKBOARDQUERIES_2\" VALUE= \"Example11\">";
       //str += "<PARAM NAME = \"EXAMPLE11_TEXT\" VALUE= \"Tasks\">";
       //str += "<PARAM NAME = \"EXAMPLE11_LINK\" VALUE= \"TRACE.PSP?MODE=TASKS_BLACKBOARD\">";
       //str += "<PARAM NAME = \"EXAMPLE11_TARGET\" VALUE= \"dataex\">";


       /////////////////////////////////////////////////////////////

       str += "<PARAM NAME = \"ROOT_3\" VALUE = \"References\">";
       str += "<PARAM NAME = \"REFERENCES_TEXT\" VALUE = \"References\">";
       str += "<PARAM NAME = \"REFERENCES_ICON\" VALUE = \"index.gif\">";


       str += "<PARAM NAME = \"REFERENCES_1\" VALUE= \"Example20\">";
       str += "<PARAM NAME = \"EXAMPLE20_TEXT\" VALUE= \"Cougaar\">";
       str += "<PARAM NAME = \"EXAMPLE20_LINK\" VALUE= \"http://www.cougaar.org\">";
       str += "<PARAM NAME = \"EXAMPLE20_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"REFERENCES_2\" VALUE= \"Example21\">";
       str += "<PARAM NAME = \"EXAMPLE21_TEXT\" VALUE= \"DAML\">";
       str += "<PARAM NAME = \"EXAMPLE21_LINK\" VALUE= \"http://www.daml.org\">";
       str += "<PARAM NAME = \"EXAMPLE21_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"REFERENCES_3\" VALUE= \"Example22\">";
       str += "<PARAM NAME = \"EXAMPLE22_TEXT\" VALUE= \"AAI\">";
       str += "<PARAM NAME = \"EXAMPLE22_LINK\" VALUE= \"http://aai.bbn.com/cougaar\">";
       str += "<PARAM NAME = \"EXAMPLE22_TARGET\" VALUE= \"dataex\">";


       //////////////////////////////////////////////////////////////////


       str += "<PARAM NAME = \"ROOT_4\" VALUE = \"CannedQueries\">";
       str += "<PARAM NAME = \"CANNEDQUERIES_TEXT\" VALUE = \"CannedQueries\">";
       str += "<PARAM NAME = \"CANNEDQUERIES_ICON\" VALUE = \"index.gif\">";

       str += "<PARAM NAME = \"CANNEDQUERIES_1\" VALUE= \"Example30\">";
       str += "<PARAM NAME = \"EXAMPLE30_TEXT\" VALUE= \"Simple Tasks (1)(HTML)\">";

       //str += "<PARAM NAME = \"EXAMPLE30_LINK\" VALUE= \"CONFIG_READER.PSP?CONFIGURL=http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_TASK\">";
       str += "<PARAM NAME = \"EXAMPLE30_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_TASK?HTML\">";
       str += "<PARAM NAME = \"EXAMPLE30_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"CANNEDQUERIES_2\" VALUE= \"Example31\">";
       str += "<PARAM NAME = \"EXAMPLE31_TEXT\" VALUE= \"Simple Assets (1)(HTML)\">";
       str += "<PARAM NAME = \"EXAMPLE31_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_ASSET?HTML\">";
       str += "<PARAM NAME = \"EXAMPLE31_TARGET\" VALUE= \"dataex\">";

       /**
       str += "<PARAM NAME = \"CANNEDQUERIES_3\" VALUE= \"Example32\">";
       str += "<PARAM NAME = \"EXAMPLE32_TEXT\" VALUE= \"AGG GU's\">";
       str += "<PARAM NAME = \"EXAMPLE32_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP\">";
       str += "<PARAM NAME = \"EXAMPLE32_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"CANNEDQUERIES_4\" VALUE= \"Example33\">";
       str += "<PARAM NAME = \"EXAMPLE33_TEXT\" VALUE= \"3ID GU's\">";
       str += "<PARAM NAME = \"EXAMPLE33_LINK\" VALUE= \"http://localhost:5555/$3ID/alpine/demo/GENERIC.PSP\">";
       str += "<PARAM NAME = \"EXAMPLE33_TARGET\" VALUE= \"dataex\">";
       **/

       str += "<PARAM NAME = \"CANNEDQUERIES_3\" VALUE= \"Example32\">";
       str += "<PARAM NAME = \"EXAMPLE32_TEXT\" VALUE= \"Simple Tasks (1)(XML)\">";
       //str += "<PARAM NAME = \"EXAMPLE30_LINK\" VALUE= \"CONFIG_READER.PSP?CONFIGURL=http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_TASK\">";
       str += "<PARAM NAME = \"EXAMPLE32_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_TASK\">";
       str += "<PARAM NAME = \"EXAMPLE32_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"CANNEDQUERIES_4\" VALUE= \"Example33\">";
       str += "<PARAM NAME = \"EXAMPLE33_TEXT\" VALUE= \"Simple Assets (1)(XML)\">";
       str += "<PARAM NAME = \"EXAMPLE33_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_SIMPLE_ASSET\">";
       str += "<PARAM NAME = \"EXAMPLE33_TARGET\" VALUE= \"dataex\">";

       str += "<PARAM NAME = \"CANNEDQUERIES_5\" VALUE= \"Example34\">";
       str += "<PARAM NAME = \"EXAMPLE34_TEXT\" VALUE= \"Org. Hierarchy (XML)\">";
       str += "<PARAM NAME = \"EXAMPLE34_LINK\" VALUE= \"http://localhost:5555/$AGG/agg/demo/GENERIC.PSP?QUERY_ORG_HIERARCHY\">";
       str += "<PARAM NAME = \"EXAMPLE34_TARGET\" VALUE= \"dataex\">";

       str += "</APPLET>";

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
    out.println("<CENTER><H3>"
                        + psc.getServerPluginSupport().getClusterIDAsString()
                        + "</H3></CENTER>");

    out.println("<TABLE align=center border=1 cellPadding=1 cellSpacing=1");
    out.println("width=100% bordercolordark=#660000 bordercolorlight=#cc9966>");

    out.println("<TR>");
    out.println("<TD> <FONT color=mediumblue ><B>Trace</FONT></B> </TD>");
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
    out.println("<TD> <FONT color=mediumblue ><B>Trace</FONT></B> </TD>");
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
                 treew.traverse(p.getDocument());
                 StringBuffer sb = sw.getBuffer();
                 StringBuffer html = XMLParseCommon.filterXMLtoHTML(sb);
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
