@echo OFF
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


set MYPROPERTIES=  -Dalp.domain.AGG=alp.ui.aggserver.ldm.Domain


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