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

import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.common.xml.XMLElement;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.Wrapper;

/**
 * Mustache object handler for ordered XML template models.
 */
final class TemplateHandler extends BaseObjectHandler {

    private static final Set<String> METADATA = Set.of("name", "value", "index", "first", "last");
    private static final Lookup MISSING = new Lookup(State.MISSING, null);
    private static final Lookup BLOCKED = new Lookup(State.BLOCKED, null);

    @Override
    public Binding createBinding(String name, TemplateContext context, Code code) {
        return scopes -> lookup(name, scopes);
    }

    @Override
    public Wrapper find(String name, List<Object> scopes) {
        return actualScopes -> lookup(name, actualScopes);
    }

    @Override
    public Writer iterate(Iteration it, Writer writer, Object object, List<Object> scopes) {
        if (object instanceof XMLElement element) {
            if (!element.children().isEmpty()) {
                return iterate(it, writer, element, element.children(), scopes);
            }
            return element.value().isEmpty() ? writer : it.next(writer, element, scopes);
        }
        if (object instanceof Selection selection) {
            return iterate(it, writer, selection.parent, selection.children, scopes);
        }
        if (object instanceof Item item) {
            return truthy(item.element) ? it.next(writer, item, scopes) : writer;
        }
        return super.iterate(it, writer, object, scopes);
    }

    @Override
    public Writer falsey(Iteration it, Writer writer, Object object, List<Object> scopes) {
        if (object instanceof XMLElement element) {
            return truthy(element) ? writer : it.next(writer, object, scopes);
        }
        if (object instanceof Selection selection) {
            return selection.children.isEmpty() ? it.next(writer, object, scopes) : writer;
        }
        if (object instanceof Item item) {
            return truthy(item.element) ? writer : it.next(writer, object, scopes);
        }
        return super.falsey(it, writer, object, scopes);
    }

    @Override
    public String stringify(Object object) {
        if (object instanceof XMLElement element) {
            return element.value();
        }
        if (object instanceof Item item) {
            return item.element.value();
        }
        if (object instanceof Selection selection) {
            return selection.children.size() == 1 ? selection.children.get(0).value() : "";
        }
        return super.stringify(object);
    }

    private static boolean truthy(XMLElement element) {
        return !element.children().isEmpty() || !element.value().isEmpty();
    }

    private static Writer iterate(Iteration it,
                                  Writer writer,
                                  XMLElement container,
                                  List<XMLElement> children,
                                  List<Object> scopes) {
        int size = children.size();
        for (int index = 0; index < size; index++) {
            writer = it.next(writer, new Item(children.get(index), container, index, size), scopes);
        }
        return writer;
    }

    private static Object lookup(String name, List<Object> scopes) {
        for (int index = scopes.size() - 1; index >= 0; index--) {
            var lookup = lookupScope(scopes.get(index), name);
            if (lookup.state != State.MISSING) {
                return lookup.value;
            }
        }
        return null;
    }

    private static Lookup lookupScope(Object scope, String name) {
        if (scope instanceof Item item) {
            return lookupItem(item, name);
        }
        if (scope instanceof XMLElement element) {
            return lookupElement(element, name, false);
        }
        if (scope instanceof Selection selection) {
            return descend(selection, name);
        }
        if (scope instanceof Map<?, ?> map) {
            return lookupMap(map, name);
        }
        return MISSING;
    }

    private static Lookup lookupItem(Item item, String name) {
        var local = lookupElement(item.element, name, false);
        if (local.state != State.MISSING) {
            return local;
        }
        var metadata = lookupMetadata(item, name);
        if (metadata.state != State.MISSING) {
            return metadata;
        }
        return lookupElement(item.container, name, false);
    }

    private static Lookup lookupMetadata(Item item, String path) {
        int dot = path.indexOf('.');
        String field = dot < 0 ? path : path.substring(0, dot);
        int offset = 0;
        while (offset < field.length() && field.charAt(offset) == '_') {
            offset++;
        }
        var metadataName = field.substring(offset);
        if (!METADATA.contains(metadataName)) {
            return MISSING;
        }
        var value = item.metadata(metadataName);
        if (dot < 0) {
            return new Lookup(State.FOUND, value);
        }
        var nested = descend(value, path.substring(dot + 1));
        return nested.state == State.MISSING ? BLOCKED : nested;
    }

    private static Lookup lookupElement(XMLElement element, String path, boolean selectMatch) {
        if (path.startsWith("@")) {
            return lookupAttribute(element, path);
        }
        var exact = element.children(path);
        if (!exact.isEmpty()) {
            return new Lookup(State.FOUND, childValue(element, exact, selectMatch));
        }
        for (int dot = path.lastIndexOf('.'); dot > 0; dot = path.lastIndexOf('.', dot - 1)) {
            var children = element.children(path.substring(0, dot));
            if (!children.isEmpty()) {
                var nested = descend(childValue(element, children, false), path.substring(dot + 1));
                return nested.state == State.MISSING ? BLOCKED : nested;
            }
        }
        return MISSING;
    }

    private static Lookup lookupAttribute(XMLElement element, String path) {
        var name = path.substring(1);
        if (element.attributes().containsKey(name)) {
            return new Lookup(State.FOUND, element.attributes().get(name));
        }
        for (int dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.', dot - 1)) {
            var prefix = name.substring(0, dot);
            if (element.attributes().containsKey(prefix)) {
                return BLOCKED;
            }
        }
        return MISSING;
    }

    private static Lookup lookupMap(Map<?, ?> map, String path) {
        if (map.containsKey(path)) {
            return new Lookup(State.FOUND, map.get(path));
        }
        for (int dot = path.lastIndexOf('.'); dot > 0; dot = path.lastIndexOf('.', dot - 1)) {
            var prefix = path.substring(0, dot);
            if (map.containsKey(prefix)) {
                var nested = descend(map.get(prefix), path.substring(dot + 1));
                return nested.state == State.MISSING ? BLOCKED : nested;
            }
        }
        return MISSING;
    }

    private static Lookup descend(Object value, String path) {
        if (value instanceof XMLElement element) {
            return lookupElement(element, path, true);
        }
        if (value instanceof Selection selection) {
            if (selection.children.size() != 1) {
                return BLOCKED;
            }
            return lookupElement(selection.children.get(0), path, true);
        }
        if (value instanceof Item item) {
            return lookupItem(item, path);
        }
        if (value instanceof Map<?, ?> map) {
            return lookupMap(map, path);
        }
        return BLOCKED;
    }

    private static Object childValue(XMLElement parent, List<XMLElement> children, boolean selectMatch) {
        return selectMatch || children.size() > 1 ? new Selection(parent, children) : children.get(0);
    }

    private enum State {
        FOUND,
        MISSING,
        BLOCKED
    }

    private record Lookup(State state, Object value) {
    }

    private record Selection(XMLElement parent, List<XMLElement> children) {
    }

    private record Item(XMLElement element, XMLElement container, int index, int size) {

        private Object metadata(String name) {
            return switch (name) {
                case "name" -> element.name();
                case "value" -> element;
                case "index" -> index;
                case "first" -> index == 0;
                case "last" -> index == size - 1;
                default -> null;
            };
        }
    }
}
