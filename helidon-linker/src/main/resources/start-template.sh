#!/bin/bash

usage() {
    echo
    echo "Start <JAR_NAME> from this runtime image."
    echo
    echo "Usage: ${scriptName} <options> [arg]..."
    echo
    echo "Options:"
    echo
    echo "    --jvm <option>    Add one or more JVM options, replacing defaults."
    echo "    --debug           Add JVM debug options."
    echo "    --cds             Use the CDS archive if present."
    echo "    --dry | --dryRun  Prints the command rather than executing it."
    echo
    echo "Unrecognized options are passed as args to <JAR_NAME>, replacing defaults."
    echo
    echo "Supported environment variables:"
    echo
    echo "    DEFAULT_JVM       <DEFAULT_JVM_DESC>"
    echo "    DEFAULT_ARGS      <DEFAULT_ARGS_DESC>"
    echo "    DEFAULT_DEBUG     <DEFAULT_DEBUG_DESC>"
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
    readonly scriptName=$(basename "${0}")
    readonly binDir=$(dirname "${0}")
    readonly homeDir=$(cd "${binDir}"/..; pwd)
    readonly java="${binDir}/java"
    readonly cdsArchive="${homeDir}/lib/start.jsa"
    readonly cdsOption="-XX:SharedArchiveFile=${cdsArchive}"
    readonly jarName="<JAR_NAME>"
    readonly defaultDebug="<DEFAULT_DEBUG>"
    readonly defaultJvm="<DEFAULT_JVM>"
    readonly defaultArgs="<DEFAULT_ARGS>"
    readonly jar="${homeDir}/app/${jarName}"
    readonly debugOptions="${DEFAULT_DEBUG:-${defaultDebug}}"
    readonly jvmDefaults="${DEFAULT_JVM:-${defaultJvm}}"
    readonly argDefaults="${DEFAULT_ARGS:-${defaultArgs}}"
    jvm= args= cds= debug= dryRun=

    while (( ${#} > 0 )); do
        case "${1}" in
            -h | --help) usage ;;
            -c | --cds) setCds ;;
            -d | --debug) debug="${debugOptions} " ;;
            -j | --jvm) shift; appendVar jvm "${1}" ;;
            --dry | --dryRun) dryRun=true ;;
            *) appendVar args "${1}" ;;
        esac
        shift
    done

    readonly command="${java} ${debug}${cds}${jvm:-${jvmDefaults}} -jar ${jar} ${args:-${argDefaults}}"
}

setCds() {
    [[ -e ${cdsArchive} ]] && cds="${cdsOption} " || echo "WARNING: CDS archive not found"
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
