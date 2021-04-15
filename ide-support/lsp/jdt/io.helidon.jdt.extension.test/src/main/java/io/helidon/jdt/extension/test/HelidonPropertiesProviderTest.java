/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.jdt.extension.test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.PropertiesManager;
import org.junit.jupiter.api.Test;

import static org.eclipse.lsp4mp.jdt.core.JavaUtils.createJavaProject;
import static org.eclipse.lsp4mp.jdt.core.JavaUtils.getJarPath;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertHints;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertProperties;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.h;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.p;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.vh;

/**
 * Test for HelidonPropertiesProvider.
 */
public class HelidonPropertiesProviderTest extends BasePropertiesManagerTest {

    private static final String HELIDON_COMMON_JAR = getJarPath("test-helidon-dependency.jar");

    /**
     * The the provider.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testPropertiesProvider() throws Exception {
        String[] classpath = {HELIDON_COMMON_JAR};
        IJavaProject project = createJavaProject("test-helidon-common", classpath);
        MicroProfileProjectInfo info = PropertiesManager
                .getInstance()
                .getMicroProfileProjectInfo(
                        project, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES, ClasspathKind.SRC, JDT_UTILS,
                        DocumentFormat.Markdown, new NullProgressMonitor()
                );

        assertProperties(info,
                p(null,
                        "test.config.int.value",
                        "java.lang.Integer",
                        "Integer configuration property for Helidon JDT extension tests",
                        true,
                        null,
                        null,
                        null,
                        0,
                        "1"),
                p(null,
                        "test.config.hint.value",
                        "java.lang.String",
                        "Property that used for hint tests.",
                        true,
                        null,
                        null,
                        null,
                        0,
                        null),
                p(null,
                        "test.config.mime-types",
                        "java.lang.String[]",
                        "Comma-separated list of MIME types for tests.",
                        true,
                        null,
                        null,
                        null,
                        0,
                        Stream.of("text/html",
                                "text/xml",
                                "text/plain",
                                "text/css",
                                "text/javascript",
                                "application/javascript",
                                "application/json",
                                "application/xml")
                              .map(s -> "\"" + s.replace("/", "\\/") + "\"")
                              .collect(Collectors.joining(",")))
        );

        assertHints(info,
                h("test.config.hint.value", null, true, null,
                        vh("hint value 1", "Description for hint value 1.", null),
                        vh("hint value 2", "Description for hint value 2.", null))
        );
    }
}
