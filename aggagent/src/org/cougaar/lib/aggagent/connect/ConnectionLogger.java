package org.cougaar.lib.aggagent.connect;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;



//#######################################################################
public class ConnectionLogger
{
     private PrintWriter myPrintWriter = null;

     public final static String DEFAULT_LOG_FILE="connxion.html";

     public ConnectionLogger() {
         defLogger( DEFAULT_LOG_FILE );
     }

     public ConnectionLogger(String filename){
         defLogger(filename);
     }

     private void defLogger( String filename )
     {
         System.out.println("Logger opened.");
         try{
                FileOutputStream fos = new FileOutputStream(filename);
                myPrintWriter = new PrintWriter(fos);

                myPrintWriter.println("<HTML>");
                myPrintWriter.println("<BODY>");
                myPrintWriter.println("<H2>Logger</H2>");
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

