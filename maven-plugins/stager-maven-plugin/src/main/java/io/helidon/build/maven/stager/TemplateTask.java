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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

import com.github.mustachejava.DefaultMustacheFactory;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Render a mustache template.
 */
final class TemplateTask extends StagingTask {

    private static final TemplateHandler MODEL_HANDLER = new TemplateHandler();

    private final String source;
    private final String target;
    private final XMLElement model;

    TemplateTask(XMLElement element) {
        super(element);
        this.source = Strings.requireValid(element.attribute("source", null), "source is required");
        this.target = Strings.requireValid(element.attribute("target", null), "target is required");
        this.model = element.child("model")
                .map(XMLElement::builder)
                .orElseGet(() -> XMLElement.builder().name("model"));
        validate(model, "model");
    }

    @Override
    public String toString() {
        return "TemplateTask{"
               + "source='" + source + '\''
               + ", target='" + target + '\''
               + ", model=" + model.toString(false)
               + '}';
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        var resolvedTarget = resolveVar(target, vars);
        var resolvedSource = resolveVar(source, vars);
        var sourceFile = ctx.resolve(resolvedSource);
        if (!Files.exists(sourceFile)) {
            throw new IllegalStateException(sourceFile + " does not exist");
        }
        var targetFile = dir.resolve(resolvedTarget).normalize();
        ctx.ensureDirectory(targetFile.getParent());
        try (var reader = Files.newBufferedReader(sourceFile);
                var writer = Files.newBufferedWriter(targetFile, CREATE, TRUNCATE_EXISTING)) {
            var factory = new DefaultMustacheFactory();
            factory.setObjectHandler(MODEL_HANDLER);
            factory.compile(reader, resolvedSource)
                    .execute(writer, resolve(model, vars))
                    .flush();
        }
    }

    private static XMLElement resolve(XMLElement model, Map<String, String> vars) {
        var resolved = XMLElement.builder(model);
        resolved.visit(new XMLElement.Visitor() {
            @Override
            public boolean visitElement(XMLElement elt) {
                elt.value(resolve(elt.value(), vars));
                elt.attributes().replaceAll((name, value) -> resolve(value, vars));
                return true;
            }
        });
        return resolved;
    }

    private static void validate(XMLElement element, String path) {
        if (!element.children().isEmpty() && !element.value().isBlank()) {
            throw new IllegalArgumentException("Model element '%s' cannot mix text and child elements".formatted(path));
        }
        for (var child : element.children()) {
            validate(child, path + "." + child.name());
        }
    }

    private static String resolve(String value, Map<String, String> vars) {
        var result = value;
        for (var variable : vars.entrySet()) {
            result = result.replace("{" + variable.getKey() + "}", variable.getValue());
        }
        return result;
    }
}
