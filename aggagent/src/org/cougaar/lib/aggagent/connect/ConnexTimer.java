package org.cougaar.lib.aggagent.connect;




public class ConnexTimer
{
     private long last_go_time = -1;
     private long min_check_time = -1;


     public ConnexTimer( long min ){
          min_check_time = min;
     }

     public boolean proceed() {
          long now  =System.currentTimeMillis();
          boolean go = false;
          if( (now - last_go_time) > min_check_time ){
              go = true;
              last_go_time = now;
          }
          return go;
     }
}

