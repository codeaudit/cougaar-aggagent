package org.cougaar.lib.aggagent.connect;




public class ConnexionGaugeEvent extends java.lang.Throwable
{
     // STATUS ENUMERATION
     public final int UNDEFINED =-1;
     public final int RESUME_READ =1;
     public final int READ_ABORTED_READ_ONCE_FOR_REMAINDER=2;
     public final int READ_ABORTED=3;
     // END STATUS ENUMERATION

     private int myStatus = UNDEFINED;

     // returns STATUS ENUMERATION code...
     public int getStatus(){return myStatus; }
     public void setStatus(int s) { myStatus = s; }

} 
