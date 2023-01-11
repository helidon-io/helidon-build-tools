#!/bin/bash
#
# Copyright (c) 2020, 2023 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

main() {
    local action command
    findRootDirectory
    findJavaExecutable
    buildCommand "$@"
    ${action} ${command}
}

#Find archive directory and use it as based directory
findRootDirectory() {
    WDIR="$0"
    while [ -h "$WDIR" ]; do
        ls=`ls -ld "$WDIR"`
        link=`expr "$ls" : '.*-> \(.*\)$'`
        if expr "$link" : '/.*' > /dev/null; then
            WDIR="$link"
        else
            WDIR=`dirname "$WDIR"`/"$link"
        fi
    done
    DIR=`dirname "$WDIR"`
    BASEDIR=`cd "$DIR/.." >/dev/null; pwd`
}

#Look for JAVA_HOME java binary, then `which java` result
findJavaExecutable() {
    if [ -z "$JAVA_HOME" ]; then
        javaExecutable=`which java`
        echo "JAVA_HOME is not set, using ${javaExecutable} instead"
        return
    fi
    if [ "$(uname)" == "CYGWIN" ]; then
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
        JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
        BASEDIR=`cygpath --path --windows "$BASEDIR"`
    fi
    javaExecutable="${JAVA_HOME}/bin/java"
    echo "Using java binary: ${javaExecutable}"
}

#Build command line
buildCommand() {
    local -r jarFile="${BASEDIR}/lib/helidon.jar"
    local -r attach="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
    local -r attachMvn="-Dmvn.debug.port=5006"
    local -r attachMvnChild="-Dmvn.child.debug.port=5007"
    local -r attachPlugin="-Dplugin.debug.port=5006"
    local jvm
    local args
    action="exec"

    while (( ${#} > 0 )); do
        case "${1}" in
            --attach) appendVar jvm "${attach}" ;;
            --attachMvn) appendVar args "${attachMvn}" ;;
            --attachMvnChild) appendVar args "${attachMvnChild}" ;;
            --attachPlugin) appendVar args "${attachPlugin}" ;;
            --dryRun) action="echo" ;;
            *) appendVar args "${1}" ;;
        esac
        shift
    done

    command="${javaExecutable} ${jvm} -jar ${jarFile} ${args}"
}

#Append variable value
appendVar() {
    export ${1}="${!1:+${!1} }${2}"
}
main "$@"

