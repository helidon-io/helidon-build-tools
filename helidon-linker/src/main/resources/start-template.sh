#!/bin/bash

usage() {
    echo
    echo "Start <MAIN_JAR_NAME> from this runtime image."
    echo
    echo "Usage: ${scriptName} <options> [arg]..."
    echo
    echo "Options:"
    echo "     --jvm <option>     Add a JVM option. Can be used multiple times, and/or quoted strings provided."
    echo "     --debug            Add JVM debug options."
    echo "     --cds              Use the CDS archive if present."
    echo "     --dry | --dryRun   Prints the command rather than executing it."
    echo
    echo "Any unrecognized option is <UNRECOGNIZED_DESC> to <MAIN_JAR_NAME>."
    echo
    echo "Supported environment variables:"
    echo
    echo "     JVM_OPTIONS    <JVM_OPTIONS_DESC>"
    echo "     MAIN_ARGS      <MAIN_ARGS_DESC>"
    echo "     DEBUG_OPTIONS  <DEBUG_OPTIONS_DESC>"
    echo
    exit 0
}

main() {
    init "$@"
    start
}

start() {
    if [[ ${dryRun} ]]; then
        echo ${command}
    else
        exec ${command}
    fi
}

init() {
    readonly mainJarName="<MAIN_JAR_NAME>"
    readonly defaultJvmOptions="<JVM_OPTIONS>"
    readonly defaultMainArgs="<MAIN_ARGS>"
    readonly defaultDebugOptions="<DEBUG_OPTIONS>"
    readonly scriptName=$(basename "${0}")
    readonly binDir=$(dirname "${0}")
    readonly homeDir=$(cd "${binDir}"/..; pwd)
    readonly javaCommand="${binDir}/java"
    readonly cdsArchive="${homeDir}/lib/start.jsa"
    readonly mainJar="${homeDir}/app/${mainJarName}"
    readonly debugOptions=${DEBUG_OPTIONS:-${defaultDebugOptions}}
    jvmOptions=${JVM_OPTIONS:-${defaultJvmOptions}}
    mainArgs=${MAIN_ARGS:-${defaultMainArgs}}
    dryRun=

    while (( ${#} > 0 )); do
        case "${1}" in
            -h | --help) usage ;;
            -c | --cds) setCds ;;
            -d | --debug) appendVar jvmOptions "${debugOptions}" ;;
            -j | --jvm) shift; appendVar jvmOptions "${1}" ;;
            --dry | --dryRun) dryRun=true ;;
            *) appendVar mainArgs "${1}" ;;
        esac
        shift
    done

    readonly command="${javaCommand} ${jvmOptions} -jar ${mainJar} ${mainArgs}"
}

setCds() {
    if [[ -e ${cdsArchive} ]]; then
        appendVar jvmOptions "-XX:SharedArchiveFile=${cdsArchive}"
    else
        echo "WARNING: CDS archive not found"
    fi
}

appendVar() {
    local var=${1}
    local value=${2}
    local sep=${3:- }
    export ${var}="${!var:+${!var}${sep}}${value}"
}

fail() {
    echo "${1}"
    exit 1
}

main "$@"
