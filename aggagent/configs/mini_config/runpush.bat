@echo off

setlocal

set NODE=PushNode

set DEVPATH=c:\alpine\aggregationAgent\aggagent\classes

set BASELIB=%COUGAAR_INSTALL_PATH%\lib
set SYSLIB=%COUGAAR_INSTALL_PATH%\sys
set EXECLASS=org.cougaar.core.node.Node
rem set FLAGS=-Dorg.cougaar.class.path=%BASELIB%\*.jar
set FLAGS=-Dorg.cougaar.useBootstrapper=false
set FLAGS=%FLAGS% -Dorg.cougaar.core.servlet.enable=true
set FLAGS=%FLAGS% -Dorg.cougaar.lib.web.https.port=-1
set FLAGS=%FLAGS% -Dorg.cougaar.install.path=%COUGAAR_INSTALL_PATH%
set NODEARGS=-c -n %NODE%

set CPATH=
set CPATH=%CPATH%;%DEVPATH%
set CPATH=%CPATH%;%BASELIB%\core.jar
set CPATH=%CPATH%;%BASELIB%\webserver.jar
set CPATH=%CPATH%;%BASELIB%\webtomcat.jar
set CPATH=%CPATH%;%SYSLIB%\servlet.jar
set CPATH=%CPATH%;%SYSLIB%\tomcat_33.jar
set CPATH=%CPATH%;%SYSLIB%\log4j.jar
set CPATH=%CPATH%;%SYSLIB%\jsse.jar
set CPATH=%CPATH%;%SYSLIB%\xerces.jar
set CPATH=%CPATH%;%SYSLIB%\silk.jar
set CPATH=%CPATH%;%SYSLIB%\jpython.jar
set CPATH=%CPATH%;C:\program files\jdk1.3\jre\lib\jaws.jar

@echo on
java -cp "%CPATH%" %FLAGS% %EXECLASS% %NODEARGS%

@echo on
