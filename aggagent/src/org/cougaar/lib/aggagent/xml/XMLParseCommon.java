
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
                if( n2.getNodeName().toLowerCase().equals(name.toLowerCase()) )
                {
                    /**
                    if( n2.getFirstChild().getNodeType() == n2.TEXT_NODE ){
                        System.out.println("++++++++++++++++++++++++++++++++++++++++++> TEXT NODE FOuND:" + name);
                    }
                    else   if( n2.getFirstChild().getNodeType() == n2.COMMENT_NODE ){
                        System.out.println("++++++++++++++++++++++++++++++++++++++++++> COMMENT NODE FOuND:" + name);
                    }
                    else   if( n2.getFirstChild().getNodeType() == n2.ELEMENT_NODE ){
                        System.out.println("++++++++++++++++++++++++++++++++++++++++++> ELEMENT NODE FOuND:" + name);
                    }
                    **/
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
                } else {
                    //System.out.println("+++++++++++++++++++++++++++++ NODE IGNORED: " + n2.getNodeName() + ", looking=" + name);
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
