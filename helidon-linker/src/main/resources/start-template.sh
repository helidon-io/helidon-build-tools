#!/bin/bash

usage() {
    echo
    echo "Start <JAR_NAME> from this runtime image."
    echo
    echo "Usage: ${scriptName} <options> [arg]..."
    echo
    echo "Options:"
    echo
    echo "    --debug           Add JVM debug options."
    echo "    --dry | --dryRun  Prints the command rather than executing it."
    echo "    --jvm <option>    Add one or more JVM options, replacing defaults."
    [[ ${hasCds} ]] && echo "    --noCds           Do not use CDS."
    echo
    echo "Unrecognized options are passed as args to <JAR_NAME>, replacing defaults."
    echo
    echo "Supported environment variables:"
    echo
    echo "    DEFAULT_JVM       <DEFAULT_JVM_DESC>"
    echo "    DEFAULT_ARGS      <DEFAULT_ARGS_DESC>"
    echo "    DEFAULT_DEBUG     <DEFAULT_DEBUG_DESC>"
    echo
    echo "Use --dry option to see defaults."
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
        cd ${homeDir}
        exec ${command}
    fi
}

init() {
    readonly scriptName=$(basename "${0}")
    readonly binDir=$(dirname "${0}")
    readonly homeDir=$(cd "${binDir}"/..; pwd)
    readonly jarName="<JAR_NAME>"
    readonly defaultDebug="<DEFAULT_DEBUG>"
    readonly defaultJvm="<DEFAULT_JVM>"
    readonly defaultArgs="<DEFAULT_ARGS>"
    readonly hasCds="<HAS_CDS>"
    readonly cdsOption="-XX:SharedArchiveFile=lib/start.jsa"
    readonly debugOptions="${DEFAULT_DEBUG:-${defaultDebug}}"
    readonly jvmDefaults="${DEFAULT_JVM:-${defaultJvm}}"
    readonly argDefaults="${DEFAULT_ARGS:-${defaultArgs}}"
    useCds=${hasCds}
    args= cds= debug= dryRun= jvm= 

    while (( ${#} > 0 )); do
        case "${1}" in
            -d | --debug) debug="${debugOptions} " ;;
            --dry | --dryRun) dryRun=true ;;
            -h | --help) usage ;;
            -j | --jvm) shift; appendVar jvm "${1}" ;;
            -n | --noCds) useCds= ;;
            *) appendVar args "${1}" ;;
        esac
        shift
    done

    [[ ${useCds} ]] && cds="${cdsOption} "
    readonly command="bin/java ${debug}${cds}${jvm:-${jvmDefaults}} -jar app/${jarName} ${args:-${argDefaults}}"
}

appendVar() {
    local var=${1}
    local value=${2}
    local sep=${3:- }
    export ${var}="${!var:+${!var}${sep}}${value}"
}

main "$@"
