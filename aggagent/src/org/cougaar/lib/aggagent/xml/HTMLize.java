package org.cougaar.lib.aggagent.xml;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Color;

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

public class HTMLize
{
     /**
       * Takes XML, Returns HTML representation
       **/
     public static StringBuffer layoutXML( StringBuffer xml,
                                    HashMap aliases,
                                    boolean color_depth_coding )
     {
            StringBuffer htmlBuffer = new StringBuffer();

            // chunk all Tags
            int len= xml.length();

            String accume_value = null;

            int depth=0;
            char c=(char)-1;
            for(int i=0; i< len; i++)
            {
                 char prev_c = c;
                 c = xml.charAt(i);

                 //---------------------------------
                 // TAG PROCESSING
                 //---------------------------------
                 if( c == '<' ) // opening tag
                 {
                      //
                      // go to end of tag;
                      //
                      String tag = new String("<");
                      char k = (char)-1;
                      while( k != '>' ) {
                          i++;
                          k=xml.charAt(i);
                          tag += k;
                      }


                      if( tag.startsWith("</") ){
                          // closing tag, unwind depth for THIS TAG
                          depth--;
                      }
                      String tagfrag = tag.substring(1,tag.length()-1);
                      htmlBuffer.append(indent(depth) + fontOpen(depth, color_depth_coding) + "&lt"
                                        + aliasFilterTag(tagfrag, aliases)
                                        + "&gt" + fontClose() + "\n");

                      if( tag.endsWith("/>") ) {
                          // neutral
                      }
                      else if( tag.startsWith("</") ){
                          //    neutral -- we already unwound
                      }
                      else {
                          // opening tag (without closing tag), GO deeper on SUBSEQUENT TAGS
                          depth++;
                      }
                 }
            }
            return htmlBuffer;
     }

     private static String aliasFilterTag( String tag, HashMap aliases ){

       StringBuffer buf = new StringBuffer();
       int sz = tag.length();

       char c = (char)-1;
       char c_next_1 = (char)-1;
       char c_prev_1 = (char)-1;

       String  currToken = null;
       for(int i=0;i<sz;i++)
       {
           c_prev_1 = c;
           c = tag.charAt(i);

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
                         buf.append(c);  // "NULLALIAS"
                     }
                     currToken = null;
                }
           }
           else if( currToken != null ) {
               // accumulate token
               currToken += c;
           } else {
               buf.append(c);
           }
     }
     return buf.toString();
     }

     private static String fontOpen(int depth, boolean color_depth_coding){
        String buf = new String();
        int actual_color_depth = 0;

        if( depth > 0 ) actual_color_depth = (depth % colorRange.length);
        if( actual_color_depth < 0) actual_color_depth=0;
        else if( actual_color_depth >= colorRange.length )actual_color_depth=colorRange.length-1;

        if( color_depth_coding) return "<FONT COLOR=" + colorRange[actual_color_depth] + ">";
        return "<FONT>";
     }

     private static String fontClose() { return "</FONT>"; }

     private static String indent(int depth){
        String buf= new String();
        for(int j=0; j<depth; j++){
             buf +="   ";
        }
        return buf;
     }

    static private String[] colorRange= {
       "000000", "FF0000", "EE0000", "DD0000", "CC0000", "BB0000", "AA0000",
       "990000", "880000", "770000", "660000", "550000", "440000", "330000", "220000",
       "110000"
    };
}
