@echo off

rem ***
rem *  this program uses one of two formats:
rem *      testConfig <file>
rem *  invokes the program runs following the instructions in the specified
rem *  file.  For details on the contents of the file, see the javadocs on the
rem *  Configurator class.  If testConfig is invoked with no arguments, then
rem *  the user is prompted to enter an appropriate file name.
rem ***

setlocal

set EXE=org.cougaar.lib.aggagent.test.Configurator

set CP=..\..\classes

java -classpath %CP% %EXE% %*

@echo on
