package org.cougaar.lib.aggagent.query;

import java.io.Serializable;
import java.io.StringReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.Vector;

import org.w3c.dom.*;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import org.cougaar.lib.aggagent.util.InverseSax;
import org.cougaar.lib.aggagent.util.XmlUtils;
import org.cougaar.lib.aggagent.util.Enum.*;

/**
 *  Instances of this class represent generalized "queries" in the sense of the
 *  Aggregation infrastructure.  That is, a specification of a subset of data
 *  available within the society, with the presumption that the infrastructure
 *  will supply this data in some fashion.  The way in which the specification
 *  is expressed will vary depending on the type of query.
 *  <br><br>
 *  Queries may be either transient ("find the answer now and then forget") or
 *  persistent ("maintain an updated view of the answer").
 *  <br><br>
 *  At this time, only one implementation (this one) is available.  The query
 *  is specified by an unordered set of Cluster names and a pair of SILK
 *  scripts which define, respectively, the set of blackboard objects that
 *  should be examined and the encoding of the findings (including any
 *  intermediate calculations that may be necessary).
 */
public class AggregationQuery implements Serializable {
    public static String QUERY_TAG = "query";
    private static String TYPE_ATT = "type";
    private static String UPDATE_ATT = "update_method";
    private static String PULL_RATE_ATT = "pull_rate";
    private static String NAME_ATT = "name";
    private static String CLUSTER_TAG = "source_cluster";

    private QueryType queryType = QueryType.TRANSIENT;
    private UpdateMethod updateMethod = UpdateMethod.PUSH;
    private int pullRate = -1; // wait period in sec.(if neg., don't auto-pull)
    private Vector sourceClusters = new Vector();

    private ScriptSpec predicateSpec = null;
    private ScriptSpec formatSpec = null;
    private ScriptSpec aggSpec = null;

    /**
     * name assigned to this query by user or ui developer;
     * not necessarily unique
     * */
    private String userDefinedName = "Query";

    /**
     *  Create a new AggregationQuery, initializing only the fundamental type
     *  as either TRANSIENT or PERSISTENT.
     *  @param queryType indicator of TRANSIENT or PERSISTENT status
     */
    public AggregationQuery(QueryType queryType)
    {
        this.queryType = queryType;
    }

    /**
     *  Create a new AggregationQuery, initializing it with data found in an XML
     *  document.
     */
    public AggregationQuery(Element root)
    {
      queryType = QueryType.fromString(root.getAttribute(TYPE_ATT));
      updateMethod =
        UpdateMethod.fromString(root.getAttribute(UPDATE_ATT));
      pullRate = Integer.parseInt(root.getAttribute(PULL_RATE_ATT));
      userDefinedName = root.getAttribute(NAME_ATT);

      NodeList nl = root.getElementsByTagName(CLUSTER_TAG);
      for (int i = 0; i < nl.getLength(); i++)
      {
        addSourceCluster(nl.item(i).getFirstChild().getNodeValue().trim());
      }

      nl = root.getElementsByTagName(ScriptType.UNARY_PREDICATE.toString());
      if (nl.getLength() > 0)
        predicateSpec = new ScriptSpec((Element) nl.item(0));

      nl = root.getElementsByTagName(ScriptType.INCREMENT_FORMAT.toString());
      if (nl.getLength() > 0)
        formatSpec = new ScriptSpec((Element) nl.item(0));

      nl = root.getElementsByTagName(ScriptType.AGGREGATOR.toString());
      if (nl.getLength() > 0)
        aggSpec = new ScriptSpec((Element) nl.item(0));
    }

    public void setName(String name)
    {
      userDefinedName = name;
    }

    public String getName()
    {
      return userDefinedName;
    }

    public QueryType getType()
    {
      return queryType;
    }

    public void setUpdateMethod(UpdateMethod updateMethod)
    {
      this.updateMethod = updateMethod;
    }

    public UpdateMethod getUpdateMethod()
    {
      return updateMethod;
    }

    public void setPullRate(int pullRate)
    {
      this.pullRate = pullRate;
    }

    public int getPullRate()
    {
      return pullRate;
    }

    public void addSourceCluster(String clusterID)
    {
        sourceClusters.add(clusterID);
    }

    public Enumeration getSourceClusters()
    {
        return sourceClusters.elements();
    }

    public ScriptSpec getPredicateSpec () {
      return predicateSpec;
    }

    public void setPredicateSpec (ScriptSpec ss) {
      predicateSpec = ss;
    }

    public ScriptSpec getFormatSpec () {
      return formatSpec;
    }

    public void setFormatSpec (ScriptSpec ss) {
      formatSpec = ss;
    }

    public ScriptSpec getAggSpec () {
      return aggSpec;
    }

    public void setAggSpec (ScriptSpec ss) {
      aggSpec = ss;
    }

    public String toXml () {
      InverseSax doc = new InverseSax();
      includeXml(doc);
      return doc.toString();
    }

    public void includeXml (InverseSax doc) {
      doc.addElement(QUERY_TAG);
      doc.addAttribute(TYPE_ATT, queryType.toString());
      doc.addAttribute(UPDATE_ATT, updateMethod.toString());
      doc.addAttribute(PULL_RATE_ATT, String.valueOf(pullRate));
      doc.addAttribute(NAME_ATT, userDefinedName);

      for (int i = 0; i < sourceClusters.size(); i++)
        doc.addTextElement(CLUSTER_TAG, sourceClusters.elementAt(i).toString());

      includeScriptXml(doc);
      if (aggSpec != null)
        aggSpec.includeXml(doc);
      doc.endElement();
    }

    public void includeScriptXml (InverseSax doc) {
      predicateSpec.includeXml(doc);
      formatSpec.includeXml(doc);
    }

    public String toString()
    {
      return userDefinedName;
    }

    private Timer pullTimer;
    public void setPullTimer(Timer newPullTimer) {
      pullTimer = newPullTimer;
    }
    public java.util.Timer getPullTimer() {
      return pullTimer;
    }

    public static void main(String args[])
    {
        AggregationQuery aq = new AggregationQuery(QueryType.PERSISTENT);
        aq.setName("Test Query");
        aq.addSourceCluster("Cluster 1");
        aq.addSourceCluster("Cluster 2");
        aq.addSourceCluster("Cluster 3");
        aq.setPredicateSpec(new ScriptSpec(
          ScriptType.UNARY_PREDICATE, Language.SILK, "SOME SCRIPT"));
        aq.setFormatSpec(new ScriptSpec(
          Language.SILK, XmlFormat.XMLENCODER, "SOME OTHER SCRIPT"));

        String xmlQuery = aq.toXml();
        System.out.println(xmlQuery);

        try
        {
          Element qr = XmlUtils.parse(xmlQuery);
          AggregationQuery aq2 = new AggregationQuery(qr);
          System.out.println(aq2.toXml());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
    }
}
