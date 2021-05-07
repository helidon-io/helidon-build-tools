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

package io.helidon.build.maven.enforcer.copyright;

import java.util.List;
import java.util.Random;

import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.RuleFailure;
import io.helidon.build.maven.enforcer.RuleFailureException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateLineTest {
    private static final Random RANDOM = new Random();

    @Test
    void testAllRightsReserved() {
        FileRequest file = FileRequest.create("UnitTest.java", "2021");
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .currentYear("2021")
                .build();
        String templateLineString = "Copyright (c) YYYY Oracle and/or its affiliates.";
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(0);

        shouldFail(templateLine,
                   file,
                   "Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.");
    }

    @Test
    void testCopyrightLine() {
        FileRequest file = FileRequest.create("UnitTest.java", "2021");
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "(c) YYYY Oracle and/or its affiliates.";
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(0);

        shouldFail(templateLine, file, "");
        shouldFail(templateLine, file, "Some test");
        shouldFail(templateLine, file, "(c) 2020 Oracle and/or its affiliates.", "2020, 2021");

        // this should succeed
        templateLine.validate(file, "(c) 2020, 2021 Oracle and/or its affiliates.", 177);

        validatorConfig = Validator.ValidatorConfig.builder()
                .checkFormatOnly(true)
                .build();
        templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(0);

        // this should succeed
        templateLine.validate(file, "(c) 2020 Oracle and/or its affiliates.", 177);
    }

    @Test
    void testTextLine() {
        FileRequest file = FileRequest.create("UnitTest.java", "2021");
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "  Cogito ergo sum";
        // first line is always copyright, unless explicitly defined
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(1);

        shouldFail(templateLine, file, "");
        shouldFail(templateLine, file, "\t");
        shouldFail(templateLine, file, "Cogito ergo sum");
        shouldFail(templateLine, file, "Copyright");

        // success
        templateLine.validate(file, "  Cogito ergo sum", 147);
    }

    @Test
    void testBlankLine() {
        Validator.ValidatorConfig validatorConfig = Validator.ValidatorConfig.builder()
                .build();
        String templateLineString = "\t ";
        // first line is always copyright, unless explicitly defined
        TemplateLine templateLine = TemplateLine.parseTemplate(validatorConfig, List.of(templateLineString))
                .get(1);

        FileRequest file = FileRequest.create("UnitTest.java", "2021");
        templateLine.validate(file, "", 1);
        templateLine.validate(file, "\t", 0);

        shouldFail(templateLine, file, "Copyright");
    }

    private void shouldFail(TemplateLine templateLine,
                            FileRequest file,
                            String actualLine,
                            String... mustContain) {

        int lineNumber = RANDOM.nextInt(278) + 1;

        RuleFailureException fe = assertThrows(RuleFailureException.class,
                                               () -> templateLine.validate(file,
                                                                       actualLine,
                                                                           lineNumber));

        RuleFailure failure = fe.failure();

        assertThat(failure.line(), is(lineNumber));
        String message = failure.message();

        for (String contains : mustContain) {
            assertThat(message, containsString(contains));
        }
    }
}