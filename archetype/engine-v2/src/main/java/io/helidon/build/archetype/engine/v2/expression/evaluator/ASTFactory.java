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

package io.helidon.build.archetype.engine.v2.expression.evaluator;

/**
 * Factory for the creation of the new instances of the {@link AbstractSyntaxTree} .
 */
abstract class ASTFactory {

    /**
     * Create the new instances of the {@link AbstractSyntaxTree} from the parsed token.
     *
     * @param token Parsed token.
     * @return AbstractSyntaxTree
     */
    public static AbstractSyntaxTree create(Token token) {
        if (token.type() == Token.Type.BOOLEAN) {
            return new BooleanLiteral(Boolean.valueOf(token.value()));
        }
        if (token.type() == Token.Type.STRING) {
            return new StringLiteral(token.value());
        }
        if (token.type() == Token.Type.ARRAY) {
            return new StringArrayLiteral(token.value());
        }
        if (token.type() == Token.Type.VARIABLE) {
            return new Variable(token.value());
        }

        throw new ParserException("Unknown AbstractSyntaxTree type");
    }
}
