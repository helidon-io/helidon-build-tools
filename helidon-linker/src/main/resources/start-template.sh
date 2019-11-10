#!/bin/bash

usage() {
    echo
    echo "Start ${mainJarName} from this runtime image."
    echo
    echo "Usage: ${scriptName} <options> [mainArg]..."
    echo
    echo "Options:"
    echo "     -j | --jvm <option>     Add a JVM option. Can be used multiple times, and/or quoted strings provided."
    echo "     -d | --debug            Add JVM debug options. Uses JAVA_DEBUG env var if present, or a default if not."
    echo "     -c | --cds              Use the CDS archive if present."
    echo
    exit 0
}

main() {
    init "$@"
    start
}

start() {
    exec ${command}
}

init() {
    readonly mainJarName="<MAIN_JAR_NAME>"
    readonly scriptName=$(basename "${0}")
    readonly binDir=$(dirname "${0}")
    readonly homeDir=$(cd "${binDir}"/..; pwd)
    readonly javaCommand="${binDir}/java"
    readonly cdsArchive="${homeDir}/lib/start.jsa"
    readonly mainJar="${homeDir}/app/${mainJarName}"
    jvmOptions=
    mainArgs=

    while (( ${#} > 0 )); do
        case "${1}" in
            -h | --help) usage ;;
            -c | --cds) setCds ;;
            -d | --debug) setDebug ;;
            -j | --jvm) shift; appendVar jvmOptions "${1}" ;;
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

setDebug() {
    if [[ ${JAVA_DEBUG} ]]; then
        appendVar jvmOptions "${JAVA_DEBUG}"
    else
        appendVar jvmOptions "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
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
