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

import io.helidon.build.archetype.engine.v2.ast.Model;
import io.helidon.build.archetype.engine.v2.ast.Node;

/**
 * Model resolver.
 * Builds a tree of model nodes that are merged while building.
 */
final class ModelResolver implements Model.Visitor<Context> {

    private MergedModel head = new MergedModel.Map(null, null, 0);

    /**
     * Get the head.
     *
     * @return head
     */
    MergedModel head() {
        return head;
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
        head.add(new MergedModel.Value(head, value.key(), value.order(), value.value()));
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
}
