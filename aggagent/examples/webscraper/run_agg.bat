@echo OFF

REM "<copyright>"
REM " Copyright 2001 BBNT Solutions, LLC"
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

REM $id$

set NODE=AGGNode

REM
REM   xerces.jar MUST PRECEED xml4j_2_0_11.jar ON CLASSPATH
REM

SET COUGAAR_INSTALL_PATH=..\..\..
set LIBPATHS=

set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\aggagent.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\planserver.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\glm.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xerces.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xalan.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xml4j_2_0_11.jar


set MYPROPERTIES=  -Dalp.domain.AGG=org.cougaar.ui.aggserver.ldm.Domain


REM
REM Appending Properties list...
@echo ON
@ECHO ################################################################
@ECHO Resetting ConfigFile reader property == to look in ./scripts
@ECHO then glm/configs/minitestconfig THEN glm/configs/common for 
@ECHO config files not found here.
@ECHO Only config files which have been altered are replicated here.
@ECHO ################################################################
@echo OFF

set MYPROPERTIES=%MYPROPERTIES% -Dorg.cougaar.config.path=.;.\scripts;%COUGAAR_INSTALL_PATH%\glm\configs\minitestconfig;%COUGAAR_INSTALL_PATH%\glm\configs\common


set MYMEMORY=
set MYCLASSES=org.cougaar.core.society.Node
set MYARGUMENTS= -c -n %NODE%
@ECHO ON
java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %2 %3
goto QUIT

:QUIT