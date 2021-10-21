/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.util.Map;

/**
 * Class PropertyEvaluator.
 */
public class PropertyEvaluator {

    private PropertyEvaluator() {
    }

    /**
     * Resolve a property of the form <code>${prop}</code>.
     *
     * @param input input to be resolved
     * @param properties properties values
     * @return resolved property
     */
    public static String evaluate(String input, Map<String, String> properties) {
        int start = input.indexOf("${");
        int end = input.indexOf("}", start);
        int index = 0;
        String resolved = null;
        while (start >= 0 && end > 0) {
            if (resolved == null) {
                resolved = input.substring(index, start);
            } else {
                resolved += input.substring(index, start);
            }
            String propName = input.substring(start + 2, end);

            // search for transformation (name/regexp/replace)
            int matchStart = 0;
            do {
                matchStart = propName.indexOf("/", matchStart + 1);
            } while (matchStart > 0 && propName.charAt(matchStart - 1) == '\\');
            int matchEnd = matchStart;
            do {
                matchEnd = propName.indexOf("/", matchEnd + 1);
            } while (matchStart > 0 && propName.charAt(matchStart - 1) == '\\');

            String regexp = null;
            String replace = null;
            if (matchStart > 0 && matchEnd > matchStart) {
                regexp = propName.substring(matchStart + 1, matchEnd);
                replace = propName.substring(matchEnd + 1);
                propName = propName.substring(0, matchStart);
            }

            String propValue = properties.get(propName);
            if (propValue == null) {
                propValue = "";
            } else if (regexp != null && replace != null) {
                propValue = propValue.replaceAll(regexp, replace);
            }

            resolved += propValue;
            index = end + 1;
            start = input.indexOf("${", index);
            end = input.indexOf("}", index);
        }
        if (resolved != null) {
            return resolved + input.substring(index);
        }
        return input;
    }

    /**
     * Resolve a property of the form <code>${prop}</code> in the input string and return the input string where all found
     * properties are replaced by the actual values from the map {@code properties}.
     *
     * @param input      input to be resolved
     * @param properties properties values
     * @return resolved input string
     */
    public static String resolve(String input, Map<String, String> properties) {
        int start = input.indexOf("${");
        int end = input.indexOf("}", start);
        while (start >= 0 && end > 0) {
            String target = input.substring(start, end + 1);
            String propName = input.substring(start + 2, end);
            String replacement = properties.get(propName);
            if (replacement != null) {
                input = input.replace(target, replacement);
            } else {
                input = input.replace(target, "");
            }
            start = input.indexOf("${");
            end = input.indexOf("}");
        }
        return input;
    }
}
