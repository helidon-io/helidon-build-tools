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

class ValueHandler implements Handler {

    private static YamlParser parser;
    private static ValueHandler instance;

    private ValueHandler(YamlParser yamlParser) {
        parser = yamlParser;
    }

    public static ValueHandler INSTANCE(YamlParser yamlParser) {
        if (instance != null && parser != null && parser.equals(yamlParser)) {
            return instance;
        }
        instance = new ValueHandler(yamlParser);
        return instance;
    }

    @Override
    public LineResult process(int lineIndex, String line) {
        return null;
    }
}