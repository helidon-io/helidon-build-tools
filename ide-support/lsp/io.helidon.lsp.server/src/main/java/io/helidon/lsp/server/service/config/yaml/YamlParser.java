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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.lsp.server.service.config.PropsDocument;
import io.helidon.lsp.server.utils.LspStringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Parser for the Helidon meta configuration file in YAML format.
 */
public class YamlParser {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String ERROR_LINE_GROUP_NAME = "errorLineNumber";
    private static final String ERROR_LINE_GROUP = String.format("(?<%s>(\\d+))", ERROR_LINE_GROUP_NAME);
    private static final Pattern ERROR_LINE_PATTERN = Pattern.compile(
            String.format(".+line %s, .+", ERROR_LINE_GROUP));

    /**
     * Parse a file that contains configuration for the Helidon and convert it to the PropsDocument object.
     *
     * @param fileContent file content.
     * @return PropsDocument object.
     */
    public PropsDocument parse(List<String> fileContent) {
        String inputYaml = String.join("\n", fileContent);
        PropsDocument propsDocument = null;
        if (inputYaml.isEmpty()) {
            return new PropsDocument();
        }
        try {
            propsDocument = MAPPER.readValue(inputYaml, PropsDocument.class);
        } catch (JsonProcessingException e) {
            String message = e.getMessage();
            String[] splitMessage = message.split("\n");
            List<Integer> errorLineNumbers = new ArrayList<>();
            //trying to find line numbers with errors in yaml document
            for (String errorString : splitMessage) {
                if (errorString.contains("in 'reader', line")) {
                    Integer errorLineNumber = getErrorLineNumber(errorString);
                    if (errorLineNumber == null) {
                        continue;
                    }
                    errorLineNumbers.add(errorLineNumber - 1);
                }
            }
            //remove duplicates
            errorLineNumbers = errorLineNumbers.stream().distinct().collect(Collectors.toList());
            Collections.reverse(errorLineNumbers);
            errorLineNumbers.forEach(errorLine -> fileContent.remove((int) errorLine));
            return parse(fileContent);
        }
        return propsDocument;
    }

    /**
     * Bind nodes in the propsDocument to the appropriate lines in the file that is the source for this PropsDocument.
     *
     * @param propsDocument propsDocument.
     * @param fileContent   fileContent.
     */
    public void setBinding(PropsDocument propsDocument, LinkedList<String> fileContent) {
        AtomicInteger lineNumber = new AtomicInteger();
        int level = 0;
        iterateYamlRecursively(propsDocument, level, fileContent, lineNumber, propsDocument);
    }

    @SuppressWarnings("unchecked")
    private void iterateYamlRecursively(
            Map<String, Object> content,
            int level,
            LinkedList<String> fileContent,
            AtomicInteger lineNumber,
            PropsDocument propsDocument
    ) {
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String line = null;
            do {
                if (fileContent != null && !fileContent.isEmpty()) {
                    line = fileContent.pop();
                    lineNumber.incrementAndGet();
                }
            }
            while (line != null && !line.contains(entry.getKey() + ":"));
            if (line == null) {
                continue;
            }
            PropsDocument.FileBinding binding = new PropsDocument.FileBinding();
            binding.setColumn(LspStringUtils.countStartingSpace(line));
            binding.setRow(lineNumber.get() - 1);
            binding.setLevel(level);
            propsDocument.getBinding().put(entry, binding);
            if (entry.getValue() instanceof Map) {
                iterateYamlRecursively((Map<String, Object>) entry.getValue(), level + 1, fileContent, lineNumber,
                        propsDocument);
            }
        }
    }

    private Integer getErrorLineNumber(String error) {
        Matcher matcher = ERROR_LINE_PATTERN.matcher(error);
        String value = null;
        if (matcher.find()) {
            value = matcher.group(ERROR_LINE_GROUP_NAME);
        }
        if (value != null) {
            return Integer.parseInt(value);
        }
        return null;
    }
}
