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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Output;
import io.helidon.build.archetype.engine.v2.ast.Output.Template;
import io.helidon.build.archetype.engine.v2.ast.Output.Transformation;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.spi.TemplateSupport;
import io.helidon.build.common.PropertyEvaluator;
import io.helidon.build.common.SourcePath;

import static io.helidon.build.archetype.engine.v2.spi.TemplateSupport.SUPPORTS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Output generator.
 */
public class OutputGenerator implements Output.Visitor<Context> {

    private final Map<String, Transformation> transformations = new HashMap<>();

    private final Path outputDir;
    private final Block block;

    /**
     * Create a new generator.
     *
     * @param block     block,  must be non {@code null}
     * @param outputDir output directory
     * @param context   context, must be non {@code null}
     * @throws NullPointerException if context or block is {@code null}
     */
    OutputGenerator(Block block, Path outputDir, Context context) {
        this.block = block;
        this.outputDir = outputDir;
        // template support perform full traversal of the tree in order to resolve the global model
        // the context inputs are kept in sync by the input resolver used by the controller
        // there can only be one traversal at a time, thus initializing the template support eagerly.
        TemplateSupport.loadAll(block, context);
    }

    @Override
    public VisitResult visitTransformation(Transformation transformation, Context arg) {
        // not doing a full traversal to get all transformations
        // assuming that transformations are declared before being used...
        transformations.putIfAbsent(transformation.id(), transformation);
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitFile(Output.File file, Context ctx) {
        copy(ctx.cwd().resolve(file.source()), outputDir.resolve(file.target()));
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitFiles(Output.Files files, Context ctx) {
        Path cwd = ctx.cwd();
        Path dir = cwd.resolve(files.directory());
        for (String resource : scan(files, ctx)) {
            Path source = dir.resolve(resource);
            String targetPath = cwd.relativize(cwd.resolve(resource).normalize()).toString();
            Path target = outputDir.resolve(transformations(files, targetPath, ctx));
            copy(source, target);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitTemplates(Output.Templates templates, Context ctx) {
        Path cwd = ctx.cwd();
        Path dir = cwd.resolve(templates.directory());
        for (String resource : scan(templates, ctx)) {
            Path source = dir.resolve(resource);
            String targetPath = cwd.relativize(cwd.resolve(resource).normalize()).toString();
            Path target = outputDir.resolve(transformations(templates, targetPath, ctx));
            render(source, target, templates.engine(), null);
        }
        return VisitResult.CONTINUE;
    }

    @Override
    public VisitResult visitTemplate(Template template, Context ctx) {
        Path source = ctx.cwd().resolve(template.source());
        Path target = outputDir.resolve(template.target());
        render(source, target, template.engine(), template);
        return VisitResult.CONTINUE;
    }

    private List<String> scan(Output.Files files, Context ctx) {
        Path dir = ctx.cwd().resolve(files.directory());
        List<SourcePath> resources = SourcePath.scan(dir);
        return SourcePath.filter(resources, files.includes(), files.excludes())
                         .stream()
                         .map(s -> s.asString(false))
                         .collect(Collectors.toList());
    }

    private void render(Path source, Path target, String engine, Template extraScope) {
        TemplateSupport templateSupport = templateSupport(engine);
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

    private String resolveVariable(String var, Context ctx) {
        Value value = ctx.lookup(var);
        if (value == null) {
            throw new IllegalArgumentException("Unresolved variable: " + var);
        }
        return String.valueOf(value.unwrap());
    }

    private String transformations(Output.Files files, String path, Context ctx) {
        StringBuilder sb = new StringBuilder(path);
        files.transformations()
             .stream()
             .flatMap(id ->
                     Optional.ofNullable(transformations.get(id))
                             .map(t -> t.operations().stream())
                             .orElseThrow(() -> new IllegalArgumentException("Unresolved transformation: " + id)))
             .forEach(op -> {
                 String replacement = PropertyEvaluator.evaluate(op.replacement(), s -> resolveVariable(s, ctx));
                 String current = sb.toString();
                 sb.setLength(0);
                 sb.append(current.replaceAll(op.regex(), replacement));
             });
        return sb.toString();
    }

    private TemplateSupport templateSupport(String engine) {
        return SUPPORTS.get(block).get(engine);
    }
}
