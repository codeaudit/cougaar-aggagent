package org.cougaar.lib.aggagent.util;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

import org.cougaar.lib.planserver.HttpInput;

/**
 *  <p>
 *  AdvancedHttpInput is a class of wrappers for HttpInput instances.  Its
 *  purpose is to add some badly needed functionality not found in the
 *  HttpInput class.  In particular, an AdvancedHttpInput automatically parses
 *  URL parameters (which it then provides access to through a
 *  programmer-friendly interface) and has a similar ability to parse HTML
 *  forms that may arrive via HTTP POST.
 *  </p><p>
 *  These features save a lot of hassle in dealing with the HttpInput instances
 *  encountered in PSP invocations.
 *  </p>
 */
public class AdvancedHttpInput {
  private HttpInput in = null;
  private HashMap params = new HashMap();
  private HashSet pragmas = new HashSet();

  private HashMap formParams = null;
  private HashSet formPragmas = null;

  public AdvancedHttpInput (HttpInput h) {
    in = h;
    String p = in.getRawParameters();
    if (p != null)
      findParameters(in.getRawParameters(), params, pragmas);
  }

  /**
   *  Check to see if a keyword appears alone in the URL.  If it appears as
   *  a parameter (i.e., it has an associated value separated by an "=" sign),
   *  then this method will return false.
   */
  public boolean hasKeyword (String s) {
    return pragmas.contains(s);
  }

  /**
   *  Check to see if a keyword appears in the URL.  It may appear alone or as
   *  a parameter with an associated value separated by an "=" sign.
   */
  public boolean hasParameter (String s) {
    return pragmas.contains(s) || params.containsKey(s);
  }

  /**
   *  Retrieve the value of a parameter found in the URL.  If the name appeared
   *  without a value or not at all, then this method returns null.
   */
  public String getParameter (String s) {
    return (String) params.get(s);
  }

  /**
   *  Check the posted HTML form for a specified keyword appearing without an
   *  associated value.  Note that this method is legal only after the
   *  parseFormBody() method (q.v.) has been called.  A NullPointerException
   *  will be generated if this method is called before parseFormBody().
   */
  public boolean hasFormKeyword (String s) {
    return formPragmas.contains(s);
  }

  /**
   *  Check the posted HTML form for a specified keyword appearing with or
   *  without a value.  Note that this method is legal only after the
   *  parseFormBody() method (q.v.) has been called.  A NullPointerException
   *  will be generated if this method is called before parseFormBody().
   */
  public boolean hasFormParameter (String s) {
    return formPragmas.contains(s) || formParams.containsKey(s);
  }

  /**
   *  Retrieve the value of a parameter in the posted HTML form.  If the name
   *  appears without a value or not at all, then this method will return null.
   *  Note that this method is legal only after the parseFormBody() method
   *  (q.v.) has been called.  A NullPointerException will be generated if this
   *  method is called before parseFormBody().
   */
  public String getFormParameter (String s) {
    return (String) formParams.get(s);
  }

  /**
   *  Expose the underlying HttpInput instance, if desired.
   */
  public HttpInput getHttpInput () {
    return in;
  }

  /**
   *  <p>
   *  Wrap the HTTP POST body in a BufferedReader for the client to parse on
   *  its own.  This method may be used in cases where the POST was not of an
   *  HTML form.
   *  </p><p>
   *  It should be noted that the implementation of the PlanServer architecture
   *  is such that the HttpInput instance already possesses the entire body of
   *  the POST as a byte array.  The BufferedReader is just a more convenient
   *  way of reading the contents of this array.  No network resources are
   *  being used here.
   *  </p>
   */
  public BufferedReader getBufferedReader () {
    return new BufferedReader(
      new InputStreamReader(new ByteArrayInputStream(in.getBody())));
  }

  /**
   *  Parse the body of the HttpInput instance as an HTML form submission.
   *  By default, this action is not performed by the constructor, but it must
   *  be invoked before calling "getFormParameter", "hasFormParameter", or
   *  "hasFormKeyword".  If the submission is not an HTML form, then there is
   *  no reason to expect the results of this operation to be valid.  The
   *  caller is responsible for knowing the difference.
   */
  public void parseFormBody () {
    if (formParams != null)
      return;

    formParams = new HashMap();
    formPragmas = new HashSet();

    findParameters(in.getBodyAsString(), formParams, formPragmas);
  }

  private static void findParameters (String b, HashMap m, HashSet s) {
    ParamParser pp = new ParamParser(b);
    Parameter p = null;
    while ((p = pp.nextParameter()) != null) {
      if (p.value == null)
        s.add(p.name);
      else
        m.put(p.name, p.value);
    }
  }
}