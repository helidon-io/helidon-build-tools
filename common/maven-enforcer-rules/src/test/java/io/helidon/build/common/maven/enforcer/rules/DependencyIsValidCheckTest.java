/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven.enforcer.rules;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DependencyIsValidCheckTest {
    static final DependencyIsValidCheck dependencyIsValidCheck = DependencyIsValidCheck.create();

    @Test
    void testServletIsNotValid() {
        assertThat(dependencyIsValidCheck.apply("javax.servlet:javax.servlet-api:3.1.0"),
                   is(false));
        assertThat(dependencyIsValidCheck.apply("jakarta.servlet:"),
                   is(false));
        assertThat(dependencyIsValidCheck.apply("jakarta.servlet"),
                   is(false));
    }

    @Test
    void testJakartaAnnotations() {
        assertThat(dependencyIsValidCheck.apply("jakarta.annotation:jakarta.annotation-api:2.1.1"),
                   is(true));
        assertThat(dependencyIsValidCheck.apply("jakarta.annotation:jakarta.annotation-api:1.0.0"),
                   is(false));
    }

    @Test
    void testJavaxCryptoIsValid() {
        assertThat(dependencyIsValidCheck.apply("javax.crypto:1"),
                   is(true));
    }

    @Test
    void testJavaxInjectIsNotValid() {
        assertThat(dependencyIsValidCheck.apply("javax.inject:javax.inject:1"),
                   is(false));
    }

}
