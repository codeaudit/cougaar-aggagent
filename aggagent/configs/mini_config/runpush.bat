@echo OFF

REM "<copyright>"
REM " "
REM " Copyright 2003-2004 BBNT Solutions, LLC"
REM " under sponsorship of the Defense Advanced Research Projects"
REM " Agency (DARPA)."
REM ""
REM " You can redistribute this software and/or modify it under the"
REM " terms of the Cougaar Open Source License as published on the"
REM " Cougaar Open Source Website (www.cougaar.org)."
REM ""
REM " THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS"
REM " "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT"
REM " LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR"
REM " A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT"
REM " OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,"
REM " SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT"
REM " LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,"
REM " DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY"
REM " THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT"
REM " (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE"
REM " OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
REM " "
REM "</copyright>"


if "%COUGAAR_INSTALL_PATH%"=="" goto AIP_ERROR

set LIBPATHS=%COUGAAR_INSTALL_PATH%\lib\bootstrap.jar

REM pass in "NodeName" to run a specific named Node

set MYPROPERTIES= -Dorg.cougaar.system.path=%COUGAAR_INSTALL_PATH%\sys -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH% -Dorg.cougaar.core.servlet.enable=true -Dorg.cougaar.lib.web.scanRange=100 -Dorg.cougaar.lib.web.http.port=8800 -Dorg.cougaar.lib.web.https.port=-1 -Dorg.cougaar.lib.web.https.clientAuth=true -Dorg.cougaar.core.logging.config.filename=logconf.lcf

set MYMEMORY=
set MYCLASSES=org.cougaar.bootstrap.Bootstrapper org.cougaar.core.node.Node
set MYARGUMENTS= -c -n "PushNode"


@ECHO ON

java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %2 %3
goto QUIT

:AIP_ERROR
echo Please set COUGAAR_INSTALL_PATH
goto QUIT

:QUIT
