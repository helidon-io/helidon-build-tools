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

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.DynamicValue;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Validation;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.context.Context;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Validator unit tests.
 */
public class ValidatorTest {

    private static final Node.BuilderInfo BUILDER_INFO = Node.BuilderInfo.of(
            ScriptLoader.create(),
            Path.of("test.xml"),
            null);

    @Test
    void testCompatibleSingleRegex() {
        RegexValidator validator = new RegexValidator();
        Block block = validations(
                validation("validation", "description", regex("/abc/")));

        Walker.walk(validator, block, Context.create());
        assertThat(validator.errors().size(), is(0));
    }

    @Test
    void testCompatibleMultiRegex() {
        RegexValidator validator = new RegexValidator();
        Block block = validations(
                validation("validation", "description",
                        regex("/abc/"),
                        regex("/\\([^\\)]+\\)/g"),
                        regex("/^(?=(?:.*[A-Z]){2})(?=(?:.*[a-z]){2})(?=.*\\d).{5,15}$/")),
                validation("validation1", "description1",
                        regex("/a/"),
                        regex("/b/"))
        );

        Walker.walk(validator, block, Context.create());
        assertThat(validator.errors().size(), is(0));
    }

    @Test
    void testIncompatibleSingleRegex() {
        RegexValidator validator = new RegexValidator();
        Block block = validations(
                validation("validation", "description", regex("[abc]")));

        Walker.walk(validator, block, Context.create());
        List<String> errors = validator.errors();
        assertThat(errors.size(), is(1));
        assertThat(errors.get(0), is(errorMessage("[abc]", "validation")));
    }

    @Test
    void testIncompatibleMultiRegex() {
        RegexValidator validator = new RegexValidator();
        Block block = validations(
                validation("validation", "description",
                        regex("[abc]"),
                        regex("foo"),
                        regex("/^(?=(?:.*[A-Z]){2})(?=(?:.*[a-z]){2})(?=.*\\d).{5,15}$/")),
                validation("validation1", "description1",
                        regex("/a/"),
                        regex("bar")));

        Walker.walk(validator, block, Context.create());
        List<String> errors = validator.errors();
        assertThat(errors.size(), is(3));
        assertThat(errors.get(0), is(errorMessage("[abc]", "validation")));
        assertThat(errors.get(1), is(errorMessage("foo", "validation")));
        assertThat(errors.get(2), is(errorMessage("bar", "validation1")));
    }

    private String errorMessage(String regex, String id) {
        return String.format("Regular expression '%s' at validation '%s' is not JavaScript compatible",
                regex, id);
    }

    /**
     * Create a validations block builder.
     *
     * @param children      nested children
     * @return block builder
     */
    private Block validations(Block.Builder... children) {
        Block.Builder builder = Block.builder(BUILDER_INFO, Block.Kind.VALIDATIONS);
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder.build();
    }

    /**
     * Create a validation block builder.
     *
     * @param id            input id
     * @param description   description
     * @param children      nested children
     * @return block builder
     */
    private Block.Builder validation(String id, String description, Block.Builder... children) {
        Block.Builder builder = Validation.builder(BUILDER_INFO, Block.Kind.VALIDATION)
                .attributes(validationAttributes(id, description));
        for (Block.Builder child : children) {
            builder.addChild(child);
        }
        return builder;
    }

    /**
     * Create a regex block builder.
     *
     * @param value     input value
     * @return block builder
     */
    private Block.Builder regex(String value) {
        return Validation.builder(BUILDER_INFO, Block.Kind.REGEX)
                .value(value);
    }

    private Map<String, Value> validationAttributes(String id, String description) {
        Map<String, Value> attributes = new HashMap<>();
        attributes.put("id", DynamicValue.create(id));
        attributes.put("description", DynamicValue.create(description));
        return attributes;
    }
}
