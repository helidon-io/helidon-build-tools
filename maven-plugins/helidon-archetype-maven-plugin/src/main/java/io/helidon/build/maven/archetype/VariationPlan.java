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
package io.helidon.build.maven.archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.Expression;
import io.helidon.build.common.xml.XMLElement;

final class VariationPlan {

    private final String id;
    private final Map<String, String> externalValues;
    private final Map<String, String> externalDefaults;
    private final List<Expression> filters;

    private VariationPlan(String id,
                          Map<String, String> externalValues,
                          Map<String, String> externalDefaults,
                          List<Expression> filters) {
        this.id = Objects.requireNonNull(id);
        this.externalValues = Collections.unmodifiableMap(new LinkedHashMap<>(externalValues));
        this.externalDefaults = Collections.unmodifiableMap(new LinkedHashMap<>(externalDefaults));
        this.filters = List.copyOf(filters);
    }

    static List<VariationPlan> load(Path file) {
        Objects.requireNonNull(file);
        if (!Files.exists(file)) {
            throw new IllegalStateException("Variation plans file does not exist: " + file);
        }
        try (InputStream is = Files.newInputStream(file)) {
            XMLElement root = XMLElement.parse(is);
            if (!root.name().equals("plans")) {
                throw new IllegalStateException("Unexpected variation plans root element: " + root.name());
            }
            List<VariationPlan> plans = load(root);
            if (plans.isEmpty()) {
                throw new IllegalStateException("Variation plans file does not contain any <plan> elements: " + file);
            }
            return plans;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    static List<VariationPlan> load(XMLElement root) {
        Map<String, Template> fragments = new LinkedHashMap<>();
        int fragmentIndex = 1;
        for (XMLElement fragment : root.children("fragment")) {
            Template template = template(fragment, "fragment-" + fragmentIndex++, true);
            if (fragments.putIfAbsent(template.id(), template) != null) {
                throw new IllegalStateException("Duplicate fragment id: " + template.id());
            }
        }

        List<VariationPlan> plans = new ArrayList<>();
        Map<String, ResolvedTemplate> resolvedFragments = new LinkedHashMap<>();
        int index = 1;
        for (XMLElement plan : root.children("plan")) {
            Template template = template(plan, "plan-" + index++, false);
            ResolvedTemplate resolved = resolvePlan(template, fragments, resolvedFragments);
            plans.add(new VariationPlan(template.id(),
                    resolved.externalValues(),
                    resolved.externalDefaults(),
                    resolved.filters()));
        }
        return List.copyOf(plans);
    }

    String id() {
        return id;
    }

    Map<String, String> externalValues() {
        return externalValues;
    }

    Map<String, String> externalDefaults() {
        return externalDefaults;
    }

    List<Expression> filters() {
        return filters;
    }

    private static Map<String, String> readMap(XMLElement root) {
        Map<String, String> values = new LinkedHashMap<>();
        for (XMLElement entry : root.children()) {
            values.put(entry.name(), entry.value());
        }
        return values;
    }

    private static Template template(XMLElement element, String defaultId, boolean requireId) {
        String id = templateId(element, defaultId, requireId);
        return new Template(id,
                extendsIds(element),
                element.child("values")
                        .map(VariationPlan::readMap)
                        .orElse(Map.of()),
                element.child("defaults")
                        .map(VariationPlan::readMap)
                        .orElse(Map.of()),
                element.child("rules")
                        .map(VariationRules::load)
                        .orElse(List.of()));
    }

    private static String templateId(XMLElement element, String defaultId, boolean requireId) {
        String id = element.attribute("id", null);
        if (id == null || id.isBlank()) {
            if (requireId) {
                throw new IllegalStateException("Fragment is missing required id attribute");
            }
            return defaultId;
        }
        return id.trim();
    }

    private static List<String> extendsIds(XMLElement element) {
        String value = element.attribute("extends", "").trim();
        if (value.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String token : value.split(",")) {
            String id = token.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static ResolvedTemplate resolvePlan(Template plan,
                                                Map<String, Template> fragments,
                                                Map<String, ResolvedTemplate> resolvedFragments) {
        ResolvedTemplate inherited = resolveParents(plan, fragments, resolvedFragments, new ArrayList<>());
        return plan.merge(inherited);
    }

    private static ResolvedTemplate resolveParents(Template template,
                                                   Map<String, Template> fragments,
                                                   Map<String, ResolvedTemplate> resolvedFragments,
                                                   List<String> stack) {

        LinkedHashMap<String, String> externalValues = new LinkedHashMap<>();
        LinkedHashMap<String, String> externalDefaults = new LinkedHashMap<>();
        List<Expression> filters = new ArrayList<>();
        for (String fragmentId : template.extendsIds()) {
            Template fragment = fragments.get(fragmentId);
            if (fragment == null) {
                throw new IllegalStateException(
                        "Unknown fragment '%s' referenced by '%s'"
                                .formatted(fragmentId, template.id()));
            }
            ResolvedTemplate resolved = resolveFragment(fragment, fragments, resolvedFragments, stack);
            externalValues.putAll(resolved.externalValues());
            externalDefaults.putAll(resolved.externalDefaults());
            filters.addAll(resolved.filters());
        }
        return new ResolvedTemplate(externalValues, externalDefaults, filters);
    }

    private static ResolvedTemplate resolveFragment(Template fragment,
                                                    Map<String, Template> fragments,
                                                    Map<String, ResolvedTemplate> resolvedFragments,
                                                    List<String> stack) {
        ResolvedTemplate cached = resolvedFragments.get(fragment.id());
        if (cached != null) {
            return cached;
        }
        if (stack.contains(fragment.id())) {
            List<String> cycle = new ArrayList<>(stack);
            cycle.add(fragment.id());
            throw new IllegalStateException("Circular fragment inheritance: " + String.join(" -> ", cycle));
        }
        stack.add(fragment.id());
        ResolvedTemplate inherited = resolveParents(fragment, fragments, resolvedFragments, stack);
        ResolvedTemplate resolved = fragment.merge(inherited);
        stack.remove(stack.size() - 1);
        resolvedFragments.put(fragment.id(), resolved);
        return resolved;
    }

    private record Template(String id,
                            List<String> extendsIds,
                            Map<String, String> externalValues,
                            Map<String, String> externalDefaults,
                            List<Expression> filters) {

        ResolvedTemplate merge(ResolvedTemplate inherited) {
            Map<String, String> mergedValues = new LinkedHashMap<>(inherited.externalValues());
            Map<String, String> mergedDefaults = new LinkedHashMap<>(inherited.externalDefaults());
            List<Expression> mergedFilters = new ArrayList<>(inherited.filters());
            mergedValues.putAll(externalValues);
            mergedDefaults.putAll(externalDefaults);
            mergedFilters.addAll(filters);
            return new ResolvedTemplate(mergedValues, mergedDefaults, mergedFilters);
        }
    }

    private record ResolvedTemplate(Map<String, String> externalValues,
                                    Map<String, String> externalDefaults,
                                    List<Expression> filters) {
    }
}
