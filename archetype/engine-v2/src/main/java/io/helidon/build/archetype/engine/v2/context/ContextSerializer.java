/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Context serializer.
 */
public class ContextSerializer implements ContextEdge.Visitor {

    private static final Set<ContextValue.ValueKind> DEFAULT_VALUE_KIND_FILTER =
            Set.of(ContextValue.ValueKind.EXTERNAL, ContextValue.ValueKind.USER);
    private static final Predicate<ContextEdge> DEFAULT_CONTEXT_FILTER =
            edge ->
                    edge.value() != null
                            && !edge.node().visibility().equals(ContextScope.Visibility.UNSET)
                            && DEFAULT_VALUE_KIND_FILTER.contains(edge.value().kind());
    private static final Function<String, String> DEFAULT_VALUE_MAPPER = value -> {
        Set<Character> forRemoval = Set.of('[', ']');
        if (value == null || value.length() == 0) {
            return value;
        }
        StringBuilder builder = new StringBuilder(value);
        if (forRemoval.contains(builder.charAt(0))) {
            builder.deleteCharAt(0);
        }
        if (builder.length() > 0 && forRemoval.contains(builder.charAt(builder.length() - 1))) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    };
    private static final String DEFAULT_VALUE_DELIMITER = ",";

    private final Map<String, String> result;
    private final Predicate<ContextEdge> filter;
    private final CharSequence valueDelimiter;
    private final Function<String, String> valueMapper;

    private ContextSerializer(Map<String, String> result,
                              Predicate<ContextEdge> filter,
                              Function<String, String> valueMapper,
                              CharSequence valueDelimiter) {
        this.result = result;
        this.filter = filter;
        this.valueMapper = valueMapper;
        this.valueDelimiter = valueDelimiter;
    }

    @Override
    public void visit(ContextEdge edge) {
        ContextNode node = edge.node();
        ContextNode parent = node.parent0();
        if (parent != null) {
            String key = node.path();
            Set<String> valueSet = edge.variations().stream()
                                       .filter(filter)
                                       .map(variation -> variation.value().unwrap().toString())
                                       .map(valueMapper)
                                       .collect(Collectors.toSet());
            if (valueSet.size() > 0) {
                result.put(key, String.join(valueDelimiter, valueSet));
            }
        }
    }

    /**
     * Visit the given archetype context and return values from the context in form of a map where keys are paths of nodes and
     * values are related values for these nodes.
     *
     * @param context        context for processing
     * @param filter         filter for context values
     * @param valueMapper    mapper for the values of the nodes
     * @param valueDelimiter delimiter for the values
     * @return map where keys are paths of nodes and values are related values for these nodes
     */
    public static Map<String, String> serialize(Context context,
                                                Predicate<ContextEdge> filter,
                                                Function<String, String> valueMapper,
                                                CharSequence valueDelimiter) {
        Map<String, String> result = new HashMap<>();
        context.scope().visitEdges(new ContextSerializer(result, filter, valueMapper, valueDelimiter), false);
        return result;
    }

    /**
     * Visit the given archetype context and return values from the context that were used by an user during the project
     * generation in form of a map where keys are paths of nodes and values are related values for these nodes.
     *
     * @param context context for processing
     * @return map where keys are paths of nodes and values are related values for these nodes
     */
    public static Map<String, String> serialize(Context context) {
        Map<String, String> result = new HashMap<>();
        context.scope()
               .visitEdges(new ContextSerializer(result, DEFAULT_CONTEXT_FILTER, DEFAULT_VALUE_MAPPER, DEFAULT_VALUE_DELIMITER),
                       false);
        return result;
    }
}
