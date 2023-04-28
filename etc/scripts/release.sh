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

# shellcheck disable=SC1091
source "${WS_DIR}"/etc/scripts/pipeline-env.sh

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

major_minor_micro() {
  # shellcheck disable=SC2001
  echo "${1}" | sed 's/\([0-9]\{1,\}\)\.\([0-9]\{1,\}\)\.\([0-9]\{1,\}\)\(.*\)/\1.\2.\3/g'
}

osgi_mvn_version(){
  # shellcheck disable=SC2001
  local major_minor_micro
  major_minor_micro=$(major_minor_micro "${1}")
  if [[ "${1}" =~ -SNAPSHOT$ ]] ; then echo "${major_minor_micro}-SNAPSHOT" ; else echo "${major_minor_micro}" ; fi
}

osgi_bundle_version() {
  # shellcheck disable=SC2001
  local major_minor_micro
  major_minor_micro=$(major_minor_micro "${1}")
  if [[ "${1}" =~ -SNAPSHOT$ ]] ; then echo "${major_minor_micro}.qualifier" ; else echo "${major_minor_micro}" ; fi
}

update_version(){
    # Update version
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} -f "${WS_DIR}"/pom.xml versions:set versions:set-property \
        -DgenerateBackupPoms="false" \
        -DnewVersion="${FULL_VERSION}" \
        -Dproperty="helidon.version" \
        -DprocessFromLocalAggregationRoot="false" \
        -DupdateMatchingVersions="false"

    local osgi_mvn_v
    osgi_mvn_v="$(osgi_mvn_version "${MVN_VERSION}")"
    local new_osgi_mvn_v
    new_osgi_mvn_v="$(osgi_mvn_version "${FULL_VERSION}")"

    # shellcheck disable=SC2044
    for pom in $(find ide-support -name "pom.xml") ; do
      # shellcheck disable=SC2002
      # shellcheck disable=SC2140
      cat "${pom}" | sed s@"<version>${osgi_mvn_v}</version>"@"<version>${new_osgi_mvn_v}</version>"@g > "${pom}.tmp"
      mv "${pom}".tmp "${pom}"
    done

    local osgi_bundle_v
    osgi_bundle_v="$(osgi_bundle_version "${MVN_VERSION}")"
    local new_osgi_bundle_v
    new_osgi_bundle_v="$(osgi_bundle_version "${FULL_VERSION}")"

    # shellcheck disable=SC2044
    for manifest in $(find ide-support/lsp -name "MANIFEST.MF") ; do
      # shellcheck disable=SC2002
      # shellcheck disable=SC2140
      cat "${manifest}" | sed s@"Bundle-Version: ${osgi_bundle_v}"@"Bundle-Version: ${new_osgi_bundle_v}"@g > "${manifest}.tmp"
      mv "${manifest}".tmp "${manifest}"
    done
}

release_build(){
    # Do the release work in a branch
    local GIT_BRANCH
    GIT_BRANCH="release/${FULL_VERSION}"
    git branch -D "${GIT_BRANCH}" > /dev/null 2>&1 || true
    git checkout -b "${GIT_BRANCH}"

    # Invoke update_version
    update_version

    # Git user info
    git config user.email || git config --global user.email "info@helidon.io"
    git config user.name || git config --global user.name "Helidon Robot"

    # Commit version changes
    git commit -a -m "Release ${FULL_VERSION} [ci skip]"

    # Perform local deployment
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} clean deploy \
        -Prelease,ide-support \
        -DskipTests \
        -DskipRemoteStaging=true

    # Upload all artifacts to nexus
    # shellcheck disable=SC2086
    mvn ${MAVEN_ARGS} -N nexus-staging:deploy-staged \
        -DstagingDescription="Helidon Build Tools v${FULL_VERSION}"

    # Create and push a git tag
    local GIT_REMOTE
    GIT_REMOTE=$(git config --get remote.origin.url | \
        sed "s,https://\([^/]*\)/,git@\1:,")

    git remote add release "${GIT_REMOTE}" > /dev/null 2>&1 || \
    git remote set-url release "${GIT_REMOTE}"

    git tag -f "${FULL_VERSION}"
    git push --force release refs/tags/"${FULL_VERSION}":refs/tags/"${FULL_VERSION}"
}

# Invoke command
${COMMAND}
