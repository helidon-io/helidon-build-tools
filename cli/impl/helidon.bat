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

set projectDir=%~dp0
set targetDir=%projectDir%target
set jarFile=%targetDir%\helidon-cli.jar
set attach="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
set attachMvn="-Dmvn.debug.port=5006"
set attachMvnChild="-Dmvn.child.debug.port=5007"
set attachPlugin="-Dplugin.debug.port=5006"
set action=
set args=
set /a nbargs=0
set /a loopcount=0

for %%x in (%*) do (
    set /a nbargs=nbargs+1
)

:start

set add=yes

if %loopcount%==%nbargs% (
    goto exitloop
)

if %1==--attach (
    call :appendJvm %attach%
    set add=no
)

if %1==--attachMvn (
    call :appendJvm %attachMvn%
    set add=no
)

if %1==--attachMvnChild (
    call :appendJvm %attachMvnChild%
    set add=no
)

if %1==--attachPlugin (
    call :appendJvm %attachPlugin%
    set add=no
)

if %1==--dryRun (
    set action=echo
    set add=no
)

if %add%==yes (
    call :appendArgs %1
)
shift

set /a loopcount=loopcount+1

goto start
:exitloop

%action% java %jvm% -jar %jarFile% %args%

call :clear

goto :eof

:appendArgs
    set args=%args% %~1
EXIT /B 0

:appendJvm
    set jvm=%jvm% %~1
EXIT /B 0

:clear
    set projectDir=
    set targetDir=
    set jarFile=
    set attach=
    set attachMvn=
    set attachMvnChild=
    set attachPlugin=
    set action=
    set args=
    set jvm=
EXIT /B 0
