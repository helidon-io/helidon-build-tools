/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.Node;
import io.helidon.build.archetype.engine.v2.Node.Kind;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Regex helper.
 */
final class Regex {

    private Regex() {
    }

    /**
     * Validate the regular expressions in the given block.
     *
     * @param block block
     * @return errors
     */
    static List<String> validate(Node block) {
        List<String> errors = new ArrayList<>();
        for (Node node : block.traverse(Kind.REGEX::equals)) {
            String validationId = node.parent().attribute("id").getString();
            String pattern = node.value().getString();
            try (Context context = Context.enter()) {
                Scriptable scope = context.initStandardObjects();
                context.evaluateString(scope, pattern, "regex", 1, null);
            } catch (Exception e) {
                errors.add(String.format(
                        "%s: Regular expression '%s' at validation '%s' is not JavaScript compatible",
                        node.location(),
                        pattern,
                        validationId));
            }
        }
        return errors;
    }
}
