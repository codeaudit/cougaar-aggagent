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
package org.cougaar.lib.aggagent;

import java.util.*;
import java.net.*;
import java.io.*;





//  Configuration assumptions associated with Aggregation Agent
//
//  Long run,  should make this configurable
//  eg. migrate to runtime settable property
//  but for now,  keeping it simple.
//
public final class Configs
{

     // Consumed at:  AggregatorPlugin.java
     public static String AGGREGATOR_DEFAULT_CONNECTION_CONFIG_FILE_NAME = "aggregator.configs.xml";

     // Consumed at: GLDictionary.java
     public static String AGGREGATION_CLUSTER_NAME_PREFIX = "AGG";
     public static String GENERIC_PSP_SOCIETY_CLUSTER_PRIMITIVES = "society_glprimitives.xml";
     public static String GENERIC_PSP_AGG_CLUSTER_PRIMITIVES = "aggregator_glprimitives.xml";


}

