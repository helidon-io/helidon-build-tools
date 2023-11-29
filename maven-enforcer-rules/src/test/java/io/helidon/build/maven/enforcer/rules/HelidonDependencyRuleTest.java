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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.enforcer.rules.HelidonDependenciesRule.checkNamespace;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HelidonDependencyRuleTest {

    @Test
    void testCheckNamespace() {
        assertThat(checkNamespace(null),
                   equalTo(HelidonDependenciesRule.JAKARTA));
        assertThat(checkNamespace(""),
                   equalTo(HelidonDependenciesRule.JAKARTA));
        assertThat(checkNamespace("jakarta"),
                   equalTo(HelidonDependenciesRule.JAKARTA));
        assertThat(checkNamespace("javax"),
                   equalTo(HelidonDependenciesRule.JAVAX));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> checkNamespace("java"));
        assertThat(e.getMessage(),
                   equalTo("The namespace 'java' is invalid. Only valid namespace names are: 'jakarta' and 'javax'."));
    }

    @Test
    void emptyGavExceptions() throws Exception {
        MavenProject project = new MavenProject();
        Set<Artifact> artifacts =
                Stream.of("javax.inject:javax.inject:1",
                          "javax.crypto:javax.crypto:1",
                          "javax.servlet:javax.servlet-api:2.1.0",
                          "other.servlet:other.servlet:1")
                        .map(DependencyIsValidCheck::toArtifact)
                        .collect(Collectors.toSet());
        project.setArtifacts(artifacts);

        HelidonDependenciesRule rule =
                new HelidonDependenciesRule();
        rule.setLog(new TestingLogger());
        rule.setProject(project);

        ViolationException e = assertThrows(ViolationException.class, rule::execute);
        assertThat(e.violations().size(), is(2));

        List<String> allowedList = new ArrayList<>();
        allowedList.add("");
        rule.setExcludedGavRegExs(allowedList);
        e = assertThrows(ViolationException.class, rule::execute);
        assertThat(e.violations().size(), is(2));
    }

    static class TestingLogger implements EnforcerLogger {
        static final Logger LOGGER = Logger.getLogger(TestingLogger.class.getName());

        @Override
        public void warnOrError(CharSequence charSequence) {
            LOGGER.warning(charSequence.toString());
        }

        @Override
        public void warnOrError(Supplier<CharSequence> supplier) {
            warnOrError(supplier.get());
        }

        @Override
        public boolean isDebugEnabled() {
            return LOGGER.isLoggable(Level.FINE);
        }

        @Override
        public void debug(CharSequence charSequence) {
            LOGGER.log(Level.FINE, charSequence.toString());
        }

        @Override
        public void debug(Supplier<CharSequence> supplier) {
            debug(supplier.get());
        }

        @Override
        public boolean isInfoEnabled() {
            return LOGGER.isLoggable(Level.INFO);
        }

        @Override
        public void info(CharSequence charSequence) {
            LOGGER.log(Level.INFO, charSequence.toString());
        }

        @Override
        public void info(Supplier<CharSequence> supplier) {
            info(supplier.get());
        }

        @Override
        public boolean isWarnEnabled() {
            return LOGGER.isLoggable(Level.WARNING);
        }

        @Override
        public void warn(CharSequence charSequence) {
            LOGGER.log(Level.WARNING, charSequence.toString());
        }

        @Override
        public void warn(Supplier<CharSequence> supplier) {
            warn(supplier.get());
        }

        @Override
        public boolean isErrorEnabled() {
            return LOGGER.isLoggable(Level.SEVERE);
        }

        @Override
        public void error(CharSequence charSequence) {
            LOGGER.log(Level.SEVERE, charSequence.toString());
        }

        @Override
        public void error(Supplier<CharSequence> supplier) {
            error(supplier.get());
        }
    }

}
