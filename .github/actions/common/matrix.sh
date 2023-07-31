#!/bin/bash
#
# Copyright (c) 2023 Oracle and/or its affiliates.
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

shopt -s globstar
shopt -s extglob

ERROR_FILE=$(mktemp -t XXXmatrix)
readonly ERROR_FILE

#
# Merge two JSON arrays.
#
# arg1: first array
# arg2: second array
#
json_merge_arrays() {
  jq '.[]' <<< "$(printf "%s\n%s" "${1}" "${2}")" | jq -s
}

#
# Merge two JSON objects.
#
# arg1: first object
# arg2: second object
#
json_merge_objects() {
  jq -s '.[0] * .[1]' <<< "$(printf "%s\n%s" "${1}" "${2}")"
}

#
# Get the value for a key in a JSON object.
#
# arg1: key
# arg2: JSON object
#
json_map_get() {
  jq -r --arg a "${1}" 'to_entries[] | select (.key == $a).value[]' <<< "${2}"
}

#
# Get the keys in a JSON object.
#
# arg1: JSON object
#
json_keys() {
  jq -r 'keys | .[]' <<< "${1}"
}

#
# Expand the given glob expressions to match directories with pom.xml files.
# Exclude directories that are nested under 'src'
#
# args: prefix expr...
#
list_modules() {
  local prefix files
  prefix="${1}"
  shift
  files=()
  printf "## Resolving module expressions: %s\n" "${*}" >&2
  for exp in "${@}" ; do
    printf "## Resolving module expression: %s\n" "${exp}" >&2
    for i in ${exp}/pom.xml ; do
      if [ -f "${i}" ] && [[ ! "${i}" =~ "src/" ]] ; then
        files+=("${prefix}${i%%/pom.xml}")
      fi
    done
  done
  if [ ${#files[*]} -eq 0 ] ; then
    printf "## ERROR: Unresolved expressions: %s\n" "${*}" >&2
    echo "${@}" >> "${ERROR_FILE}"
    return 1
  fi
  IFS=","
  printf "## Resolved expressions: %s, modules: %s\n" "${*}" "${files[*]}" >&2
  echo "${files[*]}"
}

#
# Print a JSON object for a group
#
# args: group prefix expr...
#
resolve_group() {
  local group
  group="${1}"
  shift
  echo -ne '
  {
      "group": "'"${group}"'",
      "modules": "'"$(list_modules "${@}")"'"
  }'
}

#
# Print JSON objects for the groups.
# Always add a 'misc' at the end that matches everything else
#
# arg1: JSON object E.g. '{ "group1": [ "dir1/**", "dir2/**" ], "group2": [ "dir3/**" ] }'
#
resolve_groups() {
  local groups modules all_modules
  all_modules=()
  groups="$(jq '.groups // []' <<< "${1}")"
  for group in $(json_keys "${groups}") ; do
    readarray -t modules <<< "$(json_map_get "${group}" "${groups}")"
    printf "## Resolving group: %s, expressions: %s\n" "${group}" "${modules[*]}" >&2
    resolve_group "${group}" "" "${modules[@]}"
    all_modules+=("${modules[@]}")
  done
  if [ ${#all_modules[@]} -gt 0 ] ; then
      printf "## Resolving group: misc, expressions: %s\n" "${all_modules[2]}" >&2
      resolve_group "misc" "!" "${all_modules[@]}"
  fi
}

#
# Generate the 'matrix' output
#
# arg1: JSON object E.g. '{ "group1": [ "dir1/**", "dir2/**" ], "group2": [ "dir3/**" ] }'
#
main() {
  local groups resolved_include extra_include merged_include resolved_matrix matrix errors

  printf "## Processing JSON: \n%s\n" "$(jq <<< "${1}")" >&2
  resolved_include="$(resolve_groups "${1}" | jq -s)"

  readarray -t errors < "${ERROR_FILE}"
  if [ ${#errors[*]} -ne 0 ] ; then
    printf "## ERROR: Unresolved expressions: %s\n" "${errors[*]}" >&2
    exit 1
  fi

  printf "## Resolved include JSON: \n%s\n" "$(jq <<< "${resolved_include}")" >&2

  extra_include="$(jq '.include // []' <<< "${1}")"
  extra_matrix="$(jq 'del(.groups, .group, .include)' <<< "${1}")"
  printf "## Additional include JSON: \n%s\n" "${extra_include}" >&2
  printf "## Additional matrix JSON: \n%s\n" "${extra_matrix}" >&2

  groups="$(jq '.group // []' <<< "${1}")"
  if [ "${groups}" != "[]" ] ; then
    merged_groups="$(json_merge_arrays "${groups}" '[ "misc" ]')"
  else
    merged_groups="[]"
  fi

  merged_include="$(json_merge_arrays "${resolved_include}" "${extra_include}")"
  resolved_matrix="$(jq <<< '{
    "group": '"${merged_groups}"',
    "include": '"${merged_include}"'
  }')"
  matrix="$(json_merge_objects "${resolved_matrix}" "${extra_matrix}")"
  printf "## Final matrix JSON: \n%s\n" "${matrix}" >&2

  echo "matrix=$(jq -c <<< "${matrix}")"
}

if [ ${#@} -lt 0 ] ; then
    error "Usage $(basename "${0}") JSON" >&2
    exit 1
fi

main "${1}"
