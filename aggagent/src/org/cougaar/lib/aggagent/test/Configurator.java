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

package org.cougaar.lib.aggagent.test;

import java.io.*;
import java.util.*;

/**
 *  <p>
 *  The Configurator is a utility for producing test configurations of
 *  arbitrary size.  When run as a standalone application (which is the most
 *  likely scenario), this class requires an initialization file, which may be
 *  specified on the command line (or else the user will be prompted).  The
 *  output, which is placed in the specified destination directory (see below),
 *  consists of directories named cfgAggregator and cfgNode<it>n</it>, where
 *  <it>n</it> ranges from 0 to one less than the number of specified source
 *  Nodes.  The former contains the AggregationAgent Node config., and each of
 *  the latter is a source Node configuration.  All of these nodes should be
 *  executed as part of a single society for purposes of testing.
 *  </p><p>
 *  The format of the initialization file is as a sequence of parameters given
 *  as "&lt;name&gt;=&lt;value&gt;" pairs.  Lines in the file not containing
 *  "=" or starting with "#" are presumed to be comments and are ignored.
 *  Following are the parameters currently recognized by the interpreter:
 *  <ul>
 *    <li>
 *      destination -- the directory into which the output is to be written
 *    </li>
 *    <li>
 *      aggagentCp -- path to the aggagent.jar file or the compiled aggagent
 *      class files.
 *    </li>
 *    <li>
 *      nameServer -- the host of the name server.  At least one node should
 *      be executed on this host.
 *    </li>
 *    <li>
 *      windows -- provide support for windows-style run scripts.  The value
 *      "yes" indicates that windows scripts should be produced, and any other
 *      value is equivalent to "no".  Presently, only windows is supported, and
 *      this parameter is ignored.
 *    </li>
 *    <li>
 *      linux -- provide support for linux-style run scripts.  The value "yes"
 *      indicates that linux scripts should be produced, and any other value is
 *      equivalent to "no".  Presently, only windows is supported, and this
 *      parameter is ignored.
 *    </li>
 *    <li>
 *      shellPath -- in case linux support is turned on, this value is the
 *      path to the script interpreter, e.g., "/bin/csh".
 *    </li>
 *    <li>
 *      nodes -- the number of Node configurations to be generated
 *    </li>
 *    <li>
 *      clustersPerNode -- the number of clusters operating on each node
 *    </li>
 *    <li>
 *      pauseInterval -- a parameter that regulates the amount of work
 *      performed by each Cluster.  Literally, this is the amount of time that
 *      the EffortWaster Plugin pauses between updates of the blackboard state.
 *    </li>
 *    <li>
 *      maxCycles -- another workload regulator.  This is the maximum number of
 *      objects to be managed by the EffortWaster Plugin on each Cluster, and
 *      consequently also the number of things that must be transmitted to the
 *    </li>
 *  </ul>
 *  </p>
 */
public class Configurator {

// - - - - - - - Static Configuration - - - - - - - - - - - - - - - - - - - - -
  private static List sourcePsps = new LinkedList();
  private static List aggNames = new LinkedList();
  private static List aggPlugins = new LinkedList();
  private static List aggPsps = new LinkedList();
  private static List jarFiles = new LinkedList();
  private static List sysFiles = new LinkedList();

  static {
    // sourcePsps.add(new Psp(
    //   "org.cougaar.lib.aggagent.plugin.GenericPSP", "test/generic.psp"));

    aggNames.add("Aggregator");

    aggPlugins.add("org.cougaar.lib.aggagent.servlet.AggregationComponent(/aggregator)");
    aggPlugins.add("org.cougaar.lib.aggagent.servlet.AggregationKeepAliveComponent(/aggregatorkeepalive)");
    aggPlugins.add("org.cougaar.lib.aggagent.plugin.AggregationPlugin");
    aggPlugins.add("org.cougaar.lib.aggagent.plugin.AlertPlugin");

    jarFiles.add("bootstrap.jar");
    jarFiles.add("util.jar");
    jarFiles.add("core.jar");
    jarFiles.add("webserver.jar");
    jarFiles.add("webtomcat.jar");
    sysFiles.add("tomcat_33.jar");
    sysFiles.add("servlet.jar");
    sysFiles.add("xerces.jar");
    sysFiles.add("silk.jar");
    sysFiles.add("jpython.jar");
    sysFiles.add("jsse.jar");
    sysFiles.add("log4j.jar");
  }

// - - - - - - - Instance Configuration - - - - - - - - - - - - - - - - - - - -
  private ConfigSpec config = null;

// - - - - - - - End of Configuration - - - - - - - - - - - - - - - - - - - - -

  private void writeCluster (File f, String name, List plugIns)
      throws IOException
  {
    PrintStream out = new PrintStream(new FileOutputStream(f));
    out.println("[ Cluster ]");
    out.println("uic = UIC/" + name);
    out.println();
    out.println("[ Plugins ]");

    for (Iterator i = plugIns.iterator(); i.hasNext(); )
      out.println("plugin = " + i.next());

    out.println();
    out.println("[ Policies ]");
    out.println();
    out.println("[ Permission ]");
    out.println();
    out.println("[ AuthorizedOperation ]");
    out.close();
  }

  private void writeNode (File f, List clusters) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(f));
    out.println("# $id$");
    out.println("[ Clusters ]");
    out.println();

    // insert clusters
    for (Iterator i = clusters.iterator(); i.hasNext(); )
      out.println("cluster = " + i.next());

    out.println();
    out.println("[ AlpProcess ]");
    out.println();
    out.println("[ Policies ]");
    out.println();
    out.println("[ Permission ]");
    out.println();
    out.println("[ AuthorizedOperation ]");
    out.close();
  }

  private static class Psp {
    private String className = null;
    private String lookupName = null;

    public Psp (String c, String n) {
      className = c;
      lookupName = n;
    }

    public void printout (PrintStream out) {
      out.println("  <element type=\"org.cougaar.lib.planserver.NamedPSP\">");
      out.println("    <Classname>" + className + "</Classname>");
      out.println("    <PSPName>" + lookupName + "</PSPName>");
      out.println("  </element>");
    }
  }

  private void writePspFile (File f, List psps) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(f));
    out.println("<object type=\"java.util.Vector\">");

    for (Iterator i = psps.iterator(); i.hasNext(); )
      ((Psp) i.next()).printout(out);

    out.println("</object>");
    out.close();
  }

  private void writeDosRunScript (File f, String nodeName) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(f));
    out.println("@echo off");
    out.println();
    out.println("setlocal");
    out.println();
    out.println("set NODE=" + nodeName);
    out.println();
    out.println("set EXECLASS=org.cougaar.core.node.Node");
    out.println("set PROPERTIES=-Dorg.cougaar.useBootstrapper=false");
    out.println("set PROPERTIES=%PROPERTIES% -Dorg.cougaar.core.servlet.enable=true");
    out.println("set PROPERTIES=%PROPERTIES% -Dorg.cougaar.lib.web.https.port=-1");
    out.println("set PROPERTIES=%PROPERTIES% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%");
    out.println("set NODEARGS=-c -n %NODE%");
    out.println();
    out.println("set BJPATH=" + config.aggagentCp);
    out.println("set LIB=%COUGAAR_INSTALL_PATH%\\lib");
    out.println("set SYS=%COUGAAR_INSTALL_PATH%\\sys");

    out.println();
    out.println("set CPATH=");
    out.println("set CPATH=%CPATH%;%BJPATH%");
    for (Iterator i = jarFiles.iterator(); i.hasNext(); )
      out.println("set CPATH=%CPATH%;%LIB%\\" + i.next());
    for (Iterator i = sysFiles.iterator(); i.hasNext(); )
      out.println("set CPATH=%CPATH%;%SYS%\\" + i.next());

    out.println();
    out.println("java -cp %CPATH% %PROPERTIES% %EXECLASS% %NODEARGS%");
    out.println();
    out.println("@echo on");
    out.close();
  }

  private void writeAlpReg (File f, String host) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(f));
    out.println("[ Registry ]");
    out.println("address=" + host);
    out.println("alias=AlpFDS");
    out.println("port=8003");
    out.close();
  }

  private void deleteContents (File f) {
    File[] list = f.listFiles();
    for (int i = 0; i < list.length; i++) {
      File cf = list[i];
      if (cf.isDirectory())
        recursiveDelete(cf);
      else
        cf.delete();
    }
  }

  private void recursiveDelete (File f) {
    deleteContents(f);
    f.delete();
  }

  private List genClusterNames (int nodeNumber) {
    List ret = new LinkedList();
    String prefix = "Source";
    String separator = "_";
    for (int i = 0; i < config.clustersPerNode; i++)
      ret.add(prefix + nodeNumber + separator + i);

    return ret;
  }

  private List genSourcePlugins () {
    List ret = new LinkedList();
    //ret.add(
    //  "org.cougaar.lib.planserver.PlanServerPlugin(file=source.psps.xml)");
    ret.add(
      "org.cougaar.lib.aggagent.test.EffortWaster(maxCycles=" +
      config.maxCycles + ",pauseInterval=" + config.pauseInterval + ")");
    ret.add("org.cougaar.lib.aggagent.plugin.RemoteSubscriptionPlugin");

    return ret;
  }

  private void prepareSourceNode (int nodeNum, List plugIns)
      throws IOException
  {
    prepareNode("cfgNode" + nodeNum, "SourceNode" + nodeNum, "source.psps.xml",
      sourcePsps, genClusterNames(nodeNum), plugIns);
  }

  private void prepareAggNode () throws IOException {
    prepareNode("cfgAggregator", "AggNode", "aggregator.psps.xml", aggPsps,
      aggNames, aggPlugins);
  }

  private void prepareNode (String dirName, String name, String pspFile,
      List psps, List clusters, List plugIns)
      throws IOException
  {
    File dir = new File(config.destination, dirName);
    dir.mkdir();

    File textFile = new File(dir, "alpreg.ini");
    writeAlpReg(textFile, config.nameServer);

    textFile = new File(dir, pspFile);
    writePspFile(textFile, psps);

    if (config.windows) {
      textFile = new File(dir, "run.bat");
      writeDosRunScript(textFile, name);
    }

    textFile = new File(dir, name + ".ini");
    writeNode(textFile, clusters);

    for (Iterator i = clusters.iterator(); i.hasNext(); ) {
      String clusterName = i.next().toString();
      textFile = new File(dir, clusterName + ".ini");
      writeCluster(textFile, clusterName, plugIns);
    }
  }

  private void prepareDestination (File f) throws IOException {
    if (!f.exists())
      f.mkdirs();
    else if (!f.isDirectory())
      throw new IllegalStateException("Non-directory \"" + f + "\" exists");
    else
      deleteContents(f);
  }

  public void execute () {
    if (config == null) {
      System.out.println("This Configurator has no initialization spec");
      return;
    }

    try {
      prepareDestination(config.destination);

      prepareAggNode();

      List plugIns = genSourcePlugins();

      // loop over the number of Nodes and create a directory for each
      for (int i = 0; i < config.nodes; i++)
        prepareSourceNode(i, plugIns);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static class ConfigSpec {
    // pragmatics
    public File destination = null;
    public String aggagentCp = null;

    // gross configurations
    public String nameServer = null;
    public String shellPath = null;
    public boolean windows = false;
    public boolean linux = false;

    // distribution and load
    public int nodes = 0;
    public int clustersPerNode = 0;
    public long pauseInterval = 0;
    public int maxCycles = 0;

    public void setDestination (String s) {
      destination = new File(s);
    }

    public void setBlackjackCp (String s) {
      aggagentCp = s;
    }

    public void setNameServer (String s) {
      nameServer = s;
    }

    public void setShellPath (String s) {
      shellPath = s;
    }

    public void setWindows (String s) {
      windows = "yes".equals(s);
    }

    public void setLinux (String s) {
      linux = "yes".equals(s);
    }

    private static int parseInt (String s, String hist) {
      try {
        return Integer.parseInt(s);
      }
      catch (NumberFormatException nfe) {
        System.out.println(
          "Bad number format for \"" + hist + "\":  \"" + s + "\"");
      }
      return 0;
    }

    public void setNodes (String s) {
      nodes = parseInt(s, "nodes");
    }

    public void setClustersPerNode (String s) {
      clustersPerNode = parseInt(s, "clustersPerNode");
    }

    public void setPauseInterval (String s) {
      pauseInterval = parseInt(s, "pauseInterval");
    }

    public void setMaxCycles (String s) {
      maxCycles = parseInt(s, "maxCycles");
    }

    public ConfigSpec (InputStream in)
        throws IOException, IllegalStateException
    {
      BufferedReader bufr = new BufferedReader(new InputStreamReader(in));
      String line = null;
      while ((line = bufr.readLine()) != null) {
        if (line.startsWith("#"))
          continue;

        int k = line.indexOf('=');
        if (k == -1)
          continue;

        String name = line.substring(0, k);
        String value = line.substring(k + 1);

        if (name.equals("destination"))
          setDestination(value);
        else if (name.equals("aggagentCp"))
          setBlackjackCp(value);
        else if (name.equals("nameServer"))
          setNameServer(value);
        else if (name.equals("shellPath"))
          setShellPath(value);
        else if (name.equals("windows"))
          setWindows(value);
        else if (name.equals("linux"))
          setLinux(value);
        else if (name.equals("nodes"))
          setNodes(value);
        else if (name.equals("clustersPerNode"))
          setClustersPerNode(value);
        else if (name.equals("pauseInterval"))
          setPauseInterval(value);
        else if (name.equals("maxCycles"))
          setMaxCycles(value);
        else
          System.err.println("Unrecognized parameter name \"" + name + "\"");
      }
    }
  }

  public void parseConfigFile (File infile) throws IOException {
    InputStream in = new FileInputStream(infile);
    config = new ConfigSpec(in);
    in.close();
  }

  private static File checkFile (String s) {
    File ret = new File(s);
    if (!ret.exists()) {
      System.out.println("File \"" + s + "\" does not exist.");
      return null;
    }

    if (!ret.isFile()) {
      if (ret.isDirectory())
        System.out.println("\"" + s + "\" is a directory.");
      else
        System.out.println("\"" + s + "\" is not a file.");
      return null;
    }

    if (!ret.canRead()) {
      System.out.println("File \"" + s + "\" is not readable.");
      return null;
    }

    return ret;
  }

  private static BufferedReader bufr =
    new BufferedReader(new InputStreamReader(System.in));

  private static String promptUser (String prompt) {
    try {
      System.out.print(prompt);
      System.out.flush();
      return bufr.readLine();
    }
    catch (IOException eek) {
      System.out.println("Hosed.");
    }

    return null;
  }

  public static void main (String[] argv) {
    File configFile = null;
    if (argv.length > 0)
      configFile = checkFile(argv[0]);
    while (configFile == null) {
      String input = promptUser("Enter config file path:  ");
      if (input == null || input.equalsIgnoreCase("exit"))
        System.exit(0);
      if (input.length() == 0)
        continue;
      configFile = checkFile(input);
    }

    try {
      Configurator gen = new Configurator();
      gen.parseConfigFile(configFile);
      gen.execute();
    }
    catch (IOException ioe) {
      System.out.println("Error reading file \"" + configFile.toString() +
        "\"--" + ioe);
      ioe.printStackTrace();
    }
    catch (IllegalStateException ise) {
      System.out.println("Invalid configuration--" + ise);
    }
  }
}
