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
package org.cougaar.lib.aggagent.bsax; 

import java.util.Vector;
import java.util.Iterator;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;

//
// use "Property" get/test methods where applicable.
//      getProperty()
//      equalsProperty()
//
// "Properties" are an abstraction over "attribute" and "sub-element"
// semantics...
//
// The "property" of an element can be an attribute or sub-element.
// example:
//      1. <foo bar="xxx">...</foo>
//      2. <foo>
//            <bar>xxx</bar>
//         </foo>
//  for either case (above), getProperty("bar") returns "xxx"
//

public class BElement {

  protected String myLocalName = "";
  protected String myURI = "";
  protected String myRawName = "";
  protected Vector mySubElements = new Vector();
  protected String myData = null;
  //protected org.xml.sax.Attributes myAttributes = null;
  protected HashMap myAttributes = new HashMap();

  // protected BElement myParent = null;
  
  //--------------------------------------------------------------------
  public BElement(String localName, String uri,  String rawname, org.xml.sax.Attributes attr) {
      myLocalName = localName;
      myURI = uri;
      myRawName = rawname;
      myAttributes = attributesAsHashMap(attr);
  }

  //--------------------------------------------------------------------
  // equalsProperty() returns TRUE IFF (1.) OR (2.) is true:
  //
  // 1. element has 'property' attribute defined
  //      AND it is equal to 'value' parameter
  // 2. contains subelement whose getLocalName() == 'property'
  //      AND whose getMyData() == 'value' parameter
  //
  public boolean equalsProperty( String property, String value ){
         String p = getProperty(property);
         if( p != null )
         {   return p.equals(value);
         }
         return false;
  }

  //--------------------------------------------------------------------
  //  returns "property" associated with this element.
  //  Property can be expressed as attribute or subelement
  //  See equalsProperty()
  //
  public String getProperty(String property) {

     String a = null;
     BElement be = null;

     // ~~~~~~~ 1.
     if( (a = getAttribute(property)) != null){
        return a;
     }
     // ~~~~~~~ 2.
     else if ( (be = findSubElementByLocalName(property)) != null ){
        return be.getMyData();
     }
     return null;
  }

  //--------------------------------------------------------------------
  //  removes "property" associated with this element.
  //  Property can be expressed as attribute or subelement
  //  See equalsProperty()
  //  returns true if property found and successful else false.
  //
  public boolean removeProperty(String property) {

     String a = null;
     BElement be = null;

     // ~~~~~~~ 1.
     if( (a = getAttribute(property)) != null){
        this.getMyAttributes().remove(property);
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% removing attribute " + property);
        return true;
     }
     // ~~~~~~~ 2.
     else if ( (be = findSubElementByLocalName(property)) != null ){
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% removing subelement " + property);
        this.getMySubElements().remove(be);
        return true;
     }
     //else {
     //   System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% unable to remove property " + property);
     //}
     return false;
  }

  //--------------------------------------------------------------------
  //  finds sub-element by "property"
  //  Property can be expressed as attribute or subelement on subelement
  //  See equalsProperty()
  //
  public BElement findSubElementByProperty( String property )
  {
      Iterator it = mySubElements.iterator();
      while( it.hasNext() )
      {
         BElement be = (BElement)it.next();
         if( be.getProperty(property) != null ) return be;
      }
      return null;
  }

 


  public String getLocalName(){
      return myLocalName;
  }

  public String getAttribute(String attributekey) {
      return (String)myAttributes.get(attributekey);
  }

  public void addChild( BElement c ){
       mySubElements.addElement(c);
  }

  public int getNumberChildren() {
      return mySubElements.size();
  }

  public Vector getMySubElements() {
      return mySubElements;
  }

  public HashMap getMyAttributes() {
      return myAttributes;
  }

  public BElement findSubElementByLocalName( String localname )
  {
      Iterator it = mySubElements.iterator();
      while( it.hasNext() )
      {
         BElement be = (BElement)it.next();
         if( be.getLocalName().equals(localname) ) return be;
      }
      return null;
  }

  public void setMyData(String data){
      myData = data;
  }

  // returns null if no data set
  public String getMyData() {
      return myData;
  }

  /**
  public void addParent(BElement p) {
      myParent = p;
  }
  **/

  //
  // recursively applied -- remove properties from object tree
  // used to clean-up after language extension features
  //
  public void stripProperty(String property)
  {
     _strip(this, property);
  }

  private static void _strip(BElement obj, String property)
  {
       obj.removeProperty(property);
       
       int num_subelems = obj.getMySubElements().size();
       if( (num_subelems > 0) ) {
             Iterator it = obj.mySubElements.iterator();
             while(it.hasNext()){
                 _strip((BElement)it.next(), property);
             }
       }
  }

  //
  //  Text whitespaces NOT used
  public void print( PrintWriter pw )
  {
      _print(pw, this);
  }

  //
  //  Text whitespaces used
  public void prettyprint( PrintWriter pw )
  {
      _pprint(pw, this, "");
  }

 //
  // recursively applied --print tree w/out whitespaces and carriage returns
  //
  private static void _print(PrintWriter pw, BElement obj )
  {
       // String of all attributes
       String all_attribs = (String)attributesAsString(obj.myAttributes );
       //
       // data_character:  <foo>data</foo>
       // may be null if no data (eg. only attributes or subelements)
       //
       String data_character = obj.getMyData();
       int num_subelems = obj.getMySubElements().size();

       //
       // We represent element within single tag "<foo/>" IFF
       // there are no sub-elements or data.  Assume existance of
       // sub-elemetns and data are mutually exclusive
       //

       // CASE: SUB-ELEMENTS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       if( (num_subelems > 0) ) {
             pw.print("<" + obj.myLocalName + " " + all_attribs + ">" );
             Iterator it = obj.mySubElements.iterator();
             while(it.hasNext()){
                 _print(pw,((BElement)it.next()));
             }
             pw.print("</" + obj.myLocalName + ">");
       }
       // CASE: DATA %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       else if((data_character!=null) ) {
             pw.print("<" + obj.myLocalName + " " + all_attribs + ">" );
             pw.print(data_character.trim());
             pw.print("</" + obj.myLocalName + ">");
       }
       // CASE: fit everything into single TAG %%%%%%%%
       else {
             pw.print("<" + obj.myLocalName + " " + all_attribs + "/>" );
       }
       pw.flush();
  }


  //
  // recursively applied -- pretty print object tree.
  //
  private static void _pprint(PrintWriter pw, BElement obj, String prefix )
  {
       // String of all attributes
       String all_attribs = (String)attributesAsString(obj.myAttributes );
       //
       // data_character:  <foo>data</foo>
       // may be null if no data (eg. only attributes or subelements)
       //
       String data_character = obj.getMyData();
       int num_subelems = obj.getMySubElements().size();

       //
       // We represent element within single tag "<foo/>" IFF
       // there are no sub-elements or data.  Assume existance of
       // sub-elemetns and data are mutually exclusive
       //

       // CASE: SUB-ELEMENTS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       if( (num_subelems > 0) ) {
             pw.println(prefix + "<" + obj.myLocalName + " " + all_attribs + ">" );
             Iterator it = obj.mySubElements.iterator();
             while(it.hasNext()){
                 _pprint(pw,((BElement)it.next()),"   " + prefix);
             }
             pw.println(prefix + "</" + obj.myLocalName + ">");
       }
       // CASE: DATA %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       else if((data_character!=null) ) {
             pw.print(prefix + "<" + obj.myLocalName + " " + all_attribs + ">" );
             pw.print(data_character);
             pw.println(prefix + "</" + obj.myLocalName + ">");
       }
       // CASE: fit everything into single TAG %%%%%%%%
       else {
             pw.println(prefix + "<" + obj.myLocalName + " " + all_attribs + "/>" );
       }

       pw.flush();
  }

  private static String  attributesAsString( HashMap attribs )
  {
       String all = new String();
       Set keys = (Set)attribs.keySet();
       Iterator it = keys.iterator();
       while(it.hasNext() )
       {
          String key = (String)it.next();
          String value = (String)attribs.get(key);
          all += " " + key + "=\"" + value + "\"";
       }
       return all;
  }

  private static HashMap attributesAsHashMap(org.xml.sax.Attributes attributes)
  {
      HashMap map = new HashMap();
      int sz = attributes.getLength();
      int i;
      for(i=0; i<sz; i++) {
          map.put(attributes.getLocalName(i), attributes.getValue(i));
      }
      return map;
  }

}