@REM
@REM Copyright (c) 2022 Oracle and/or its affiliates.
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
set jarFile=%targetDir%\helidon.jar
set attach="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
set attachMvn="-Dmvn.debug.port=5006"
set attachMvnChild="-Dmvn.child.debug.port=5007"
set attachPlugin="-Dplugin.debug.port=5006"
set action=
set args=

for %%x in (%*) do (

    set add=yes

    if %%~x==--attach (
        call :appendVar jvm %attach%
        set add=no
    ) else if %%~x==--attachMvn (
        call :appendVar jvm %attachMvn%
        set add=no
    ) else if %%~x==--attachMvnChild (
        call :appendVar jvm %attachMvnChild%
        set add=no
    ) else if %%~x==--attachPlugin (
        call :appendVar jvm %attachPlugin%
        set add=no
    ) else if %%~x==--dryRun (
        set action=echo
        set add=no
    ) else if %add%==yes (
        call :appendVar %%~x
    )
)

%action% java %jvm% -jar %jarFile% %args%

call :clear

:appendVar
    set args=%args% %~1
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
EXIT /B 0
