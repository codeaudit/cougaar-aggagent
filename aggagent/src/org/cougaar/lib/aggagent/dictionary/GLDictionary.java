
package   org.cougaar.lib.aggagent.dictionary;


import java.util.*;
import java.net.URL;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import org.cougaar.lib.planserver.HttpInput;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.lib.planserver.server.NameService;

import org.cougaar.lib.aggagent.Configs;
import org.cougaar.lib.aggagent.XMLParseCommon;
import org.cougaar.lib.aggagent.dictionary.GenericLogic;
import org.cougaar.lib.aggagent.dictionary.glquery.GenericQuery;
import org.cougaar.lib.aggagent.ldm.PlanObject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;


import org.apache.xalan.xslt.XSLTInputSource;


////////////////////////////////////////////////////////////////////////////
class GLPrimitives{
    private List myPrimitives = Collections.synchronizedList(new ArrayList());

    public GLPrimitives(){
        /**
        myPrimitives.add(GLQ_KEY_QUERY_PLAN_WILDCARD);
        myPrimitives.add(GLQ_KEY_QUERY_SIMPLE_ALL);
        myPrimitives.add(GLQ_KEY_QUERY_SIMPLE_TASK);
        myPrimitives.add(GLQ_KEY_QUERY_SIMPLE_ASSET);
        **/
    }

    public void add( Object obj ) {
        myPrimitives.add(obj);
    }

    public String getKey( String keyname ){
       /// System.out.println("Looking up..." + keyname);
        synchronized( myPrimitives ) {
            Iterator it = myPrimitives.iterator();
            while( it.hasNext() ) {
               String str  = (String)it.next();
               ///System.out.println("comparing...'" + keyname + "' '" + str + "'");
               if( str.indexOf(keyname.toUpperCase()) > -1 ) {
                   //System.out.println("found...");
                   return str;
               }
            }
        }
        return null;
    }

    // GLPrimitives.GLQ_KEY_QUERY_WILDCARD => always match in Query Mode
    static final String GLQ_KEY_QUERY_PLAN_WILDCARD = "QUERY_PLAN_WILDCARD";  // IF USED, ALWAYS MATCHES

    /**
    static final String GLQ_KEY_QUERY_SIMPLE_ALL    = "QUERY_SIMPLE_ALL";
    static final String GLQ_KEY_QUERY_SIMPLE_TASK   = "QUERY_SIMPLE_TASK";
    static final String GLQ_KEY_QUERY_SIMPLE_ASSET  = "QUERY_SIMPLE_ASSET";
    **/
}

////////////////////////////////////////////////////////////////////////////
class GLEntry{
    public GLEntry(GenericLogic gl, List kwords, Map params)
               { entry = gl; keywords = kwords; parameters=params; }
    public GenericLogic entry;
    public List keywords;
    public Map parameters;
}
////////////////////////////////////////////////////////////////////////////
public class GLDictionary extends DictionaryBase
{

      // myGenericLogicUnits + myXSLEntries + myPredicateEntries share "keys"
      //       myGenericLogicUnit => URL KEYWORD
      //       myXSLEntries       =>  HashMap key
      //       myPredicateEntries =>  HashMap key
      //
      private List myGenericLogicUnits = Collections.synchronizedList( new ArrayList());
      private Map myXSLEntries = Collections.synchronizedMap( new HashMap() );
      private Map myPredicateEntries =  Collections.synchronizedMap( new HashMap() );
      private GLPrimitives myGLPrimitives = new GLPrimitives();

      
      /////////////////////////////////////////////////////////////////////////////
      public int getNumGLEntries() {
          int size=0;
          size= myGenericLogicUnits.size();
          return size;
      }

      //
      // @param filepath : file cache (read) for dictionary entries...
      //
      public GLDictionary( String this_cluster_id ){
           //if( filepath == null)
           if(  this_cluster_id.indexOf(Configs.AGGREGATION_CLUSTER_NAME_PREFIX) > -1 ) {
               default_load_agg_cluster();
               //default_load_agg_cluster(this_cluster_id);
           }
           else {
               default_load_society_cluster();
               // default_load_society_cluster(this_cluster_id);
           }
      }


      private static String GL_QUERY_PRIMITIVE_TYPE = "glquery";
      private static String GL_UPDATE_PRIMITIVE_TYPE = "glupdate";

      /////////////////////////////////////////////////////////////////////////////
      private void default_load_society_cluster( ) {

         try{
             //Document doc = Common.getDocument(new File(Configs.GENERIC_PSP_SOCIETY_CLUSTER_PRIMITIVES));
             Document doc = ConfigFileFinder.parseXMLConfigFile(Configs.GENERIC_PSP_SOCIETY_CLUSTER_PRIMITIVES);
             Element root = doc.getDocumentElement();
             // parse for GL Query primitives
             parseXMLConfigFile(doc, GL_QUERY_PRIMITIVE_TYPE );
         } catch (Exception ex ){
             ex.printStackTrace();
         }
      }

      private void default_load_agg_cluster( ) {

         try{
             //Document doc = Common.getDocument(new File(Configs.GENERIC_PSP_SOCIETY_CLUSTER_PRIMITIVES));

             Document doc = ConfigFileFinder.parseXMLConfigFile(Configs.GENERIC_PSP_AGG_CLUSTER_PRIMITIVES);
             Element root = doc.getDocumentElement();
             // parse for GL Query primitives
             parseXMLConfigFile(doc, GL_QUERY_PRIMITIVE_TYPE );
         } catch (Exception ex ){
             ex.printStackTrace();
         }
      }

      /**

           // GLPrimitives.GLQ_KEY_QUERY_WILDCARD => always match in Query Mode
           default_load_GQXML(GLPrimitives.GLQ_KEY_QUERY_SIMPLE_ALL,
                               "../../gldictionary_entries/all.plan.xsl",
                               GLUnaryPredicates.getInstanceOfPredicate("java.lang.Object"),
                               "mil.darpa.log.alpine.ui.psp.xmlservice.GenericQueryXML");
      **/


      /**
        * @param klass : klass name to instantiate Entry
        **/
      private void default_load_GQXML(Object key, String xsl_file_path,
                                      String sax_class_name,
                                      UnaryPredicate predicate, // String pred_test_klass_name,
                                      String klassname )
      {
            try{
                 ////////////////////////////////////////////////////////////
                 // ADD ENTRY TO DICTIONARY: XSL
                 ////////////////////////////////////////////////////////////
                 // xsl_file_path can be null.   in which case, XSL
                 // will not be applied to XML result.
                 if( xsl_file_path != null )
                 {
                     File xslf = new File( xsl_file_path);
                     System.out.println("XSL FILE ATTEMPTED READ:" + xslf.getAbsolutePath() );
                     FileReader fr = new FileReader(xslf);
                     StringBuffer sbuf  = new StringBuffer();
                     char buf[] = new char[256];
                     int sz=0;
                     while((sz=fr.read(buf)) > -1 ){
                         sbuf.append(buf,0,sz);
                     }
                     this.addXSL(key, sbuf);
                 }

                  ///////////////////////////////////////////////////////////
                  // ADD ENTRY TO DICTIONARY: UNARYPREDICATE
                  ///////////////////////////////////////////////////////////
                  //UnaryPredicate predicate=getInstanceOfPredicate(Class.forName(pred_test_klass_name) );
                  this.addPredicate(key, predicate);

                 ////////////////////////////////////////////////////////////
                 // ADD ENTRY TO DICTIONARY: GenericQuery instance
                 ////////////////////////////////////////////////////////////
                 ArrayList keys = new ArrayList();
                 keys.add(key);
                 Map params = new HashMap();
                 params.put( "XSL", this.getXSL(key) );
                 params.put( "PREDICATE", this.getPredicate(key));
                 params.put( "SAX", sax_class_name);

                 Class klass = Class.forName(klassname);
                 GenericQuery queryObj = (GenericQuery)klass.newInstance();
                 queryObj.init(key, params);

                 // Right now append in both cases --
                 // we allow this fiction as it forces to be explicit
                 // at higher levels which GLs must be at end...
                 //
                 this.addGenericLogic( queryObj, keys, params );

            } catch (Exception ex ){
                 ex.printStackTrace();
            }
      }


      /**
        * @param URLParamKeywords = mandatory keyworks which must be in
        *         incoming URL to match
        * @param defaultParams = default parameters applied when entry
        *         is evoked
        **/
      public void addGenericLogic( GenericLogic entry, List URLParamKeywords, Map defaultParams ) {
            GLEntry gle = new GLEntry( entry, URLParamKeywords, defaultParams);
            myGenericLogicUnits.add(gle);
      }

      public void addXSL( Object key, StringBuffer xsl_data ){
            myXSLEntries.put(key, xsl_data);
      }
      public StringBuffer getXSL(Object key) {
            Object obj = myXSLEntries.get(key);
            //System.out.println("+++++++++++++++++++++++++++++++++++++++++++" + obj);
            StringBuffer sbuf = (StringBuffer)obj;
            return sbuf;
      }

      public StringBuffer lookupXSLByString( String name ){
            synchronized( myXSLEntries ) {
                Iterator it = myXSLEntries.keySet().iterator();
                int count=0;
                while( it.hasNext() ){
                    String k = (String)it.next();
                    System.out.println("lookup:  k=" + k + ", name=" + name);
                    if(k.equals(name) )return (StringBuffer)(myXSLEntries.entrySet().toArray()[count]);
                    count  ++;
                }
                return null;
            }
      }

      public void addPredicate( Object key, UnaryPredicate pred ){
            myPredicateEntries.put(key, pred);
      }
      public UnaryPredicate getPredicate(Object key) {
            Object obj = myPredicateEntries.get(key);
            return (UnaryPredicate)obj;
      }


      public final static int MATCH_MODE_QUERY = 1;

      /**
       * @param urlinfo :
       * Returns first GenericLogic (Query or Update) which matches
       * URL request.
       * @param mode :   MATCH_MODE_QUERY = Query
       **/
      public GenericLogic match( HttpInput urlinfo,  int mode)
      {
           System.out.print(">>>>>>>>>>>...ENTER MATCH");
           Map inputs = parseURLArgs( urlinfo );
           Object [] copy = myGenericLogicUnits.toArray();

           for( int i=0; i< copy.length; i++)
           {
                  GLEntry ge = (GLEntry)copy[i];
                  List keys = ge.keywords;
                  Iterator itk = keys.iterator();
                  boolean match=true; // no keywords - match automatically
                  while(itk.hasNext())
                  { // for each keyword
                        String k = (String)itk.next();
                        System.out.print(">>>>>>>>>>>...test KEY: " + k);

                        if( (mode==MATCH_MODE_QUERY) && k.equals(GLPrimitives.GLQ_KEY_QUERY_PLAN_WILDCARD) ) {
                            // Automatically accept this
                            match=true;
                            break;
                        }

                        if( (false == inputs.containsKey(k)) ) {
                           match = false;
                           break;
                        }
                  }
                  // check if matched all keywords...
                  if( match == true) {
                           System.out.print(">>>>>>>>>>>>>>>>>>..FOUND ENTRY!");
                           return (GenericLogic)ge.entry; // FOUND ONE!!!
                  }
           }
           //System.out.println("GLDictionary returning NULL");
           return null;
      }

      public String toHTMLString(){
           String str = new String("<P>GLDictionary Entries...</P>");
           str = "<TABLE>";
           synchronized( myGenericLogicUnits )
           {
             Iterator it = myGenericLogicUnits.iterator();
             while(it.hasNext()) {
                str += "<TR><TD>";
                GLEntry ge = (GLEntry)it.next();
                List keys = ge.keywords;
                Iterator itk = keys.iterator();
                while(itk.hasNext()){ // for each keyword
                        String k = (String)itk.next();
                        str += "keyword= <FONT COLOR=RED SIZE=+1>" + k + "</FONT> ";
                }
                str += "</TD>"; str += "</TR>";

                str += "<TR><TD>";
                StringBuffer sbuf = ((StringBuffer)ge.entry.getParam("XSL"));
                str += "XSL <FONT COLOR=GREEN SIZE=-1>:";
                if( sbuf != null ) // can be null if user-defined non-xsl GU
                {
                    str += XMLParseCommon.filterXMLtoHTML(sbuf).toString();
                } else  {
                    str += "NULL XSL";
                }
                str += "</FONT>";

                str += "<P>UnaryPredicate <FONT COLOR=BLUE SIZE=-1>:";
                if( ge.entry.getPredicate() instanceof UnaryPredicateGLWrapper ) {
                    UnaryPredicateGLWrapper w = (UnaryPredicateGLWrapper)ge.entry.getPredicate();
                    str+= "<PRE><BLOCKQUOTE>" + w.getAnnotation()  + "</PRE></BLOCKQUOTE>";
                }
                str += "</FONT></P>";
                str += "</TD></TR>";
             }
             str += "</TABLE>";
             return str;
           } // end synchronized ( myGenericLogicUnits )
      }

      /**
  public static StringBuffer filterXMLtoHTML(StringBuffer dataout)
  {
       int srcend = dataout.length();
       char csrc[] = new char[srcend];
       dataout.toString().getChars(0,srcend,csrc,0);

       //StringWriter sw = new StringWriter(srcend);
       StringBuffer buf = new StringBuffer(srcend);

       int i;
       int sz = dataout.length();
       for(i=0;i<sz;i++){
           char c = csrc[i];
           if( c == '<' ) buf.append("&lt");
           else if( c == '>' ) buf.append("&gt");
           else buf.append(c);
       }
       return buf;
  }
**/
   //
   //   Pattern with example values...
   //
   //   <xml>
   //      <glquery>
   //          <keyname>QUERY_SIMPLE_ALL</keyname>
   //          <xsl>../../gldictionary_entries/asset.simple.1.xsl</xsl>
   //          <predicate>
   //               <instanceof>java.lang.Object</instanceof>  <!-- mutually exclusive w/domnode -->
   //               <domnode>Task</domnode>  <!-- mutually exclusive w/instanceof -->
   //          </predicate>
   //          <adapter>mil.darpa.log.alpine.ui.psp.xmlservice.GenericQueryXML</adapter>
   //      </glquery>
   //  </xml>

   private void parseXMLConfigFile( Document doc, String gl_primitive_type ){

        NodeList nl = doc.getElementsByTagName(gl_primitive_type);
        System.out.println("glprim_type=" + gl_primitive_type );
        System.out.println("Number of Source Entries=" + nl.getLength() );
        int size = nl.getLength();
        for(int i = 0; i< size; i++)
        {
            Node n = (Node)nl.item(i);
            String keyname = XMLParseCommon.getAttributeOrChildNodeValue("keyname", n);
            String xsl = XMLParseCommon.getAttributeOrChildNodeValue("xsl", n);
            String sax = XMLParseCommon.getAttributeOrChildNodeValue("sax", n);
            Node prednode = XMLParseCommon.getAttributeOrChildNode("predicate", n);
            String adapter = XMLParseCommon.getAttributeOrChildNodeValue("adapter", n);
            String instv  = null; // instanceof predicate elem
            String domv   = null;
            String twv   = null;

            // XSL argument can be NULL!
            // Case:  where user provides non-xsl private GenericLogic

            if( prednode != null ) {
                System.out.println("prednode:  haschildren="  + prednode.hasChildNodes()
                                     + " sibling=" + prednode.getNextSibling().getNodeName());
                instv = XMLParseCommon.getAttributeOrChildNodeValue("instanceof", prednode);
                domv = XMLParseCommon.getAttributeOrChildNodeValue("domnode", prednode);
                twv = XMLParseCommon.getAttributeOrChildNodeValue("taskwverb", prednode);
            } else {
                 System.err.println("[GLDictionary] parseXMLConfigFile error:  no predicate element found in XML");
                 System.err.println("\tkeyname=" + keyname + ",\n\tadapter=" + adapter + "\n\txsl="+xsl);
            }

            UnaryPredicate upred = null;
            int numPreds = 0;
            if (instv != null) { numPreds++; }
            if (domv != null) { numPreds++; }
            if (twv != null) { numPreds++; }
            //if( (instv == null) && (domv == null) && (twv==null) ) {
            if( numPreds == 0 ) {
                 System.err.println("[GLDictionary] parseXMLConfigFile error:  no PREDICATE defined for entry");
                 System.err.println("\tkeyname=" + keyname + ",\n\tadapter=" + adapter + "\n\txsl="+xsl);
            }
            else if( numPreds > 1  ) {
                 System.err.println("[GLDictionary] parseXMLConfigFile error: ambiguous PREDICATE defintion for entry");
                 System.err.println("\tkeyname=" + keyname + ",\n\tadapter=" + adapter + "\n\txsl="+xsl);
            }
            else if( (instv != null ) ) {
                upred = GLUnaryPredicates.getInstanceOfPredicate(instv);
            }
            else if( (twv != null ) ) {
                upred = GLUnaryPredicates.getTaskWithVerbPredicate(twv);
            }
            else if( (domv != null)) {
                upred = GLUnaryPredicates.getDOMNodeNamePredicate(domv);
            }

           //
           // myGLPrimitives identifies set of valid keys.
           //
           // use keyname String instance as key -- stash it away
           //
           myGLPrimitives.add(keyname);
           //
           // load the glprimitive for that key
           //
           default_load_GQXML(keyname, // myGLPrimitives.getKey(keyname),
                               xsl,
                               sax,
                               upred,
                              adapter);

        }
    }

}