/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.FileUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class YamlParserTest {

    @Test
    void testIncorrectYaml() throws IOException {
        YamlParser yamlParser = new YamlParser();
        List<String> strings = readFile("incorrect-yaml-file.yaml");
        Map<LineResult, String> result = yamlParser.parse(strings);

        assertThat(getKey("firstName", result).indent(), is(0));
        assertThat(getKey("firstName", result).line(), is(15));
        assertThat(getKey("firstName", result).tokens().peek().value(), is("firstName"));

        assertThat(getKey("lastName.age", result).indent(), is(2));
        assertThat(getKey("lastName.age", result).tokens().peek().value(), is("age"));
        assertThat(getKey("lastName.age", result).line(), is(18));

        assertThat(getKey("lastName.address.street", result).indent(), is(4));
        assertThat(getKey("lastName.address.street", result).tokens().peek().value(), is("street"));
        assertThat(getKey("lastName.address.street", result).line(), is(20));
    }

    @Test
    void testCorrectYaml() throws IOException {
        YamlParser yamlParser = new YamlParser();
        List<String> strings = readFile("correct-yaml-file.yaml");
        Map<LineResult, String> result = yamlParser.parse(strings);

        assertThat(result.size(), is(53));

        assertThat(getKey("security", result).indent(), is(0));
        assertThat(getKey("security", result).line(), is(18));
        assertThat(getKey("security", result).tokens().peek().type(), is(Token.Type.KEY));
        assertThat(getKey("security", result).tokens().peek().value(), is("security"));

        assertThat(getKey("security.provider-policy.type", result).indent(), is(2));
        assertThat(getKey("security.provider-policy.type", result).line(), is(24));
        assertThat(getKey("security.provider-policy.type", result).tokens().peek().type(), is(Token.Type.KEY));
        assertThat(getKey("security.provider-policy.type", result).tokens().peek().value(), is("provider-policy.type"));

        assertThat(getKey("security.secrets.provider", result).indent(), is(4));
        assertThat(getKey("security.secrets.provider", result).line(), is(103));
        assertThat(getKey("security.secrets.provider", result).tokens().peek().type(), is(Token.Type.KEY));
        assertThat(getKey("security.secrets.provider", result).tokens().peek().value(), is("provider"));

        assertThat(getKey("server.sockets.tls.session-timeout-seconds", result).indent(), is(6));
        assertThat(getKey("server.sockets.tls.session-timeout-seconds", result).line(), is(189));
        assertThat(getKey("server.sockets.tls.session-timeout-seconds", result).tokens().peek().type(), is(Token.Type.KEY));
        assertThat(getKey("server.sockets.tls.session-timeout-seconds", result).tokens().peek()
                            .value(), is("session-timeout-seconds"));
    }

    private List<String> readFile(String path) throws IOException {
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        try (Stream<String> lines = Files.lines(FileUtils.pathOf(resource))) {
            return lines.collect(Collectors.toList());
        }
    }

    private LineResult getKey(String value, Map<LineResult, String> map) {
        return map.entrySet().stream()
                  .filter(entry -> entry.getValue().equals(value))
                  .map(Map.Entry::getKey)
                  .findFirst().orElse(null);
    }
}