/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

package org.cougaar.lib.aggagent.util;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *  Typesafe enumerated types used by AggregationQuery
 */
public abstract class Enum implements Serializable {
  private String enumName;

  protected Enum (String name) {
    enumName = name;
  }

  public String toString () {
    return enumName;
  }

  protected abstract String getStringObject(String name);
  
  public boolean equals (Object obj) {
    if (obj == this)
      return true;

    // Can't just return true if objects are == because one may have been serialized.
    if (!(obj instanceof Enum))
      return false;

    return (((Enum)obj).enumName == enumName);
  }

  private void readObject (ObjectInputStream ois) throws java.io.IOException, ClassNotFoundException { 
    enumName = (String) ois.readObject();
    enumName = getStringObject(enumName);
  }

  protected static Object findEnum (Collection col, String name) {
    for (Iterator i = col.iterator(); i.hasNext();) {
      Object nextEnum = i.next();
      if (name.equals(nextEnum.toString()))
        return nextEnum;
    }
    return null;
  }

  public static class ScriptType extends Enum {
    private static final Collection validValues = new LinkedList();

    public static final ScriptType UNARY_PREDICATE = new ScriptType("unary_predicate");
    public static final ScriptType INCREMENT_FORMAT = new ScriptType("xml_encoder");
    public static final ScriptType AGGREGATOR = new ScriptType("aggregator");
    public static final ScriptType ALERT = new ScriptType("alert_script");

    public static Collection getValidValues () {
      return new LinkedList(validValues);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }

    public static ScriptType fromString (String enumName) {
      return (ScriptType) findEnum(validValues, enumName);
    }

    private ScriptType (String name) {
      super(name);
      validValues.add(this);
    }
  }

  public static class QueryType extends Enum
  {
    private static final LinkedList validValues = new LinkedList();
    public static  final QueryType TRANSIENT = new QueryType("Transient");
    public static  final QueryType PERSISTENT = new QueryType("Persistent");

    private QueryType(String name)
    {
      super(name);
      validValues.add(this);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }
    
    public static QueryType fromString(String enumName)
    {
      return (QueryType)findEnum(validValues, enumName);
    }

    public static Collection getValidValues()
    {
      return (Collection)validValues.clone();
    }
  }

  public static class UpdateMethod extends Enum
  {
    private static final LinkedList validValues = new LinkedList();
    public static final UpdateMethod PUSH = new UpdateMethod("Push");
    public static final UpdateMethod PULL = new UpdateMethod("Pull");

    private UpdateMethod(String name)
    {
      super(name);
      validValues.add(this);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }

    public static UpdateMethod fromString(String enumName)
    {
      return (UpdateMethod)findEnum(validValues, enumName);
    }

    public static Collection getValidValues()
    {
      return (Collection)validValues.clone();
    }
  }

  public static class Language extends Enum
  {
    private static final LinkedList validValues = new LinkedList();
    public static final Language SILK    = new Language("SILK");
    public static final Language JPYTHON = new Language("JPython");
    public static final Language JAVA    = new Language("Java");

    private Language(String name)
    {
      super(name);
      validValues.add(this);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }

    public static Language fromString(String enumName)
    {
      return (Language)findEnum(validValues, enumName);
    }

    public static Collection getValidValues()
    {
      return (Collection)validValues.clone();
    }
  }

  public static class XmlFormat extends Enum
  {
    private static final LinkedList validValues = new LinkedList();
    public static final XmlFormat INCREMENT =  new XmlFormat("Increment");
    public static final XmlFormat XMLENCODER = new XmlFormat("XMLEncoder");

    private XmlFormat(String name)
    {
      super(name);
      validValues.add(this);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }

    public static XmlFormat fromString(String enumName)
    {
      return (XmlFormat)findEnum(validValues, enumName);
    }

    public static Collection getValidValues()
    {
      return (Collection)validValues.clone();
    }
  }

  public static class AggType extends Enum {
    private static final LinkedList validValues = new LinkedList();
    public static final AggType AGGREGATOR =  new AggType("Aggregator");
    public static final AggType MELDER = new AggType("Melder");

    private AggType (String name) {
      super(name);
      validValues.add(this);
    }

    protected String getStringObject(String enumName)
    {
      Enum enum = (Enum) findEnum(validValues, enumName);
      return enum == null ? null : enum.toString();
    }

    public static AggType fromString (String enumName) {
      return (AggType) findEnum(validValues, enumName);
    }

    public static Collection getValidValues () {
      return (Collection) validValues.clone();
    }
  }
}
