#!/bin/bash -e
#
# Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

usage(){
    cat <<EOF

DESCRIPTION: Helidon Release Script

USAGE:

$(basename "${0}") [ --build-number=N ] CMD

  --version=V
        Override the version to use.
        This trumps --build-number=N

  --help
        Prints the usage and exits.

  CMD:

    update_version
        Update the version in the workspace

    release_build
        Perform a release build
        This will create a local branch, deploy artifacts and push a tag

EOF
}

# parse command line args
ARGS=( "${@}" )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--version="*)
        VERSION=${ARG#*=}
        ;;
    "--help")
        usage
        exit 0
        ;;
    *)
        if [ "${ARG}" = "update_version" ] || [ "${ARG}" = "release_build" ] ; then
            readonly COMMAND="${ARG}"
        else
            echo "ERROR: unknown argument: ${ARG}"
            exit 1
        fi
        ;;
    esac
}

if [ -z "${COMMAND}" ] ; then
  echo "ERROR: no command provided"
  exit 1
fi

# Path to this script
if [ -h "${0}" ] ; then
    SCRIPT_PATH="$(readlink "${0}")"
else
    SCRIPT_PATH="${0}"
fi
readonly SCRIPT_PATH

# Path to the root of the workspace
# shellcheck disable=SC2046
WS_DIR=$(cd $(dirname -- "${SCRIPT_PATH}") ; cd ../.. ; pwd -P)
readonly WS_DIR

# get current maven version
# shellcheck disable=SC2086
MVN_VERSION=$(mvn ${MAVEN_ARGS} \
    -q \
    -f "${WS_DIR}"/pom.xml \
    -Dexec.executable="echo" \
    -Dexec.args="\${project.version}" \
    --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
readonly MVN_VERSION

# Resolve FULL_VERSION
if [ -z "${VERSION+x}" ]; then
    # strip qualifier
    readonly VERSION="${MVN_VERSION%-*}"
    readonly FULL_VERSION="${VERSION}"
else
    readonly FULL_VERSION="${VERSION}"
fi

export FULL_VERSION
printf "\n%s: FULL_VERSION=%s\n\n" "$(basename "${0}")" "${FULL_VERSION}"

update_version(){
    # Update version
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} -f "${WS_DIR}"/pom.xml versions:set versions:set-property \
        -DgenerateBackupPoms="false" \
        -DnewVersion="${FULL_VERSION}" \
        -Dproperty="helidon.version" \
        -DprocessFromLocalAggregationRoot="false" \
        -DupdateMatchingVersions="false"
}

release_build(){
    # Do the release work in a branch
    local git_branch tmpfile
    git_branch="release/${FULL_VERSION}"
    git branch -D "${git_branch}" > /dev/null 2>&1 || true
    git checkout -b "${git_branch}"

    # Invoke update_version
    update_version

    # Git user info
    git config user.email || git config --global user.email "info@helidon.io"
    git config user.name || git config --global user.name "Helidon Robot"

    # Commit version changes
    git commit -a -m "Release ${FULL_VERSION} [ci skip]"

    # Bootstrap credentials from environment
    if [ -n "${MAVEN_SETTINGS}" ] ; then
        tmpfile=$(mktemp XXXXXXsettings.xml)
        echo "${MAVEN_SETTINGS}" > "${tmpfile}"
        MAVEN_ARGS="${MAVEN_ARGS} -s ${tmpfile}"
    fi
    if [ -n "${GPG_PRIVATE_KEY}" ] ; then
        tmpfile=$(mktemp XXXXXX.key)
        echo "${GPG_PRIVATE_KEY}" > "${tmpfile}"
        gpg --allow-secret-key-import --import --no-tty --batch "${tmpfile}"
        rm "${tmpfile}"
    fi
    if [ -n "${GPG_PASSPHRASE}" ] ; then
        echo "allow-preset-passphrase" >> ~/.gnupg/gpg-agent.conf
        gpg-connect-agent reloadagent /bye
        GPG_KEYGRIP=$(gpg --with-keygrip -K | grep "Keygrip" | head -1 | awk '{print $3}')
        /usr/lib/gnupg/gpg-preset-passphrase --preset "${GPG_KEYGRIP}" <<< "${GPG_PASSPHRASE}"
    fi

    # Perform local deployment
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} clean deploy \
        -Prelease \
        -DskipTests \
        -DskipRemoteStaging=true

    # Upload all artifacts to nexus
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} -N nexus-staging:deploy-staged \
        -DstagingDescription="Helidon Build Tools v${FULL_VERSION}"

    # Create and push a git tag
    git tag -f "${FULL_VERSION}"
    git push --force origin refs/tags/"${FULL_VERSION}":refs/tags/"${FULL_VERSION}"
}

# Invoke command
${COMMAND}
