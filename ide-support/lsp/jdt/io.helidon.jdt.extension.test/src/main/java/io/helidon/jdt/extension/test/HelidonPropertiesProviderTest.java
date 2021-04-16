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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.lsp4mp.commons.ClasspathKind;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.PropertiesManager;
import org.junit.jupiter.api.Test;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertHints;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertProperties;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.h;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.p;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.vh;

/**
 * Test for HelidonPropertiesProvider.
 */
class HelidonPropertiesProviderTest extends BasePropertiesManagerTest {

    /**
     * The the provider.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testPropertiesProvider() throws Exception {
        IJavaProject project = createJavaProject("test-helidon-common", "test-helidon-module.jar");
        MicroProfileProjectInfo info = PropertiesManager
                .getInstance()
                .getMicroProfileProjectInfo(
                        project,
                        MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES,
                        ClasspathKind.SRC,
                        JDT_UTILS,
                        DocumentFormat.Markdown,
                        new NullProgressMonitor());

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

    public static IJavaProject createJavaProject(String projectName, String... jars) throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            File dir = new File("target", "workingProjects");
            FileUtils.forceMkdir(dir);
            IPath projectLocation = new Path(dir.getAbsolutePath()).append(projectName);
            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
            description.setLocation(projectLocation);
            IProgressMonitor monitor = new NullProgressMonitor();
            project.create(description, monitor);
            project.open(monitor);
            description = project.getDescription();
            description.setNatureIds(new String[] {JavaCore.NATURE_ID});
            project.setDescription(description, monitor);
            IJavaProject javaProject = JavaCore.create(project);
            List<IClasspathEntry> classpath = new ArrayList<>();
            if (jars != null) {
                for (String jar : jars) {
                    IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(new File("target", jar).getAbsolutePath());
                    IClasspathEntry libClasspath = JavaCore.newLibraryEntry(root.getPath(), null, null);
                    classpath.add(libClasspath);
                }
            }
            javaProject.setRawClasspath(classpath.toArray(new IClasspathEntry[0]), monitor);
        }
        return JavaCore.create(project);
    }
}
