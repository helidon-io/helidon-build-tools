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

import java.util.ArrayList;
import java.util.List;

import io.helidon.build.archetype.engine.v2.Expression;
import io.helidon.build.common.xml.XMLElement;

final class VariationRules {

    private VariationRules() {
    }

    static List<Expression> load(XMLElement root) {
        List<Expression> excludes = new ArrayList<>();
        for (XMLElement elt : root.traverse(it -> it.name().equals("exclude"))) {
            excludes.add(exclude(root, elt));
        }
        return List.copyOf(excludes);
    }

    private static Expression exclude(XMLElement root, XMLElement elt) {
        Expression exclude = Expression.create(elt.attribute("if"));
        for (XMLElement n = elt.parent(); n != null; n = n.parent()) {
            if (n.name().equals("rule")) {
                exclude = exclude.and(Expression.create(n.attribute("if")));
            }
            if (n == root) {
                break;
            }
        }
        return exclude;
    }
}
