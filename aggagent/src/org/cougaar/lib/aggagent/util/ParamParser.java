package org.cougaar.lib.aggagent.util;

import java.net.*;
import java.util.*;

public class ParamParser {
  private StringTokenizer tok = null;

  public ParamParser (String input) {
    tok = new StringTokenizer(input, "&?\r\n");
  }

  public Parameter nextParameter () {
    if (tok.hasMoreTokens())
      return newParameter(tok.nextToken());
    return null;
  }

  private static Parameter newParameter (String s) {
    int j = s.indexOf('=');
    if (j == -1)
      return new Parameter(dec(s), null);
    else
      return new Parameter(dec(s.substring(0, j)), dec(s.substring(j + 1)));
  }

  private static String dec (String s) {
    return URLDecoder.decode(s);
  }
}
