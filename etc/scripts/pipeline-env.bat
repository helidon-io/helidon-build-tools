@REM
@REM Copyright (c) 2020 Oracle and/or its affiliates.
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

if not "%HELIDON_PIPELINES%"=="" (
  set MAVEN_ARGS=%MAVEN_ARGS% -B %MAVEN_HTTP_ARGS% -Djdk.toolchain.version=%JAVA_VERSION%
)

if "%JENKINS_HOME%"=="" exit 0

set JAVA_HOME=C:\tools\graalvm-ce-java11-20.2.0
set MAVEN_HOME=C:\tools\apache-maven-3.6.3
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

set SL4J_ARGS= -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn ^
               -Dorg.slf4j.simpleLogger.showDateTime=true ^
               -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss,SSS

if not "%MAVEN_OPTS%"=="" (
    set MAVEN_OPTS=%MAVEN_OPTS% %SL4J_ARGS%
) else (
    set MAVEN_OPTS=%SL4J_ARGS%
)

if not "%MAVEN_ARGS%"=="" (
    set MAVEN_ARGS=%MAVEN_ARGS% -B
) else (
    set MAVEN_ARGS=-B
)

if not "%MAVEN_SETTINGS_FILE%"=="" (
    MAVEN_ARGS=%MAVEN_ARGS% -s %MAVEN_SETTINGS_FILE%
)
