/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.maven.stager;

import java.util.Map;

import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

/**
 * Archive entry path mapper.
 */
record Mapper(String match, String replace) {

    Mapper(XMLElement element) {
        this(Strings.requireValid(element.attribute("match", null), "match is required"),
                Strings.requireValid(element.attribute("replace", null), "replace is required"));
    }

    String match(Map<String, String> vars) {
        return StagingTask.resolveVar(match, vars);
    }

    String replace(Map<String, String> vars) {
        return StagingTask.resolveVar(replace, vars);
    }
}
