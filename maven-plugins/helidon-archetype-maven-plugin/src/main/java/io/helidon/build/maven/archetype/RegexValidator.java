/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Validation;
import io.helidon.build.archetype.engine.v2.context.Context;

import org.mozilla.javascript.Scriptable;

class RegexValidator implements Node.Visitor<Context>, Block.Visitor<Context> {

    private static final String ERROR_FORMAT = "Regular expression '%s' at validation '%s' is not JavaScript compatible";

    private final List<String> errors = new LinkedList<>();
    private String validationId;

    static List<String> validate(Path script) {
        RegexValidator validator = new RegexValidator();
        Context context = Context.builder()
                                 .cwd(script.getParent())
                                 .build();
        Walker.walk(validator, ScriptLoader.load(script), context, context::cwd);
        return validator.errors();
    }

    @Override
    public Node.VisitResult visitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.pushCwd(block.scriptPath().getParent());
            return Node.VisitResult.CONTINUE;
        }
        return block.accept((Block.Visitor<Context>) this, ctx);
    }

    @Override
    public Node.VisitResult postVisitBlock(Block block, Context ctx) {
        if (block.kind() == Block.Kind.INVOKE_DIR) {
            ctx.popCwd();
            return Node.VisitResult.CONTINUE;
        }
        return block.acceptAfter((Block.Visitor<Context>) this, ctx);
    }

    @Override
    public Node.VisitResult visitValidation(Validation validation, Context arg) {
        validationId = validation.id();
        return visitAny(validation, arg);
    }

    @Override
    public Node.VisitResult postVisitValidation(Validation validation, Context arg) {
        validationId = "Unknown";
        return postVisitAny(validation, arg);
    }

    @Override
    public Node.VisitResult visitRegex(Validation.Regex regex, Context arg) {
        String pattern = regex.pattern().pattern();
        try (org.mozilla.javascript.Context context = org.mozilla.javascript.Context.enter()) {
            Scriptable scope = context.initStandardObjects();
            context.evaluateString(scope, pattern, "regex", 1, null);
        } catch (Exception e) {
            errors.add(String.format(ERROR_FORMAT, pattern, validationId));
        }
        return Node.VisitResult.CONTINUE;
    }

    /**
     * Get list of errors.
     *
     * @return errors
     */
    List<String> errors() {
        return errors;
    }
}
