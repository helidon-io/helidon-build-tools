@REM
@REM Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
call %~dp0\pipeline-env.bat

cd %~dp0\..\..

if "%~1"=="/release" GOTO Release

mvn %MAVEN_ARGS% ^
    -f cli/impl/pom.xml ^
    clean install ^
    -DskipTests ^
    -Pnative-image ^
GOTO :eof

:Release
for /f "delims=*" %%a in ('mvn %MAVEN_ARGS% -q -N org.codehaus.mojo:exec-maven-plugin:1.3.1:exec -Dexec.executable^="cmd" -Dexec.args^="/c echo ${project.version}"') do set MVN_VERSION=%%a
set VERSION=%MVN_VERSION:-SNAPSHOT=%
git fetch origin refs/tags/%VERSION%:refs/tags/%VERSION%
git checkout refs/tags/%VERSION% && (
  mvn %MAVEN_ARGS% ^
      -f cli/impl/pom.xml ^
      clean install ^
      -DskipTests ^
      -Pnative-image ^
      -Possrh-staging
)
