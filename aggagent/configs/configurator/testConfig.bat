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
