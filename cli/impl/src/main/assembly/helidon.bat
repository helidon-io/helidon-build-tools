@REM
@REM Copyright (c) 2023 Oracle and/or its affiliates.
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
set args=
set localDebug=

for %%x in (%*) do (
   call :parseArgs %%~x
)

if NOT "%isDebug%"=="" (
    echo "[WARNING] Use HELIDON_JAVA_OPS environment property to setup JVM arguments"
    echo "[DEBUG] Distribution located at : %BASEDIR%"
    echo "[DEBUG] Using java command : %JAVACMD%"
    echo "[DEBUG] Command executed : %JAVACMD% %HELIDON_JAVA_OPTS% -jar %JARFILE% %args%"
)

%JAVACMD% %HELIDON_JAVA_OPTS% -jar %JARFILE% %args%

ENDLOCAL
goto :eof

:parseArgs
if "--cli-debug" == "%~1" (
    set isDebug="true"
) else (
    set args=%args% %~1
)
exit /b

exit /B 0
