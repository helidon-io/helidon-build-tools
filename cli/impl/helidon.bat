@REM
@REM Copyright (c) 2023, 2025 Oracle and/or its affiliates.
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

if not "%JAVA_HOME%" == "" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java"
) else (
    set "JAVA_EXE=java"
)

set "HELIDON_CLI_CMD=%JAVA_EXE%"
if not "%HELIDON_CLI_JAVA_OPTS%"=="" (
    set "HELIDON_CLI_CMD=%HELIDON_CLI_CMD% %HELIDON_CLI_JAVA_OPTS%"
)

@REM Find script base directory
for %%i in ("%~dp0..") do set BASEDIR=%%~fi

set "HELIDON_CLI_CMD=%HELIDON_CLI_CMD% -jar %BASEDIR%\helidon-cli.jar %*"

if /i "%HELIDON_CLI_DEBUG%" == "true" (
    echo.
    echo [DEBUG] Use HELIDON_CLI_JAVA_OPTS environment property to setup JVM arguments
    echo [DEBUG] Distribution located at : %BASEDIR%
    echo [DEBUG] Using java command : %JAVA_EXE%
    echo [DEBUG] Command executed : %HELIDON_CLI_CMD%
    echo.
)

%HELIDON_CLI_CMD%

IF %ERRORLEVEL% neq 0 goto fail
goto end

:fail
exit /b 1

:end
