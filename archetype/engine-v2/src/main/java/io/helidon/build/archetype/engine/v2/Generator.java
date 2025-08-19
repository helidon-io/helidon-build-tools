/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.common.SourcePath;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Output generator.
 */
public class Generator implements Node.Visitor {

    private final Map<String, List<Map.Entry<String, String>>> transformations = new HashMap<>();
    private final List<String> includes = new ArrayList<>();
    private final List<String> excludes = new ArrayList<>();
    private final Path outputDir;
    private final TemplateModel model;
    private final Context context;
    private String tId;

    /**
     * Create a new generator.
     *
     * @param model     model
     * @param context   context
     * @param outputDir output directory
     */
    Generator(TemplateModel model, Context context, Path outputDir) {
        this.model = model;
        this.context = context;
        this.outputDir = outputDir;
    }

    @Override
    public boolean visit(Node node) {
        switch (node.kind()) {
            case TRANSFORMATION:
                // transformations are not scoped
                // visiting order applies, can be overridden.
                tId = node.attribute("id").getString();
                break;
            case REPLACE:
                String regex = node.attribute("regex").getString();
                String replacement = node.attribute("replacement").getString();
                transformations.computeIfAbsent(tId, id -> new ArrayList<>())
                        .add(Map.entry(regex, replacement));
                break;
            case FILE:
            case TEMPLATE:
                Path source = context.cwd().resolve(node.attribute("source").getString());
                Path target = outputDir.resolve(node.attribute("target").getString());
                if (node.kind() == Node.Kind.FILE) {
                    copy(source, target);
                } else {
                    String engine = node.attribute("engine").getString();
                    render(source, target, engine, node, context);
                }
                break;
            case FILES:
            case TEMPLATES:
                includes.clear();
                excludes.clear();
                break;
            case INCLUDE:
                includes.add(node.value().getString());
                break;
            case EXCLUDE:
                excludes.add(node.value().getString());
                break;
            default:
        }
        return true;
    }

    @Override
    public void postVisit(Node node) {
        switch (node.kind()) {
            case TRANSFORMATION:
                tId = null;
                break;
            case FILES:
            case TEMPLATES:
                Path cwd = context.cwd();
                Path dir = cwd.resolve(node.attribute("directory").getString());
                for (String resource : scan(dir)) {
                    Path source = dir.resolve(resource);
                    String targetPath = cwd.relativize(cwd.resolve(resource).normalize()).toString();
                    List<String> transformations = node.attribute("transformations").asList().orElse(List.of());
                    Path target = outputDir.resolve(transform(targetPath, transformations, context));
                    if (node.kind() == Node.Kind.FILES) {
                        copy(source, target);
                    } else {
                        String engine = node.attribute("engine").getString();
                        render(source, target, engine, node, context);
                    }
                }
                break;
            default:
        }
    }

    private List<String> scan(Path dir) {
        List<SourcePath> files = SourcePath.scan(dir);
        return SourcePath.filter(files, includes, excludes)
                .stream()
                .map(s -> s.asString(false))
                .collect(Collectors.toList());
    }

    private void render(Path source, Path target, String engine, Node extraScope, Context context) {
        try {
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                InputStream is = Files.newInputStream(source);
                OutputStream os = Files.newOutputStream(target);
                if (TemplateSupport.isSupported(engine)) {
                    TemplateSupport ts = new TemplateSupport(model, context);
                    ts.render(is, source.toAbsolutePath().toString(), UTF_8, os, extraScope);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void copy(Path source, Path target) {
        try {
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private List<Map.Entry<String, String>> transformationOps(String id) {
        List<Map.Entry<String, String>> ops = transformations.get(id);
        if (ops != null) {
            return ops;
        }
        throw new IllegalArgumentException("Unresolved transformation: " + id);
    }

    private String transform(String path, List<String> transformations, Context context) {
        String str = path;
        for (String id : transformations) {
            for (Map.Entry<String, String> op : transformationOps(id)) {
                str = str.replaceAll(op.getKey(), context.scope().interpolate(op.getValue()));
            }
        }
        return str;
    }
}
