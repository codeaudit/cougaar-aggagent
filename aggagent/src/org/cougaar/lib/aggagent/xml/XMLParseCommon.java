
package org.cougaar.lib.aggagent.xml;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.*;

//
//  Common Helper functions associated with Aggregation Agent
//

public class XMLParseCommon
{
    public static StringBuffer filterXMLtoHTML(StringBuffer dataout)
    {
       int srcend = dataout.length();
       char csrc[] = new char[srcend];
       dataout.toString().getChars(0,srcend,csrc,0);

       // the return buffer (XML converted to HTML)
       StringBuffer buf = new StringBuffer(srcend);

       int i;
       int sz = dataout.length();
       for(i=0;i<sz;i++){
           char c = csrc[i];
           if( c == '<' ) buf.append("&lt");
           else if( c == '>' ) buf.append("&gt");
           else buf.append(c);
       }
       return buf;
    }

   /**
    static private String[] colorRange= {
       "000000", "FF0000", "EE0000", "DD0000", "CC0000", "BB0000", "AA0000",
       "990000", "880000", "770000", "660000", "550000", "440000", "330000", "220000",
       "110000",
       // REPEAT
       "000000", "FF0000", "EE0000", "DD0000", "CC0000", "BB0000", "AA0000",
       "990000", "880000", "770000", "660000", "550000", "440000", "330000", "220000",
       "110000",
       // REPEAT
       "000000", "FF0000", "EE0000", "DD0000", "CC0000", "BB0000", "AA0000",
       "990000", "880000", "770000", "660000", "550000", "440000", "330000", "220000",
       "110000"
    };


    //################################################################################
    // @param aliases:   cannot be null.  pass empty HashMap if no aliases.
    //                   if aliases defined, replace %TOKEN% with its alias (if exists)
    //
    public static StringBuffer filterXMLtoHTML_withIndentation(StringBuffer dataout,
                                                  HashMap aliases, boolean color_depth_coding)
    {
       StringBuffer buf = new StringBuffer();

       int depth=0;
       int i;
       int sz = dataout.length();
       //char prev_c = (char)-1;
       //char prev_prev_c = (char)-1;
       char c = (char)-1;
       char c_next_1 = (char)-1;
       char c_prev_1 = (char)-1;
       boolean indent=false;
       int num_opening_brackets =0;

       String  currToken = null;
       for(i=0;i<sz;i++)
       {
           c = dataout.charAt(i);
           if( (i-1)>=0)  { if( Character.isWhitespace(dataout.charAt(i-1)) == false) c_prev_1 = dataout.charAt(i-1); }
           if( (i+1)<sz ) { c_next_1 = dataout.charAt(i+1); }
           else c_next_1 =  (char)-1;

           // ###################################################
           // ALIAS TOKEN PROCESSING
           //
           if( c == '%' ) {
                if( currToken == null) {
                     // opening '%'
                     currToken = new String();
                }
                else {
                     // closing '%'
                     String alias = (String)aliases.get(currToken);
                     if( alias != null) {
                         buf.append(alias);
                     }
                     else {
                         buf.append("NULL_ALIAS");
                     }
                     currToken = null;
                }
           }
           else if( currToken != null ) {
               // accumulate token
               currToken += c;
           }
           else

           // ###################################################
           // XML PROCESSING
           //

           //  CASE:  "</"
           if( (c== '<') && (c_next_1 =='/')) {
                // if no value, indent, else keep value on same line
                if( c_prev_1 == '>') buf.append(indent(depth, color_depth_coding)+"&lt");
                else buf.append("&lt");
                depth--;  // after output
                i++;
                num_opening_brackets++;
           }
           else if( c == '<' ) {
                depth++;
                // if no value, OR FIRST '<' indent, else keep value on same line
                if( (c_prev_1 == '>') || (num_opening_brackets ==0)) buf.append(indent(depth, color_depth_coding)+"&lt");
                else buf.append("&lt");
                num_opening_brackets++;
           }
           // case: "/>"
           else if((c== '/') && (c_next_1 =='>')) {
                buf.append("/&gt\n" );
                if( color_depth_coding ){
                    buf.append("</FONT>");
                }
                i++;
                depth--;
                //indent =true;
           }
           else if( c == '>' ) {
                buf.append("&gt");  //System.out.print(" &gt ");
                if(c_next_1 == '<') buf.append("\n");  // case: no value, dont want carriage return
                if( color_depth_coding ){
                    buf.append("</FONT>");
                }
           }
           else {
               buf.append(c); //System.out.print(c);
           }
       }
       return buf;
    }
    
    //
    //
    //
    private static String indent(int depth, boolean color_depth_coding) {
        String buf= new String();
        for(int j=0; j<depth; j++){
           buf +="   ";
        }
        if( color_depth_coding) buf += "<FONT COLOR=" + colorRange[depth] + ">";
        return buf;
    }
    **/

    // @returns value
    public static String getAttributeOrChildNodeValue( String name, Node node ){
         Node n = getAttributeOrChildNode(name, node);
         if( n != null ){
             if( n.getNodeType() == n.ELEMENT_NODE ) {
                  Node n2 = n.getFirstChild();
                  return n2.getNodeValue();
             }
             return n.getNodeValue();
         }
         return null;
    }

    public static Node getAttributeOrChildNode( String name, Node node ){
       Node n = null;
       if( node.getAttributes() != null) n = node.getAttributes().getNamedItem(name);
       if( n == null )
       {
          NodeList nl = node.getChildNodes();
          //System.out.println(">>>>>>>>Node=" + node.getNodeName() + ", len children=" + nl.getLength());
          int size = nl.getLength();
          for(int i=0; i<size; i++) {
                Node n2 = nl.item(i);
                if( n2.getNodeName().toLowerCase().equals(name) ) {
                    /**
                    System.out.println("<<<<<<<<<<<<found n2=" + n2.getNodeName()
                                          + ", value=" + n2.getNodeValue()
                                          + ", type=" + n2.getNodeType()
                                           + ", cn.item(0)val=" + n2.getFirstChild().getNodeValue()
                                           +  ", cn.item(0)type=" + n2.getFirstChild().getNodeType()
                                           +  ", TEXT_NODE=" + n2.TEXT_NODE
                                           + ", ELEMENT_NODE=" + n2.ELEMENT_NODE
                                           + ", ENTITY_NODE=" + n2.ENTITY_NODE);
                    **/
                    return n2; //.getChildNodes().item(0);
                }
          }
       } else return n;
       return null;
    }


    public static Document getDocument(File file ){
        try{
                    FileReader fr = new FileReader(file);
                    InputSource is = new InputSource(fr);

                    DOMParser domp = new DOMParser();
                    domp.setErrorHandler(new ErrorHandler(){
                          public void error(SAXParseException exception) {
                             System.err.println("[ErrorHandler.error]: " + exception);
                           }
                           public void fatalError(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.fatalError]: " + exception);
                           }
                           public void warning(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.warning]: " + exception);
                            }
                        }
                    );

                    domp.parse(is);
                    Document doc = domp.getDocument();
                    return doc;
        } catch (Exception ex ) {
           ex.printStackTrace();
        }
        return null;
    }
}
