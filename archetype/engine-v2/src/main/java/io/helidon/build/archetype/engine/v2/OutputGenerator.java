/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Output.Replace;
import io.helidon.build.archetype.engine.v2.ast.Output.Template;
import io.helidon.build.archetype.engine.v2.ast.Output.Transformation;
import io.helidon.build.archetype.engine.v2.context.Context;
import io.helidon.build.archetype.engine.v2.spi.TemplateSupport;
import io.helidon.build.common.SourcePath;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Output generator.
 */
public class OutputGenerator implements Output.Visitor<Context> {

    private final Map<String, List<Replace>> transformations = new HashMap<>();
    private final List<String> includes = new LinkedList<>();
    private final List<String> excludes = new LinkedList<>();
    private final Path outputDir;
    private final MergedModel model;
    private String transformationId;

    /**
     * Create a new generator.
     *
     * @param model     model
     * @param outputDir output directory
     */
    OutputGenerator(MergedModel model, Path outputDir) {
        this.model = model;
        this.outputDir = outputDir;
    }

    @Override
    public VisitResult visitTransformation(Transformation transformation, Context context) {
        // transformations are not scoped
        // visiting order applies, can be overridden.
        transformationId = transformation.id();
        transformations.put(transformationId, new LinkedList<>());
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitReplace(Replace replace, Context context) {
        transformations.get(transformationId).add(replace);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitTransformation(Transformation transformation, Context context) {
        transformationId = null;
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitFile(Output.File file, Context context) {
        copy(context.cwd().resolve(file.source()), outputDir.resolve(file.target()));
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitFiles(Output.Files files, Context arg) {
        includes.clear();
        excludes.clear();
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitTemplates(Output.Templates templates, Context arg) {
        includes.clear();
        excludes.clear();
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitInclude(Output.Include include, Context context) {
        includes.add(include.value());
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitExclude(Output.Exclude exclude, Context context) {
        excludes.add(exclude.value());
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitFiles(Output.Files files, Context context) {
        Path cwd = context.cwd();
        Path dir = cwd.resolve(files.directory());
        for (String resource : scan(files, context)) {
            Path source = dir.resolve(resource);
            String targetPath = cwd.relativize(cwd.resolve(resource).normalize()).toString();
            Path target = outputDir.resolve(transformations(files, targetPath, context));
            copy(source, target);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult postVisitTemplates(Output.Templates templates, Context context) {
        Path cwd = context.cwd();
        Path dir = cwd.resolve(templates.directory());
        for (String resource : scan(templates, context)) {
            Path source = dir.resolve(resource);
            String targetPath = cwd.relativize(cwd.resolve(resource).normalize()).toString();
            Path target = outputDir.resolve(transformations(templates, targetPath, context));
            render(source, target, templates.engine(), null, context);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitTemplate(Template template, Context context) {
        Path source = context.cwd().resolve(template.source());
        Path target = outputDir.resolve(template.target());
        render(source, target, template.engine(), template, context);
        return VisitResult.CONTINUE;
    }

    private List<String> scan(Output.Files files, Context context) {
        Path dir = context.cwd().resolve(files.directory());
        List<SourcePath> resources = SourcePath.scan(dir);
        return SourcePath.filter(resources, includes, excludes)
                         .stream()
                         .map(s -> s.asString(false))
                         .collect(Collectors.toList());
    }

    private void render(Path source, Path target, String engine, Template extraScope, Context context) {
        TemplateSupport templateSupport = TemplateSupport.get(engine, model, context);
        try {
            Files.createDirectories(target.getParent());
            InputStream is = Files.newInputStream(source);
            OutputStream os = Files.newOutputStream(target);
            templateSupport.render(is, source.toAbsolutePath().toString(), UTF_8, os, extraScope);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void copy(Path source, Path target) {
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private List<Replace> transformationOps(String id) {
        List<Replace> ops = transformations.get(id);
        if (ops != null) {
            return ops;
        }
        throw new IllegalArgumentException("Unresolved transformation: " + id);
    }

    private String transformations(Output.Files files, String path, Context context) {
        StringBuilder sb = new StringBuilder(path);
        files.transformations()
             .stream()
             .flatMap(id -> transformationOps(id).stream())
             .forEach(op -> {
                 String replacement = context.scope().interpolate(op.replacement());
                 String current = sb.toString();
                 sb.setLength(0);
                 sb.append(current.replaceAll(op.regex(), replacement));
             });
        return sb.toString();
    }
}
