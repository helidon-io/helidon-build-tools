#!/bin/bash
#
# Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

set -o pipefail || true  # trace ERR through pipes
set -o errtrace || true # trace ERR through commands and functions
set -o errexit || true  # exit the script if any statement returns a non-true return value

on_error(){
    CODE="${?}" && \
    set +x && \
    printf "[ERROR] Error(code=%s) occurred at %s:%s command: %s\n" \
        "${CODE}" "${BASH_SOURCE[0]}" "${LINENO}" "${BASH_COMMAND}"
}
trap on_error ERR

# Path to this script
if [ -h "${0}" ] ; then
    readonly SCRIPT_PATH="$(readlink "${0}")"
else
    readonly SCRIPT_PATH="${0}"
fi

# Path to the root of the workspace
# shellcheck disable=SC2046
readonly WS_DIR=$(cd $(dirname -- "${SCRIPT_PATH}") ; cd ../.. ; pwd -P)

source "${WS_DIR}"/etc/scripts/pipeline-env.sh
export PATH=/tools/graalvm-ce-java17-21.3.0/bin:${PATH}

if [ "${1}" = "--release" ] ; then
    # get maven version
    # shellcheck disable=SC2086
    MVN_VERSION=$(mvn ${MAVEN_ARGS} \
        -q \
        -f "${WS_DIR}"/pom.xml \
        -Dexec.executable="echo" \
        -Dexec.args="\${project.version}" \
        --non-recursive \
        org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)

    # strip qualifier
    readonly VERSION="${MVN_VERSION%-*}"
    git fetch origin refs/tags/"${VERSION}":refs/tags/"${VERSION}"
    git checkout refs/tags/"${VERSION}"
fi

mvn "${MAVEN_ARGS}" -f "${WS_DIR}"/helidon-cli/impl/pom.xml \
    clean install \
    -DskipTests \
    -Pnative-image \
    -Possrh-staging
