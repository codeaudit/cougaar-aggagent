
package org.cougaar.lib.aggagent.util;

import java.util.*;

/**
 *  Typesafe enumerated types used by AssessmentQuery
 */
public class Enum {
  private final String enumName;

  private Enum (String name) {
    enumName = name;
  }

  public String toString () {
    return enumName;
  }

  public boolean equals (Object obj) {
    return (obj == this);
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
    public static final ScriptType ALERT = new ScriptType("alert_script");

    public static Collection getValidValues () {
      return new LinkedList(validValues);
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

    public static XmlFormat fromString(String enumName)
    {
      return (XmlFormat)findEnum(validValues, enumName);
    }

    public static Collection getValidValues()
    {
      return (Collection)validValues.clone();
    }
  }
}