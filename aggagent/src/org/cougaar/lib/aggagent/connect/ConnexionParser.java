/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.lib.aggagent.connect;

import java.io.BufferedReader;
import java.io.StringWriter;

/**
ConnexionParser, for example enables attachment of a parser/filter which can regulate keep-alive connections --
ie. stop and return data/control to connection manager
when encounter "delimiter marker" between documents in stream.

Parsers specializations can
chunk other streaming data sources/types.  These parsers intended for any
serious transform of data (use transform plugins).
these parsers/filters should be light-weiight and focused on simple reactive action.
**/


public interface ConnexionParser {

    // parse input and write to writer
    //
    //  @return boolean true if connexion still open, else false
    //
    public boolean parse(StringWriter writer, BufferedReader input);
}
