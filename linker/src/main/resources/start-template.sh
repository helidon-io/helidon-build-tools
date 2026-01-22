#!/bin/bash
#
# Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

usage() {
    echo
    echo "Start <JAR_NAME> from this runtime image."
    echo
    echo "Usage: ${scriptName} <options> [arg]..."
    echo
    echo "Options:"
    echo
    echo "    --jvm <option>  Add one or more JVM options, replacing defaults."
    echo "    --noCds         Do not use CDS."
    echo "    --debug         Add JVM debug options."
    echo "    --test          Exit when started."
    echo "    --dryRun        Display the command rather than executing it."
    echo "    --help          Display usage."
    echo
    echo "Unrecognized options are passed as arguments to <JAR_NAME>, replacing defaults."
    echo
    echo "Supported environment variables:"
    echo
    echo "    DEFAULT_APP_JVM     <DEFAULT_APP_JVM_DESC>"
    echo "    DEFAULT_APP_ARGS    <DEFAULT_APP_ARGS_DESC>"
    echo "    DEFAULT_APP_DEBUG   <DEFAULT_APP_DEBUG_DESC>"
    echo
    exit 0
}

main() {
    local action command
    init "$@"
    # shellcheck disable=SC2086,SC2164
    ${action} ${command}
}

init() {
    local -r scriptName=$(basename "${0}")
    local -r binDir=$(dirname "${0}")
    # shellcheck disable=SC2164,SC2086
    local -r homeDir=$(cd "${binDir}/.."; pwd)
    local -r jarName="<JAR_NAME>"
    local -r defaultDebug="<DEFAULT_APP_DEBUG>"
    local -r defaultJvm="<DEFAULT_APP_JVM>"
    local -r defaultArgs="<DEFAULT_APP_ARGS>"
    local -r cdsOption="<CDS_UNLOCK>-XX:SharedArchiveFile=${homeDir}/lib/start.jsa -Xshare:"
    local -r aotOption="-XX:AOTCache=${homeDir}/lib/start.aot"
    local -r exitOption="-Dexit.on.started=<EXIT_ON_STARTED>"
    local -r jvmDefaults="${DEFAULT_APP_JVM:-${defaultJvm}}"
    local -r argDefaults="${DEFAULT_APP_ARGS:-${defaultArgs}}"
    local -r useAot=<USE_AOT>
    local pathPrefix="${homeDir}/"
    local args jvm test share=auto
    local useCds=true
    local debug
    action="exec"

    while (( ${#} > 0 )); do
        case "${1}" in
            --jvm) shift; appendVar jvm "${1}" ;;
            --noCds) useCds= ;;
            --debug) debug=true ;;
            --test) test=true; share=on ;;
            --dryRun) action="echo" ;;
            -h | --help) usage ;;
            *) appendVar args "${1}" ;;
        esac
        shift
    done

    local jvmOptions=${jvm:-${jvmDefaults}}
    [[ ${useCds} ]] && setupCds
    [[ ${debug} ]] && appendVar jvmOptions "${DEFAULT_APP_DEBUG:-${defaultDebug}}"
    if [[ ${test} ]]; then
        appendVar jvmOptions "${exitOption}"
        [[ ${useCds} ]] && checkTimeStamps
    fi
    command="${pathPrefix}bin/java ${jvmOptions} -jar ${pathPrefix}app/${jarName} ${args:-${argDefaults}}"
}

appendVar() {
  # shellcheck disable=SC2140,SC2086
    export ${1}="${!1:+${!1} }${2}"
}

setupCds() {
    if [[ ${useAot} ]]; then
        appendVar jvmOptions "${aotOption}"
    else
        appendVar jvmOptions "${cdsOption}${share}"
    fi
    pathPrefix=
    # shellcheck disable=SC2164
    cd "${homeDir}"
}

checkTimeStamps() {
    local -r timeStampFormat="<STAT_FORMAT>"
    local -r modulesTimeStamp=$(stat ${timeStampFormat} "${homeDir}/lib/modules")
    local -r jarTimeStamp=$(stat ${timeStampFormat} "${homeDir}/app/${jarName}")
    if [[ ${modulesTimeStamp} != "<MODULES_TIME_STAMP>" || ${jarTimeStamp} != "<JAR_TIME_STAMP>" ]]; then
        echo "WARNING: CDS will likely fail since it appears this image is a copy (timestamps differ)."
        echo "         <COPY_INSTRUCTIONS>"
    fi
}

main "$@"
