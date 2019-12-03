#!/bin/bash

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
    echo "    DEFAULT_JVM     <DEFAULT_JVM_DESC>"
    echo "    DEFAULT_ARGS    <DEFAULT_ARGS_DESC>"
    echo "    DEFAULT_DEBUG   <DEFAULT_DEBUG_DESC>"
    echo
    exit 0
}

main() {
    local action command
    init "$@"
    ${action} ${command}
}

init() {
    local -r scriptName=$(basename "${0}")
    local -r binDir=$(dirname "${0}")
    local -r jarName="<JAR_NAME>"
    local -r defaultDebug="<DEFAULT_DEBUG>"
    local -r defaultJvm="<DEFAULT_JVM>"
    local -r defaultArgs="<DEFAULT_ARGS>"
    local -r cdsOption="<CDS_UNLOCK>-XX:SharedArchiveFile=lib/start.jsa -Xshare:"
    local -r exitOption="-Dexit.on.started=✅"
    local -r jvmDefaults="${DEFAULT_JVM:-${defaultJvm}}"
    local -r argDefaults="${DEFAULT_ARGS:-${defaultArgs}}"
    local args jvm test share=auto 
    local useCds=true
    local debug
    action=exec
 
    while (( ${#} > 0 )); do
        case "${1}" in
            --jvm) shift; appendVar jvm "${1}" ;;
            --noCds) useCds= ;;
            --debug) debug=true ;;
            --test) test=true; share=on ;;
            --dryRun) action=echo ;;
            -h | --help) usage ;;
            *) appendVar args "${1}" ;;
        esac
        shift
    done

    local jvmOptions=${jvm:-${jvmDefaults}}
    [[ ${useCds} ]] && appendVar jvmOptions "${cdsOption}${share}"
    [[ ${debug} ]] && appendVar jvmOptions "${DEFAULT_DEBUG:-${defaultDebug}}"
    [[ ${test} ]] && appendVar jvmOptions "${exitOption}"
    command="bin/java ${jvmOptions} -jar app/${jarName} ${args:-${argDefaults}}"
    cd "${binDir}/.."
}

appendVar() {
    export ${1}="${!1:+${!1} }${2}"
}

main "$@"
