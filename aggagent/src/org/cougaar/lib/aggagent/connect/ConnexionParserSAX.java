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

import org.cougaar.lib.aggagent.bsax.*;


//
// Don't use.  This is Illustration-ware only.
// Illustrates pattern on how to use "content-handlers" to validate
// input stream.  In this case, tried with SAX parser.  However,
// This particular implementation doesn't work.  But you get the idea.
// Try with your own stream content handler.
//
public class ConnexionParserSAX implements ConnexionParser
{
    private BContentHandler myContentHandler = new BContentHandler_Stack();
    private SAXParser mySaxParser = new SAXParser();

    public ConnexionParserSAX() {
        try {
            // try to activate validation
            mySaxParser.setFeature("http://xml.org/sax/features/validation", true);
        } catch (SAXException e) {
            System.err.println("Cannot activate validation.");
        }
        mySaxParser.setContentHandler(myContentHandler);
        mySaxParser.setErrorHandler(new BErrorHandler());
    }


    //
    //  @return boolean true if connexion still open, else false
    //
    public boolean parse(StringWriter writer, BufferedReader reader) {

                         // register event handlers

        myContentHandler.clear();

                         // parse the first document
        boolean reset=false;
        try {
            if( reader.markSupported() ) reader.mark(1024);
            InputSource in = new InputSource(reader);
            mySaxParser.parse(in);
        } catch (IOException e) {
           System.err.println("I/O exception reading XML document. " + e);
           return false;
        } catch (SAXException e) {
           System.err.println("SAX exception parsing document. " + e);
           if( reader.markSupported() ) reset= true;
           e.printStackTrace();
        }

        /**
        try {
          if ( reset ) {
              reader.reset();
              System.out.println("[ConnexionParserSAX] RESETTING SAX READER");
          }
        } catch (Exception ex ){
           ex.printStackTrace();
        }
        **/

        //-----------------------------------------------------------------
        // Traverse and "write" the tree
        // Start from the root object
        //-----------------------------------------------------------------
        PrintWriter pw = new PrintWriter(writer);
        pw.println("<?xml version=\"1.0\"?>");
        Iterator it3 = myContentHandler.getRootElements().iterator();
        while(it3.hasNext()){
           BElement be = (BElement)it3.next();
           be.print(pw);
        }
        pw.flush();
        System.out.println("[ConnexionParserSAX] pw.toString()=" + pw.toString() );
        return true;
    }
}