@echo off

setlocal

set NODE=PushNode

rem for some reason ..\..\..\classes does not work; must use absolute path
set DEVPATH=..\..\..\classes
set DEVPATH=c:\alp\aggagent\classes

set BASELIB=%COUGAAR_INSTALL_PATH%\lib
set SYSLIB=%COUGAAR_INSTALL_PATH%\sys
set EXECLASS=org.cougaar.core.node.Node
rem set FLAGS=-Dorg.cougaar.class.path=%BASELIB%\*.jar
set NODEARGS=-c -n %NODE%

set CPATH=
set CPATH=%CPATH%;%DEVPATH%
set CPATH=%CPATH%;%BASELIB%\core.jar
set CPATH=%CPATH%;%BASELIB%\jsse.jar
set CPATH=%CPATH%;%BASELIB%\log4j.jar
set CPATH=%CPATH%;%BASELIB%\planserver.jar
set CPATH=%CPATH%;%SYSLIB%\xerces.jar
set CPATH=%CPATH%;%SYSLIB%\silk.jar
set CPATH=%CPATH%;%SYSLIB%\jpython.jar
set CPATH=%CPATH%;C:\program files\jdk1.3\jre\lib\jaws.jar

@echo on
java -Dorg.cougaar.useBootstrapper=false -cp "%CPATH%" %FLAGS% %EXECLASS% %NODEARGS%

@echo on
