@echo OFF
REM $id$
REM
REM   xerces.jar MUST PRECEED xml4j_2_0_11.jar ON CLASSPATH
REM

set NODE=MiniNode


SET COUGAAR_INSTALL_PATH=..\..\..
set LIBPATHS=

set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\aggagent.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\core.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\configgen.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\planserver.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\glm.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xerces.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xalan.jar
set LIBPATHS=%LIBPATHS%;%COUGAAR_INSTALL_PATH%\lib\xml4j_2_0_11.jar



REM pass in "NodeName" to run a specific named Node
set MYPROPERTIES=-Dalp.domain.alp=mil.darpa.log.alp.domain.ALPDomain 

set MYPROPERTIES=%MYPROPERTIES% -Dorg.cougaar.config.path=.;%COUGAAR_INSTALL_PATH%\glm\configs\minitestconfig;%COUGAAR_INSTALL_PATH%\glm\configs\common

set MYMEMORY=
set MYCLASSES=org.cougaar.core.society.Node
set MYARGUMENTS= -c -n %NODE%
@ECHO ON
java.exe %MYPROPERTIES% %MYMEMORY% -classpath %LIBPATHS% %MYCLASSES% %MYARGUMENTS% %2 %3

goto QUIT

:QUIT