/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.service.config.yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parser for the files in YAML format.
 */
public class YamlParser {

    private Handler currentHandler;

    public Map<String, LineResult> parse(List<String> yaml) {
        Map<String, LineResult> result = new LinkedHashMap<>();
        currentHandler = InitialHandler.INSTANCE(this);
        for (String line : yaml) {
            LineResult lineResult = currentHandler.process(line);
            if (lineResult != null) {
                result.put(getPath(lineResult, result), lineResult);
            }
        }
        return result;
    }

    public void currentHandler(Handler currentHandler) {
        this.currentHandler = currentHandler;
    }

    private String getPath(LineResult lineResult, Map<String, LineResult> previousResults) {
        StringBuilder path = new StringBuilder();
        Map<Integer, String> parents = new LinkedHashMap<>();
        previousResults.values().forEach(prev->parents.put(prev.indent(), Objects.requireNonNull(prev.tokens().peek()).value()));
        parents.entrySet().removeIf(e -> e.getKey() >= lineResult.indent());//entry.getValue().indent()
        parents.values().forEach(parent->path.append(parent).append("."));
        return path.append(Objects.requireNonNull(lineResult.tokens().peek()).value()).toString();
    }
}
