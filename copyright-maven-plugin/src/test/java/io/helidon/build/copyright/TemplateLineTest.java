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

package io.helidon.build.copyright;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

class TemplateLineTest {
    @Test
    void testCopyrightLine() {
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "(c) YYYY Oracle and/or its affiliates.";
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(0);

        Optional<String> validate = templateLine.validate("", "2021", 177);
        assertThat(validate, not(Optional.empty()));
        String message = validate.get();
        assertThat(message, startsWith("177: "));

        validate = templateLine.validate("Some test", "2021", 177);
        assertThat(validate, not(Optional.empty()));
        message = validate.get();
        assertThat(message, startsWith("177: "));

        validate = templateLine.validate("(c) 2020 Oracle and/or its affiliates.", "2021", 177);
        assertThat(validate, not(Optional.empty()));
        message = validate.get();
        assertThat(message, startsWith("177: "));
        assertThat(message, containsString("2020"));
        assertThat(message, containsString("2020, 2021"));

        validate = templateLine.validate("(c) 2020, 2021 Oracle and/or its affiliates.", "2021", 177);
        assertThat(validate, is(Optional.empty()));

        validatorConfig = Validator.ValidatorConfig.builder()
                .checkFormatOnly(true)
                .build();
        templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(0);
        validate = templateLine.validate("(c) 2020 Oracle and/or its affiliates.", "2021", 177);
        assertThat(validate, is(Optional.empty()));

    }

    @Test
    void testTextLine() {
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "  Cogito ergo sum";
        // first line is always copyright, unless explicitly defined
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(1);

        Optional<String> validate = templateLine.validate("", "2021", 177);
        assertThat(validate, not(Optional.empty()));
        String message = validate.get();
        assertThat(message, startsWith("177: "));

        validate = templateLine.validate("\t", "2021", 177);
        assertThat(validate, not(Optional.empty()));
        message = validate.get();
        assertThat(message, startsWith("177: "));

        validate = templateLine.validate("Cogito ergo sum", "2021", 147);
        assertThat(validate, not(Optional.empty()));
        message = validate.get();
        assertThat(message, startsWith("147: "));

        validate = templateLine.validate("Copyright", "2021", 147);
        assertThat(validate, not(Optional.empty()));
        message = validate.get();
        assertThat(message, startsWith("147: "));

        validate = templateLine.validate("  Cogito ergo sum", "2021", 147);
        assertThat(validate, is(Optional.empty()));
    }

    @Test
    void testBlankLine() {
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "\t ";
        // first line is always copyright, unless explicitly defined
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(1);

        Optional<String> validate = templateLine.validate("", "2021", 1);
        assertThat(validate, is(Optional.empty()));

        validate = templateLine.validate("\t", "2021", 0);
        assertThat(validate, is(Optional.empty()));

        validate = templateLine.validate("Copyright", "2021", 147);
        assertThat(validate, not(Optional.empty()));

        String message = validate.get();
        assertThat(message, startsWith("147: "));
    }
}