@echo off

REM "<copyright>"
REM " Copyright 2003 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects Agency (DARPA)."
REM ""
REM " This program is free software; you can redistribute it and/or modify"
REM " it under the terms of the Cougaar Open Source License as published by"
REM " DARPA on the Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS"
REM " PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR"
REM " IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF"
REM " MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT"
REM " ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT"
REM " HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL"
REM " DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,"
REM " TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR"
REM " PERFORMANCE OF THE COUGAAR SOFTWARE."
REM "</copyright>"


setlocal

set NODE=PushNode

set DEVPATH=c:\alp\aggagent\classes

set BASELIB=%COUGAAR_INSTALL_PATH%\lib
set SYSLIB=%COUGAAR_INSTALL_PATH%\sys
set EXECLASS=org.cougaar.core.node.Node
rem set FLAGS=-Dorg.cougaar.class.path=%BASELIB%\*.jar
set FLAGS=-Dorg.cougaar.useBootstrapper=false
set FLAGS=%FLAGS% -Dorg.cougaar.core.servlet.enable=true
set FLAGS=%FLAGS% -Dorg.cougaar.core.logging.config.filename=logconf.lcf
set FLAGS=%FLAGS% -Dorg.cougaar.lib.web.https.port=-1
set FLAGS=%FLAGS% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%
set NODEARGS=-c -n %NODE%

set CPATH=
set CPATH=%CPATH%;%DEVPATH%
set CPATH=%CPATH%;%BASELIB%\bootstrap.jar
set CPATH=%CPATH%;%BASELIB%\util.jar
set CPATH=%CPATH%;%BASELIB%\core.jar
set CPATH=%CPATH%;%BASELIB%\glm.jar
set CPATH=%CPATH%;%BASELIB%\webserver.jar
set CPATH=%CPATH%;%BASELIB%\webtomcat.jar
set CPATH=%CPATH%;%SYSLIB%\servlet.jar
set CPATH=%CPATH%;%SYSLIB%\tomcat_40.jar
set CPATH=%CPATH%;%SYSLIB%\log4j.jar
set CPATH=%CPATH%;%SYSLIB%\jsse.jar
set CPATH=%CPATH%;%SYSLIB%\xerces.jar
set CPATH=%CPATH%;%SYSLIB%\silk.jar
set CPATH=%CPATH%;%SYSLIB%\jpython.jar

@echo on
java -cp "%CPATH%" %FLAGS% %EXECLASS% %NODEARGS%

@echo on
