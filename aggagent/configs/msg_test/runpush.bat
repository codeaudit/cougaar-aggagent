@echo off

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
set CPATH=%CPATH%;%BASELIB%\core.jar
set CPATH=%CPATH%;%BASELIB%\glm.jar
set CPATH=%CPATH%;%BASELIB%\webserver.jar
set CPATH=%CPATH%;%BASELIB%\webtomcat.jar
set CPATH=%CPATH%;%SYSLIB%\servlet.jar
set CPATH=%CPATH%;%SYSLIB%\tomcat_40.jar
set CPATH=%CPATH%;%SYSLIB%\log4j.jar
set CPATH=%CPATH%;%SYSLIB%\jsse.jar
set CPATH=%CPATH%;%SYSLIB%\xercesImpl.jar
set CPATH=%CPATH%;%SYSLIB%\xml-apis.jar
set CPATH=%CPATH%;%SYSLIB%\silk.jar
set CPATH=%CPATH%;%SYSLIB%\jpython.jar
set CPATH=%CPATH%;C:\program files\jdk1.3\jre\lib\jaws.jar

@echo on
java -cp "%CPATH%" %FLAGS% %EXECLASS% %NODEARGS%

@echo on
