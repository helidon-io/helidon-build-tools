/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build.maven;

import java.nio.file.Path;

import io.helidon.build.test.TestFiles;
import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectSupplier;
import io.helidon.dev.build.TestMonitor;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link DefaultHelidonProjectSupplier}.
 */
class DefaultHelidonProjectSupplierTest {

    @Test
    void testGet() throws Exception {
        final Path projectDir = TestFiles.helidonSeProject();
        final ProjectSupplier supplier = new DefaultHelidonProjectSupplier();
        final TestMonitor monitor = new TestMonitor(false, 1);
        final Project project = supplier.get(projectDir, false, monitor.stdOutConsumer(), monitor.stdErrConsumer());
        assertThat(project, is(not(nullValue())));
        assertThat(project.components().size(), is(2));
        assertThat(project.classpath().size(), is(greaterThan(2)));
    }
}
