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

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.w3c.dom.*;



//#######################################################################
public class ConnectionListener implements Runnable
{
    //  Add new live ConnectionListeners to activeSet
    //  Once finished, move "dead" ConnectionListener to deadSet
    //  note deadSEt is not static -- it is owned by ConnectionManager
    //
    private static List activeSet; // set of all live ConnectionListeners  (Transient)
    private  List  myDeadSet; // set of all "dead" connectionListeners in the CM "namespace"


    private static Hashtable connexionSpaces; // set of connections partitioned into Spaces (per CM instance "client")

    static {
        // set = new ArrayList();
        activeSet = Collections.synchronizedList(new ArrayList());
        connexionSpaces = new Hashtable();
    }

    private URLConnexionProbe myURLConnexionProbe = null;
    private Thread myThread = null;
    private ConnectionLogger myLogger = null;
    private Object mySpaceKey = null;

    private long openConnectionTimestamp=0;
    private long connectionInterval=0;
    
    // totalByteCount vs. lastByteCount
    // These should be identical in case of non keep-alive tabulation...
    // in Keep-alive case, lastbytecount = each increment size, total is cumulative
    private long totalByteCount=0;
    private int lastByteCount=0;

    // List: Write Strings of data from connection.  Multiple strings if this
    // connection listener listening to keep alive
    private List myDataPool = null;


    private ConnectionListener(){
    }

    public ConnectionListener(URLConnexionProbe u, ConnectionLogger logger,
                              Object space_key,
                              List dead_set,
                              List data_pool)
    {
       myDeadSet = dead_set;
       myURLConnexionProbe = u;
       myLogger = logger;
       mySpaceKey = space_key;
       myDataPool = data_pool;
    }

    public URLConnexionProbe getConnexionProbe(){
       return myURLConnexionProbe;
    }

    public static boolean isAllDone(Object space_key){
       List space_set = (List)connexionSpaces.get(space_key);
       return space_set.isEmpty();
    }
    public static int spaceSize(Object space_key){
       List space_set = (List)connexionSpaces.get(space_key);
       return space_set.size();
    }

    /** @returns wait lock **/
    public void start(){
       addMyself();
       myThread = new Thread(this);
       myThread.start();
    }

    //------------------------------------------------------------
    // THREAD EXECUTION
    //------------------------------------------------------------
    public void run(){
        //System.out.print(".");

        // These should be identical in case of non keep-alive tabulation...
        // in Keep-alive case, lastbytecount = each increment size, total is cumulative
        lastByteCount=0;
        totalByteCount=0;

        try{
           openConnectionTimestamp = System.currentTimeMillis();

           /**
           URLConnection uc = myURLConnexionProbe.openConnection();
           InputStream is = uc.getInputStream();
           **/

           //BufferedInputStream bis = new BufferedInputStream(is);
           byte buf[] = null; // new byte[512];
           StringWriter result =null;

           myURLConnexionProbe.init();


           boolean connextionActive = true;
           while( connextionActive )
           {
               result = new StringWriter();
               connextionActive = myURLConnexionProbe.readConnexion(result);
               int len = result.getBuffer().length();
               if( len > 0 ) {
                   synchronized( myDataPool ) {
                       System.out.println("[ConnectionListener] run()=ADDing result, sz:" + len);
                       myDataPool.add(result.getBuffer());
                       lastByteCount = len;
                       totalByteCount += len;
                   }
               }
           }

           if( lastByteCount <= 0 ) {
              myLogger.log("<LI><FONT COLOR=RED>NO DATA from Connection:</FONT><UL><LI>"
                        + myURLConnexionProbe.toExternalForm()
                        +    "<LI>Connexion Space: " + mySpaceKey
                        +    "</UL>");
           }

           /**
           //System.out.println(">" + myURLConnexionProbe.toString() + " : " + result.substring(0,80));
           synchronized( myDataPool ) {
               myDataPool.add(result);
           }
           **/

           long end = System.currentTimeMillis();
           this.connectionInterval = end - openConnectionTimestamp;

        } catch( ConnexionGaugeEvent event ) {
           new RuntimeException ( "ConnexionGaugeEvent encountered - dunno how to handle");

        } catch( Exception ex ){
           myLogger.log("<UL>" +  "<LI><FONT COLOR=RED>Exception</FONT>"
                        +    "<LI>Connection to: " + myURLConnexionProbe.toExternalForm()
                        +    "<LI>Connexion Space: " + mySpaceKey
                        +    "<LI>Exception " + ex
                        +    "</UL>");
           System.err.println(ex);
           ex.printStackTrace();
        }
        removeMyself();
        myLogger.log("<LI><FONT COLOR=RED>Successfully closed connection</FONT>, remainder:"
                        +    "<FONT SIZE=-2 COLOR=BLUE>"
                        +          describeSetOfListeners(this.mySpaceKey, this.myDeadSet)
                        +    "</FONT>"
                        +    "<UL>"
                        +       "<LI>Connexion Space: " + mySpaceKey
                        +       "<LI>Connection to: " + myURLConnexionProbe.toExternalForm()
                        +        "<LI>Duration: " +  this.connectionInterval
                        +        "<LI>lastByteCount      :" + lastByteCount
                        +        "<LI>cumulativeByteCount:" + this.totalByteCount + " (applicable to keepalive only)"
                        +        "<LI>data pool size:" + myDataPool.size()
                        +    "</UL>"
                        +    "</UL>");
        System.out.println("activeSet removed called..." + activeSet.size() );
    }

    public static String describeSetOfListeners(Object space_key, List dead_set){
        int space_active_size = spaceSize(space_key);
        String str = new String( "<UL>" +
                                 "<LI>Total number of ConnectionListeners active= " + activeSet.size() +
                                 "<LI>Number of ConnectionListeners active in this space=" +  space_active_size +
                                 "<LI>Number of ConnectionListeners dead in this space=" + dead_set.size() +
                                 "<LI>Total Spaces (CM 'clients')= " + connexionSpaces.size());
        Enumeration en = connexionSpaces.keys();
        while( en.hasMoreElements() )
        {
            Object k = en.nextElement();
            str += "<UL><LI>Key=" + k +"</UL>";
        }
        str +="</UL>";
        return str;
    }


    private  void removeMyself() {
        List space_set = (List)connexionSpaces.get(this.mySpaceKey);
        synchronized( activeSet) { activeSet.remove(this); }
        synchronized( myDeadSet) { myDeadSet.add(this);    }
        synchronized( space_set) { space_set.remove(this); }
    }

    private  void addMyself() {
        List space_set = (List)connexionSpaces.get(this.mySpaceKey);
        if( space_set == null){
            space_set = Collections.synchronizedList(new ArrayList());
            connexionSpaces.put(this.mySpaceKey,space_set);
        }
        synchronized( activeSet) { activeSet.add(this);    }
        synchronized( myDeadSet) { myDeadSet.remove(this); }
        synchronized( space_set) { space_set.add(this);    }
    }
}
