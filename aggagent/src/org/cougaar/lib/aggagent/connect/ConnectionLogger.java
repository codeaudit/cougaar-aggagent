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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.File;



//#######################################################################
public class ConnectionLogger
{
     private PrintWriter myPrintWriter = null;

     public final static String DEFAULT_LOG_FILE_PREFIX="connxion";
     public final static String DEFAULT_LOG_FILE_SUFFIX=".html";
     public final static int MAX_NUMBER_LOG_FILES = 100;

     public static String MY_LOG_FILE = "";


     public ConnectionLogger(String filename_prefix, String filename_suffix) {
          File f = null;
          for(int i=0; i<MAX_NUMBER_LOG_FILES; i++) {
                   if( i==0 ) f = new File( filename_prefix +  filename_suffix );
                   else f = new File( filename_prefix + i + filename_suffix );

                   if( f.exists() )  {
                       continue;
                   } else break;
          }
         MY_LOG_FILE = f.getName();
         defLogger( new File( MY_LOG_FILE) );
     }

     public ConnectionLogger(String filename){
         MY_LOG_FILE = filename;
         defLogger( new File( MY_LOG_FILE) );
     }

     private void defLogger( File f )
     {
         System.out.println("[ConnectionLogger] Logger opened.");
         try{

                FileOutputStream fos = new FileOutputStream(f);
                myPrintWriter = new PrintWriter(fos);

                myPrintWriter.println("<HTML>");
                myPrintWriter.println("<BODY>");
                myPrintWriter.println("<H2>Logger(file=" + MY_LOG_FILE + ")</H2>");
                myPrintWriter.println("<P><I>Classpath=" + System.getProperty("java.class.path") + "</I></P>");
                myPrintWriter.println("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
         } catch (Exception ex) {
                ex.printStackTrace();
         }
     }

     public synchronized void log(String msg){
         myPrintWriter.println("<TR><td valign=\"top\" align=\"center\" bgcolor=\"6699cc\" height=\"23\" width=\"18%\">"
                                + System.currentTimeMillis() + "</TD><TD>" + msg + "<TD>");
         myPrintWriter.flush();
     }

     public void finalize(){
         myPrintWriter.println("</TABLE>");
         myPrintWriter.println("</HTML>");
         System.out.println("Logger closed.");
     }
}

