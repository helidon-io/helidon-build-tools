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
#

baseurl="https://github.com/koalaman/shellcheck/releases/download"
readonly baseurl

statuscode=0
declare -a filepaths

version=0.9.0
readonly version

cacheDir="/home/runner/work/shellcheck/"
readonly cacheDir

shellcheck="${cacheDir}shellcheck"
readonly shellcheck

# Caching the shellcheck
# shellcheck disable=SC2046
if [ ! $(cd "${cacheDir}") ]; then
    mkdir "${cacheDir}"
    wget --no-check-certificate -P "${cacheDir}" "${baseurl}/v${version}/shellcheck-v${version}.linux.x86_64.tar.xz"

    tar -xf "${cacheDir}shellcheck-v0.9.0.linux.x86_64.tar.xz" -C "${cacheDir}"

    mv "${cacheDir}shellcheck-v${version}/shellcheck" ${shellcheck}
fi

echo "ShellCheck version"
${shellcheck} --version

while IFS= read -r -d '' file; do
  filepaths+=("$file")
done < <(find "$(pwd)" -name "*.sh" -print0)

for file in "${filepaths[@]}"; do
   ${shellcheck} "${file}" || statuscode=$?
done

echo "tested files :"
for file in "${filepaths[@]}"; do
   echo "${file}"
done

if [ "${statuscode}" -ne "0" ]; then
    exit 4
fi
