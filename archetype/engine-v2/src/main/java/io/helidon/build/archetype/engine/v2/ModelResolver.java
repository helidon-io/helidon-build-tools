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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.common.PropertyEvaluator;

/**
 * Model resolver.
 * Builds a tree of model nodes that are merged while building.
 */
final class ModelResolver implements Model.Visitor<Context> {

    private final MergedModel model;
    private MergedModel.Node head;

    /**
     * Create a new model resolver.
     *
     * @param block block
     */
    ModelResolver(Block block) {
        model = new MergedModel(block, new MergedModel.Map(null, null, 0));
        head = model.node();
    }

    /**
     * Get the model.
     *
     * @return model
     */
    MergedModel model() {
        return model;
    }

    @Override
    public Node.VisitResult visitList(Model.List list, Context ctx) {
        head = head.add(new MergedModel.List(head, list.key(), list.order()));
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitMap(Model.Map map, Context ctx) {
        head = head.add(new MergedModel.Map(head, map.key(), map.order()));
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult visitValue(Model.Value value, Context ctx) {
        // value is a leaf-node, thus we are not updating the head
        head.add(new MergedModel.Value(head, value.key(), value.order(), evaluate(value, ctx), value.template()));
        return Node.VisitResult.CONTINUE;
    }

    @Override
    public Node.VisitResult postVisitList(Model.List list, Context ctx) {
        head.sort();
        return postVisitAny(list, ctx);
    }

    @Override
    public Node.VisitResult postVisitAny(Model model, Context ctx) {
        head = head.parent();
        return Node.VisitResult.CONTINUE;
    }

    private static String valueContent(Model.Value value, Context ctx) throws IOException {
        String content = value.value();
        if (content == null) {
            String file = value.file();
            if (file == null) {
                throw new IllegalStateException("Value has no content");
            }
            Path contentFile = ctx.cwd().resolve(file);
            if (!Files.exists(contentFile)) {
                throw new IllegalStateException("Value content file does not exist: " + contentFile);
            }
            content = Files.readString(contentFile);
        }
        return content;
    }

    private static String evaluate(Model.Value value, Context ctx) {
        try {
            String content = valueContent(value, ctx);
            String template = value.template();
            if (template == null) {
                return PropertyEvaluator.evaluate(content, s -> String.valueOf(ctx.lookup(s).unwrap()));
            } else {
                return content;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
