package org.cougaar.lib.aggagent.plugin;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.io.OutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.io.PrintWriter;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;

import org.cougaar.lib.planserver.PSP_BaseAdapter;
import org.cougaar.lib.planserver.PlanServiceProvider;
import org.cougaar.lib.planserver.UISubscriber;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Enumerator;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.plugin.PlugInDelegate;
import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.lib.planserver.RuntimePSPException;
import org.cougaar.lib.planserver.PlanServiceContext;
import org.cougaar.lib.planserver.PlanServiceUtilities;
import org.cougaar.core.plugin.SimplifiedPlugIn;
import org.cougaar.lib.planserver.server.FDSProxy;
import org.cougaar.lib.planserver.server.NameService;
import org.cougaar.lib.planserver.server.ProxyMapAdapter;

import org.cougaar.lib.aggagent.Configs;
import org.cougaar.lib.aggagent.connect.ConnectionManager;
import org.cougaar.lib.aggagent.connect.ConnectionLogger;
import org.cougaar.lib.aggagent.ldm.*;


public class SimpleTransformerPlugin extends SimplifiedPlugIn
{
    public long INITIAL_WAKE_UP_INTERVAL = 10000;
    public long POLL_INTERVAL = 15000; // miliseconds

    // Don't GC if time since last GC is less than this....
    private long GC_MIN_INTERVAL = 30000;
    private long STATUS_MIN_INTERVAL = 15000;



    private IncrementalSubscription mySubscription;
    private UnaryPredicate myPredicate;
    //
    // keep handle on blackboard objects we encounter
    //
    private ArrayList watchedBlackboardObjects = new ArrayList();

    private boolean removeObjectsImmediately=false;
    private int numberRemovedObjects=0;

    public void setupSubscriptions() {
        try{
           Class klass = Class.forName("java.lang.Object");
           myPredicate = this.getInstanceOfPredicate(klass);
           mySubscription = (IncrementalSubscription)subscribe(myPredicate);


           this.wakeAfter(INITIAL_WAKE_UP_INTERVAL);

           String remove_val = getStringParameter("remove=", getParameters().elements(),
                                  "false");
           if( remove_val.trim().toLowerCase().startsWith("true") == true) removeObjectsImmediately = true;


        } catch (Exception ex ) {
           ex.printStackTrace();
        }
    }

    // Keep track last time did a GC
    private long timeStampLastGC = 0;

    // Keep track of last status message
    private long timeStampLastStatus=0;

    public void execute() {

       long currTime= System.currentTimeMillis();
       
       Enumeration en = mySubscription.getAddedList();
        while( en.hasMoreElements() ){
            Object obj = en.nextElement();

            if( removeObjectsImmediately )
            {
                this.numberRemovedObjects++;

                boolean status = this.publishRemove(obj);
                //System.out.println("Remove Status:" + status);

                if( (currTime - timeStampLastGC) >  GC_MIN_INTERVAL )
                {
                    System.gc();
                    timeStampLastGC =  currTime;
                }
            }
            else {
                watchedBlackboardObjects.add(obj);
            }
        }

        if( (currTime - timeStampLastStatus) >  STATUS_MIN_INTERVAL )
        {
             printStatus( System.out);
             timeStampLastStatus =  currTime;
        }
        this.wakeAfter(POLL_INTERVAL);
    }

    private void printStatus( OutputStream out)
    {
         PrintWriter pw = new PrintWriter(out);
         pw.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
         pw.println("! SimpleTransformer status");
         pw.println("! num watched objects (blackboard)=" + watchedBlackboardObjects.size() );
         pw.println("! num removed objects (blackboard)=" + this.numberRemovedObjects );
         long free = Runtime.getRuntime().freeMemory();
         long total = Runtime.getRuntime().totalMemory();
         long used = total-free;
         pw.println("! VM free memory =" + free );
         pw.println("! VM total memory=" + total);
         pw.println("! -------------------------");
         pw.println("! used            " + used  );
         pw.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
         pw.flush();
    }


    /**
   * Return a String parameter from the head of a list of plugin parameters
   * @param Enumeration of Plugin command line parameters
   * @param integer default value if no numeric value found
   * @return int value parsed from first numeric argument
   */
  public static String getStringParameter(String prefix, Enumeration parameters,
                                          String default_value)
  {
    while(parameters.hasMoreElements()) {
      String param = (String)parameters.nextElement();
      if (param.startsWith(prefix)) {
        String sVal = param.substring(prefix.length()).trim();
        return sVal;
      }
    }
    return default_value;
  }

    public UnaryPredicate getInstanceOfPredicate(final Class klass ) {
         UnaryPredicate pred = new UnaryPredicate() {
                 public boolean execute(Object o) {
                     if (klass.isInstance(o)) {
                        return true;
                     }
                     return false;
                }
         };
         return pred;
    }
}


