/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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