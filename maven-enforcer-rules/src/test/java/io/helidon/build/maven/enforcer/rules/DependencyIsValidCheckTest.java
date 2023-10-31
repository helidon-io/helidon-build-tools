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

package io.helidon.build.maven.enforcer.rules;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyIsValidCheckTest {
    DependencyIsValidCheck dependencyIsValidCheckForJakarta =
            new DependencyIsValidCheck(HelidonDependenciesRule.JAKARTA, List.of());
    DependencyIsValidCheck dependencyIsValidCheckForJavax =
            new DependencyIsValidCheck(HelidonDependenciesRule.JAVAX, List.of());

    @Test
    void testServletIsNotValid() {
        assertThat(dependencyIsValidCheckForJakarta.apply("javax.servlet:javax.servlet-api:2.1.0"),
                   is(false));
        assertThat(dependencyIsValidCheckForJavax.apply("javax.servlet:javax.servlet-api:2.1.0"),
                   is(false));
    }

    @Test
    void testServletIsValidWithExplicitExclusion() {
        dependencyIsValidCheckForJakarta =
                new DependencyIsValidCheck(HelidonDependenciesRule.JAKARTA, List.of(Pattern.compile("javax.servlet.*")));
        assertThat(dependencyIsValidCheckForJakarta.apply("javax.servlet:javax.servlet-api:2.1.0"),
                   is(true));
        assertThat(dependencyIsValidCheckForJakarta.apply("jakarta.servlet:jakarta.servlet-api:2.1.0"),
                   is(false));

        dependencyIsValidCheckForJavax =
                new DependencyIsValidCheck(HelidonDependenciesRule.JAVAX, List.of(Pattern.compile("javax.servlet.*")));
        assertThat(dependencyIsValidCheckForJakarta.apply("javax.servlet:javax.servlet-api:2.1.0"),
                   is(true));
        assertThat(dependencyIsValidCheckForJakarta.apply("jakarta.servlet:jakarta.servlet-api:2.1.0"),
                   is(false));
    }

    @Test
    void testAnnotations() {
        assertThat(dependencyIsValidCheckForJakarta.apply("jakarta.annotation:jakarta.annotation-api:2.1.1"),
                   is(true));
        assertThat(dependencyIsValidCheckForJakarta.apply("jakarta.annotation:jakarta.annotation-api:1.0.0"),
                   is(false));

        assertThat(dependencyIsValidCheckForJavax.apply("jakarta.annotation:jakarta.annotation-api:2.1.1"),
                   is(false));
        assertThat(dependencyIsValidCheckForJavax.apply("jakarta.annotation:jakarta.annotation-api:1.0.0"),
                   is(true));
    }

    @Test
    void testJavaxCryptoIsValid() {
        assertThat(dependencyIsValidCheckForJakarta.apply("javax.crypto:javax.crypto:1"),
                   is(true));
    }

    @Test
    void testJavaxInject() {
        assertThat(dependencyIsValidCheckForJakarta.apply("javax.inject:javax.inject:1"),
                   is(false));
        assertThat(dependencyIsValidCheckForJavax.apply("javax.inject:javax.inject:1"),
                   is(true));
    }

    @Test
    void testArtifactApply() {
        DependencyIsValidCheck dependencyIsValidCheckForJakarta =
                new DependencyIsValidCheck(HelidonDependenciesRule.JAKARTA,
                                           List.of(Pattern.compile("jakarta.servlet:jakarta.servlet-api.*")));
        Artifact artifact = new DefaultArtifact("jakarta.servlet",
                                                "jakarta.servlet-api",
                                                "2.1.0",
                                                "",
                                                "",
                                                "",
                                                null);
        assertThat(dependencyIsValidCheckForJakarta.apply(artifact), is((true)));
    }

    @Test
    void testValidate() {
        ViolationException ve = assertThrows(ViolationException.class, () ->
                dependencyIsValidCheckForJakarta.validate("javax.inject:javax.inject:1",
                                                "javax.crypto:javax.crypto:1",
                                                "javax.servlet:javax.servlet-api:2.1.0",
                                                "other.servlet:other.servlet:1"));
        assertThat(ve.getMessage(),
                   startsWith("Bad dependencies spotted (review with mvn dependency:tree): "));
        assertThat(ve.violations(),
                   contains("javax.inject:javax.inject:1", "javax.servlet:javax.servlet-api:2.1.0"));
    }

}
