/*
 * <copyright>
 *  
 *  Copyright 2003-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.lib.aggagent.script;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.util.PythonInterpreter;

/**
 *  The purpose of the NoErrorPython class is to be a directly instantiable
 *  subclass of PythonInterpreter within the COUGAAR framework.  Interaction
 *  between COUGAAR's BootstrapClassLoader and JPython's custom ClassLoader
 *  turns out to be fatal (for JPython) unless this class is used.
 *  <br><br>
 *  Note:  This implementation is only necessary because of the way jython 
 *  keeps track of java packages using system properties.
 */
public class NoErrorPython extends PythonInterpreter {
    /**
     * Static initializer tries to initialize python system properties based on cougaar properties. It will gracefully fail if
     * permission is denied, but then you'd better have all the jars you need in java.class.path.
     */
  static {
      try {
          String pathsep = java.io.File.pathSeparator;
          String filesep = java.io.File.separator;
          // "python.packages.path" refers to list of properties that contain class paths.  Add org.cougaar.class.path to that list.
          String ccp = System.getProperty("org.cougaar.class.path");
          if (ccp != null) {
            System.setProperty("python.packages.paths","org.cougaar.lib.aggagent.python.class.path,java.class.path,sun.boot.class.path"); 
            System.setProperty("org.cougaar.lib.aggagent.python.class.path", ccp);
          }

          // "python.packages.directories" refers to list of properties that contain directorys paths.  
          // Add org.cougaar.install.path /lib and /sys to that list.  Also add org.cougaar.system.path for "3rd party" stuff.
          String cip = System.getProperty("org.cougaar.install.path");
          // always add lib and sys directories
          String dirPath = cip+filesep+"lib"+pathsep+cip+filesep+"sys"+pathsep+cip+filesep+"plugins";
          String csp = System.getProperty("org.cougaar.system.path");
          if (csp != null)
              dirPath += pathsep + csp;
          
          System.setProperty("python.packages.directories", "org.cougaar.lib.aggagent.python.directories,java.ext.dirs");
          System.setProperty("org.cougaar.lib.aggagent.python.directories", dirPath);
          
          // jython will write a cache of processed JAR files in "." unless we set "python.cachedir"
          // Set it here unless it is already set.
          if (System.getProperty("python.cachedir") == null) {
              String cougaarWorkspace = System.getProperty("org.cougaar.workspace", cip+java.io.File.separator+"workspace");
              System.setProperty("python.cachedir", cougaarWorkspace+java.io.File.separator+"jythoncache");
          }
      } catch (SecurityException ex) {
          //ex.printStackTrace();
          // Well, we tried.
      }
  }
  /**
   *  Create a new
   */
  public NoErrorPython () {
    Py.getSystemState().setClassLoader(BytecodeLoader.class.getClassLoader());
  }
}
