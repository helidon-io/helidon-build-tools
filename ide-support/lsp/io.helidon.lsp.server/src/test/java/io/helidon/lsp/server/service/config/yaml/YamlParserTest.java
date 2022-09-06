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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Disabled
//TODO change it for the new implementation or remove it
class YamlParserTest {

    @Test
    public void testIncorrectYaml() throws IOException {
        YamlParser yamlParser = new YamlParser();
        List<String> strings = readFile("incorrect-yaml-file.yaml");
        Map<String, LineResult> result = yamlParser.parse(strings);

        assertThat(result.get("firstName").indent(), is(0));
        assertThat(result.get("firstName").tokens().peek().value(), is("firstName"));
        assertThat(result.get("lastName.age").indent(), is(2));
        assertThat(result.get("lastName.age").tokens().peek().value(), is("age"));
        assertThat(result.get("lastName.address.street").indent(), is(4));
        assertThat(result.get("lastName.address.street").tokens().peek().value(), is("street"));
    }

    @Test
    public void testCorrectYaml() throws IOException {
        YamlParser yamlParser = new YamlParser();
        List<String> strings = readFile("correct-yaml-file.yaml");
        Map<String, LineResult> resultMap = yamlParser.parse(strings);

        assertThat(resultMap.size(), is(53));

        assertThat(resultMap.get("security").indent(), is(0));
        assertThat(resultMap.get("security").tokens().peek().type(), is(Token.Type.KEY));
        assertThat(resultMap.get("security").tokens().peek().value(), is("security"));

        assertThat(resultMap.get("security.provider-policy.type").indent(), is(2));
        assertThat(resultMap.get("security.provider-policy.type").tokens().peek().type(), is(Token.Type.KEY));
        assertThat(resultMap.get("security.provider-policy.type").tokens().peek().value(), is("provider-policy.type"));

        assertThat(resultMap.get("security.secrets.provider").indent(), is(4));
        assertThat(resultMap.get("security.secrets.provider").tokens().peek().type(), is(Token.Type.KEY));
        assertThat(resultMap.get("security.secrets.provider").tokens().peek().value(), is("provider"));

        assertThat(resultMap.get("server.sockets.tls.session-timeout-seconds").indent(), is(6));
        assertThat(resultMap.get("server.sockets.tls.session-timeout-seconds").tokens().peek().type(), is(Token.Type.KEY));
        assertThat(resultMap.get("server.sockets.tls.session-timeout-seconds").tokens().peek().value(), is("session-timeout-seconds"));

        System.out.println(resultMap);
    }

    private List<String> readFile(String path) throws IOException {
        String path1 = URLDecoder.decode(
                this.getClass().getClassLoader().getResource(path).getPath(),
                "UTF-8"
        );
        List<String> result = null;
        try (Stream<String> lines = Files.lines(Paths.get(path1))) {
            result = lines.collect(Collectors.toList());
        }
        return result;
    }
}