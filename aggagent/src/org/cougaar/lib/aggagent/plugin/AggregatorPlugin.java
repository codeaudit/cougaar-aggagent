package org.cougaar.lib.aggagent.plugin;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.StringReader;

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


public class AggregatorPlugin extends SimplifiedPlugIn
{
    private ConnectionManager myConnectionManager = null;

    public long INITIAL_WAKE_UP_INTERVAL = 10000;
    public long POLL_INTERVAL = 20000; // miliseconds

    /**
         NO SUBSCRIPTIONS FOR NOW
         
    private IncrementalSubscription mySubscription;
    private UnaryPredicate myPredicate;
    **/
    private String ConfigFileName = null;

    public void setupSubscriptions() {
        try{
           /**
               NO SUBSCRIPTIONS FOR NOW

           Class klass = Class.forName("alp.ui.aggserver.ldm.Task");
           myPredicate = this.getInstanceOfPredicate(klass);
           mySubscription = (IncrementalSubscription)subscribe(myPredicate);
           **/
           ConfigFileName = getStringParameter(getParameters().elements(),
                                  Configs.AGGREGATOR_DEFAULT_CONNECTION_CONFIG_FILE_NAME);

          //
          // Cant do this here, apparently hookup to society (nameserver) not ready
          //
          //NameService myNameServiceProvider = new ProxyMapAdapter( new FDSProxy() );
          //myConnectionManager = new ConnectionManager(
          //        new ConnectionLogger(), ConfigFileName, myNameServiceProvider);

          // force execute and initialize connection manager.
          this.wakeAfter(INITIAL_WAKE_UP_INTERVAL);


        } catch (Exception ex ) {
           ex.printStackTrace();
        }
    }

    private boolean first_time = true;

    public void execute() {
        if( first_time ){
           FDSProxy fds = new FDSProxy();
           NameService myNameServiceProvider = new ProxyMapAdapter( fds );
           myConnectionManager = new ConnectionManager(
                   new ConnectionLogger(),
                   ConfigFileName,
                   myNameServiceProvider);
           myConnectionManager.connectAllAsynchronous();
           first_time = false;
        }


        // visit all connections, harvest data, and recycle any which
        // may have failed and or are down (ie. non keep-alive)
        List data = myConnectionManager.cycleAllAsynchronousConnections(true);
        System.out.println("[AggregatorPlugin] total data List size=" + data.size());
        Iterator it = data.iterator();
        while (it.hasNext() ) {
            Object obj = it.next();
            String str = null;
            if( obj instanceof StringBuffer ) {
                 str = ((StringBuffer)obj).toString();
            }


            /**
            int substr_index =-1;
            if( str.length() > 6 ) {
               substr_index = str.substring(0,5).toLowerCase().indexOf(" <?xml");
            }

            if( substr_index > -1 )
            {
               System.out.println("Is XML");
            **/
               try{
                    //ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
                    //InputSource is = new InputSource(bais);
                    StringReader sr = new StringReader(str.trim());
                    InputSource is = new InputSource(sr);

                    //System.err.println("str=" + str);
                    //FileReader freader = new FileReader("task.xml");
                    //InputSource is = new InputSource(freader);

                    DOMParser domp = new DOMParser();
                    domp.setErrorHandler(new ErrorHandler(){
                          public void error(SAXParseException exception) {
                             System.err.println("[ErrorHandler.error]: " + exception);
                           }
                           public void fatalError(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.fatalError]: " + exception);
                           }
                           public void warning(SAXParseException exception) {
                                 System.err.println("[ErrorHandler.warning]: " + exception);
                            }
                        }
                    );

                    domp.parse(is);
                    Document doc = domp.getDocument();

                    System.out.println("Is XML");

                    if( doc != null ) {
                       PlanObject p = new PlanObject(doc);
                       publishAdd(p);
                    }
                } catch (Exception ex ){
                    //ex.printStackTrace();
                    System.out.println("Not XML");
                    System.out.println("\n[AggregatorPlugin] data=" + str + "\n");
                }
            /**
            }else {
                System.out.println("Not XML");
                System.out.println("[AggregatorPlugin] data=" + str);
            }
            **/
            int sz = str.length();
            System.out.println("[AggregatorPlugin] data len=" + str.length());
        }

        /**
        for(Enumeration e = mySubscription.getAddedList(); e.hasMoreElements();)
        {
        }
        **/

        this.wakeAfter(POLL_INTERVAL);
    }
    /**
   * Return a String parameter from the head of a list of plugin parameters
   * @param Enumeration of Plugin command line parameters
   * @param integer default value if no numeric value found
   * @return int value parsed from first numeric argument
   */
  public static String getStringParameter(Enumeration parameters,
  String default_value)
  {
    String value = default_value;
    while(parameters.hasMoreElements()) {
      String param = (String)parameters.nextElement();
      try {
        value = new String(param);
        break;
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    return value;
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

/**
  // mil.darpa.log.alpine.ui.psp.xmlservice.PSP_LogPlanProvider
  //
  //  Called when a request is received from a client.
  //  Get the POST data; parse the request; get the log plan objects
  //  that match the request; encode these in XML document; send the
  //  document to the client.

  public void execute( PrintStream out,
		       HttpInput query_parameters,
		       PlanServiceContext psc,
		       PlanServiceUtilities psu) throws Exception {
			 
    String postData = null;
    boolean limit = false; // flag to limit number of items returned

    if (query_parameters.getPostData() != null) {
      postData = new String(query_parameters.getPostData());
      postData = postData.trim();
      System.out.println("POST DATA:" + postData);

      // if postData starts with the term "LIMIT"
      // then strip this off and limit the number of items returned
      if (postData.startsWith(LIMIT_COMMAND)) {
	postData = postData.substring(LIMIT_COMMAND.length());
	limit = true;
      }
      // parse request from user
      Vector terms = RequestParser.parseRequest(postData);

      // if request is a command, execute it and return
      if (terms != null) {
	Term term = (Term)terms.elementAt(0);
	Method commandMethod = term.commandMethod;
	if (commandMethod != null) {
	  Object[] args = { psc, out };
	  commandMethod.invoke(null, args);
	  return;
	}
      }

      // define predicate to retrieve selected log plan objects
      XMLObjectSelector xmlPredicate = new XMLObjectSelector(terms);

      // enter subscription and get objects from it
      Subscription subscription =
	psc.getServerPlugInSupport().subscribe(this, xmlPredicate);
      Collection container = 
	((CollectionSubscription)subscription).getCollection();
      Vector planObjects = new Vector(container.size());
      // if user wants only a few objects, then limit the number
      // of returned objects here
      if (limit) {
        int nItems = 10;
        for (Iterator i = container.iterator(); i.hasNext();) {
	  planObjects.addElement(i.next());
	  nItems--;
	  if (nItems == 0) break;
	}
      } else 
        for (Iterator i = container.iterator(); i.hasNext();)
	  planObjects.addElement(i.next());

      // unsubscribe, don't need this subscription any more
      psc.getServerPlugInSupport().unsubscribeForSubscriber(subscription);

      // create XML document for selected plan objects
      XMLPlanObjectProvider provider = new XMLPlanObjectProvider();
      for (int i = 0; i < planObjects.size(); i++)
	provider.addPlanObject(planObjects.elementAt(i));
      TXDocument doc = provider.getDocument();

      // send document to client
      doc.print(new PrintWriter(out));
      System.out.println("Sent XML document");
    }
  }
  **/