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
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.interpreter.ASTNode;
import io.helidon.build.archetype.engine.v2.interpreter.ContextAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.ContextNodeASTFactory;
import io.helidon.build.archetype.engine.v2.interpreter.ContextTextAST;
import io.helidon.build.archetype.engine.v2.interpreter.Flow;
import io.helidon.build.archetype.engine.v2.interpreter.InputNodeAST;
import io.helidon.build.archetype.engine.v2.interpreter.UserInputAST;
import io.helidon.build.archetype.engine.v2.interpreter.Visitor;
import io.helidon.build.archetype.engine.v2.prompter.Prompt;
import io.helidon.build.archetype.engine.v2.prompter.PromptFactory;
import io.helidon.build.archetype.engine.v2.prompter.Prompter;

/**
 * Archetype engine (version 2).
 */
public class ArchetypeEngineV2 {

    private final Archetype archetype;
    private final String startPoint;
    private final Prompter prompter;
    private final Map<String, String> externalValues = new HashMap<>();
    private final Map<String, String> externalDefaults = new HashMap<>();
    private final boolean failOnUnresolvedInput;
    private boolean skipOptional;
    private List<Visitor<ASTNode>> additionalVisitors = new ArrayList<>();

    /**
     * Create a new archetype engine instance.
     * @param archetype          archetype
     * @param startPoint         entry point in the archetype
     * @param prompter           prompter
     * @param presets            external Flow Context values
     * @param defaults           external Flow Context default values
     * @param skipOptional       mark that indicates whether to skip optional input
     * @param failOnUnresolvedInput fail if there are any unresolved inputs
     * @param additionalVisitors additional Visitor for the {@code Interpreter}
     */
    public ArchetypeEngineV2(Archetype archetype,
                             String startPoint,
                             Prompter prompter,
                             Map<String, String> presets,
                             Map<String, String> defaults,
                             boolean skipOptional,
                             boolean failOnUnresolvedInput,
                             List<Visitor<ASTNode>> additionalVisitors) {
        this.archetype = archetype;
        this.startPoint = startPoint;
        this.prompter = prompter;
        if (presets != null) {
            externalValues.putAll(presets);
        }
        if (defaults != null) {
            externalDefaults.putAll(defaults);
        }
        this.skipOptional = skipOptional;
        this.failOnUnresolvedInput = failOnUnresolvedInput;
        if (additionalVisitors != null) {
            this.additionalVisitors.addAll(additionalVisitors);
        }
    }

    /**
     * Run the archetype.
     *
     * @param projectDirSupplier maps project name to project directory
     * @return The project directory
     */
    public Path generate(Function<String, Path> projectDirSupplier) {
        Flow flow = Flow.builder()
                .archetype(archetype)
                .startDescriptorPath(startPoint)
                .skipOptional(skipOptional)
                .externalValues(externalValues)
                .externalDefaults(externalDefaults)
                .addAdditionalVisitor(additionalVisitors)
                .build();

        ContextAST context = initContext();
        flow.build(context);
        while (!flow.unresolvedInputs().isEmpty()) {
            UserInputAST userInputAST = flow.unresolvedInputs().get(0);
            ContextNodeAST contextNodeAST;
            String path = userInputAST.path();
            if (externalValues.containsKey(path)) {
                contextNodeAST = ContextNodeASTFactory.create(
                        (InputNodeAST) userInputAST.children().get(0),
                        userInputAST.path(),
                        externalValues.get(path)
                );
            } else if (failOnUnresolvedInput) {
                throw new UnresolvedInputException(path);
            } else {
                Prompt<?> prompt = PromptFactory.create(userInputAST, flow.canBeGenerated());
                contextNodeAST = prompt.acceptAndConvert(prompter, path);
                flow.skipOptional(prompter.skipOptional());
            }
            ContextAST contextAST = new ContextAST();
            contextAST.children().add(contextNodeAST);
            flow.build(contextAST);
        }

        String projectName = ((ContextTextAST) flow.pathToContextNodeMap().get("project.name")).text();
        Path projectDir = projectDirSupplier.apply(projectName);
        ContextTextAST projectDirNode = new ContextTextAST("project.directory");
        projectDirNode.text(projectDir.toString());
        context.children().add(projectDirNode);
        flow.pathToContextNodeMap().put("project.directory", projectDirNode);

        flow.build(new ContextAST());
        Flow.Result result = flow.result().orElseThrow(() -> {
            throw new RuntimeException("No results after the Flow instance finished its work. Project cannot be generated.");
        });

        OutputGenerator outputGenerator = new OutputGenerator(result);
        try {
            outputGenerator.generate(projectDir.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                result.archetype().close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return projectDir;
    }

    private static ContextAST initContext() {
        ContextAST context = new ContextAST();
        ContextTextAST currentDateNode = new ContextTextAST("current.date");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
        ZonedDateTime now = ZonedDateTime.now();
        currentDateNode.text(dtf.format(now));
        context.children().add(currentDateNode);
        return context;
    }
}
