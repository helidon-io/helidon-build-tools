/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.common.Strings.requireValid;

/**
 * Internal model for a list of variables.
 */
final class Variables extends LinkedHashMap<String, List<Map<String, String>>> {

    private final boolean join;

    Variables(XMLElement element) {
        join = element.attributeBoolean("join", false);
        for (XMLElement variableElt : element.children()) {
            if ("variable".equals(variableElt.name())) {
                String name = requireValid(variableElt.attribute("name", null), "name is required");
                List<Map<String, String>> values = new ArrayList<>();
                for (XMLElement valueElt : variableElt.children()) {
                    if ("value".equals(valueElt.name())) {
                        Map<String, String> value = new HashMap<>();
                        String text = valueElt.value();
                        if (!text.isBlank()) {
                            value.put(name, text);
                        } else {
                            Map<String, String> attributes = valueElt.attributes();
                            if (!attributes.isEmpty()) {
                                value.putAll(attributes);
                            }
                        }
                        values.add(value);
                    }
                }
                put(name, values);
            }
        }
    }

    boolean join() {
        return join;
    }
}
