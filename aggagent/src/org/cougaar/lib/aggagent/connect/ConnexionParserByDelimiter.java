package org.cougaar.lib.aggagent.connect;


import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.io.IOException;


import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;

import  org.cougaar.lib.aggagent.bsax.*;


public class ConnexionParserByDelimiter implements ConnexionParser
{
    private BContentHandler myContentHandler = new BContentHandler_Stack();
    private SAXParser mySaxParser = new SAXParser();

    public ConnexionParserByDelimiter() {
    }


    //
    //  @return boolean true if connexion still open, else false
    //
    public boolean parse(StringWriter writer, BufferedReader reader) {

       try{
           reader.mark(1024);
           int c;
           int count=0;
           // if see "&&&" treat as delimiter
           while( (c = reader.read()) > -1 ) {
               if( c == '&' ) {
                   count ++;
               }
               if( count >= 3 ) {
                   return true;
               }else if( c != '&')
               {
                   for( ; count > 0; count--) writer.write('&');
                   writer.write(c);
               }
           }

           writer.flush();
       } catch (Exception ex ) {
           ex.printStackTrace();
       }
       return false;
    }
}