/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
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

//
// If encounters delimiter "&&&" will hold connection open but write data.
// otherwise reads until connection closed.
//
// first case is for KeepAlive connections, 2nd cse is for polling connections
//
public class ConnexionParserByDelimiter implements ConnexionParser
{
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