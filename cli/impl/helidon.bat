@REM
@REM Copyright (c) 2022, 2023 Oracle and/or its affiliates.
@REM
@REM Licensed under the Apache License, Version 2.0 (the "License");
@REM you may not use this file except in compliance with the License.
@REM You may obtain a copy of the License at
@REM
@REM     http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
SETLOCAL

if NOT "%JAVA_HOME%"=="" set JAVACMD=%JAVA_HOME%\bin\java
if "%JAVACMD%"=="" set JAVACMD=java

@REM Find script base directory
for %%i in ("%~dp0..") do set "BASEDIR=%%~fi"

set JARFILE=%BASEDIR%\lib\helidon.jar
set argCount=0
set args=
set attach=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005
set attachMvn=-Dmvn.debug.port=5006
set attachMvnChild=-Dmvn.child.debug.port=5007
set attachPlugin=-Dplugin.debug.port=5006

for %%x in (%*) do (
   call :parseArgs %%~x
)

%JAVACMD% %JAVA_OPTS% -jar %JARFILE% %args%

ENDLOCAL
goto :eof

:parseArgs
if "--attach" == "%~1" set args=%args% %attach% & exit /b
if "--attachMvn" == "%~1" set args=%args% %attachMvn% & exit /b
if "--attachMvnChild" == "%~1" set args=%args% %attachMvnChild% & exit /b
if "--attachPlugin" == "%~1" set args=%args% %attachPlugin% & exit /b
if "--dryRun" == "%~1" set JAVACMD=echo & exit /b
set args=%args% %~1
exit /b

exit /B 0
