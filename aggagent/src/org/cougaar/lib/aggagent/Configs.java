/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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

