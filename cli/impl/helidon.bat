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

:main
    call :init
    %action% %command%
EXIT /B 0

:init
    set projectDir=%~dp0
    set targetDir="%projectDir%\target"
    set jarFile="%targetDir%\helidon.jar"
    set attach="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
    set attachMvn="-Dmvn.debug.port=5006"
    set attachMvnChild="-Dmvn.child.debug.port=5007"
    set attachPlugin="-Dplugin.debug.port=5006"
    set action="exec"

    for %%x in (%*) do (
        if %%x==--attach (
            call :appendVar jvm %attach%
            goto END_LOOP
        )
        if %%x==--attachMvn (
            call :appendVar jvm %attachMvn%
            goto END_LOOP
        )
        if %%x==--attachMvnChild (
            call :appendVar jvm %attachMvnChild%
            goto END_LOOP
        )
        if %%x==--attachPlugin (
            call :appendVar jvm %attachPlugin%
            goto END_LOOP
        )
        if %%x==--dryRun (
            set action=echo
            goto END_LOOP
        )
        call :appendVar args %%x
        :END_LOOP
    )

    set command="java %jvm% -jar %jarFile% %args%"
EXIT /B 0

:appendVar
    set %~1="%~1 %~2"
EXIT /B 0

