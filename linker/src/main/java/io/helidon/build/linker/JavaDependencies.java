/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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

package io.helidon.build.linker;

import java.io.ByteArrayOutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.InputStreams.toPrintStream;
import static io.helidon.build.linker.JavaRuntime.CURRENT_JDK;

/**
 * Collects Java module dependencies for a set of jars.
 */
public final class JavaDependencies {

    private final ToolProvider jdeps = ToolProvider.findFirst("jdeps")
            .orElseThrow(() -> new IllegalStateException("jdeps not found"));

    private JavaDependencies() {
    }

    /**
     * Collect the dependencies of the given jars on the given Java Runtime.
     *
     * @param jars The jars.
     * @return The module names.
     */
    public static Set<String> collect(List<Jar> jars) {
        return new JavaDependencies().collect0(jars);
    }

    private Set<String> collect0(List<Jar> jars) {
        Set<String> dependencies = new HashSet<>();
        for (Jar jar : jars) {
            ModuleDescriptor descriptor = jar.moduleDescriptor();
            if (descriptor != null) {
                Log.info("  Checking module %s", descriptor.name());
                for (ModuleDescriptor.Requires require : descriptor.requires()) {
                    String name = require.name();
                    if (CURRENT_JDK.moduleNames().contains(name)) {
                        dependencies.add(name);
                    }
                }
            } else {
                String out = jdeps(jar);
                List<String> lines = out.lines().collect(Collectors.toList());
                for (String line : lines) {
                    if (!line.isBlank()) {
                        String parsed = parseLine(jar, line.trim());
                        if (parsed != null && CURRENT_JDK.moduleNames().contains(parsed)) {
                            dependencies.add(parsed);
                        }
                    }
                }
            }
        }

        Set<String> closure = new TreeSet<>();
        Deque<String> stack = new ArrayDeque<>(dependencies);
        while (!stack.isEmpty()) {
            String name = stack.pop();
            if (closure.add(name)) {
                ModuleDescriptor moduleDescriptor = CURRENT_JDK.module(name);
                for (ModuleDescriptor.Requires require : moduleDescriptor.requires()) {
                    stack.push(require.name());
                }
            }
        }
        return closure;
    }

    private String jdeps(Jar jar) {
        List<String> args = new ArrayList<>();
        if (jar.isMultiRelease()) {
            args.add("--multi-release");
            args.add(String.valueOf(CURRENT_JDK.version().feature()));
        }
        if (CURRENT_JDK.jdepsRequiresMissingDeps()) {
            args.add("--ignore-missing-deps");
        }
        args.add("--list-deps");
        args.add(jar.path().toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int result = jdeps.run(toPrintStream(out, false), System.err, args.toArray(new String[0]));
        if (result != 0) {
            throw new RuntimeException("Could not collect dependencies of " + jar);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String parseLine(Jar jar, String line) {
        if (line.contains("split package")) {
            if (line.contains("javax.annotation") || line.contains("javax.activation")) {
                // known split packages;
                return null;
            }
            Log.warn(jar + " -> " + line);
            return null;
        } else if (line.contains("not found")) {
            Log.debug(jar + " -> " + line);
            return null;
        } else if (line.contains("unnamed module")) {
            Log.debug(jar + " -> " + line);
            return null;
        } else if (line.contains("jdk8internals") || line.contains("JDK removed internal API")) {
            Log.debug(jar + " -> " + line);
            return null;
        } else if (!line.contains(":") && line.contains("/")) {
            return parseLine(jar, line.split("/")[0]);
        } else if ("java.xml.ws.annotation".equals(line)) {
            throw new IllegalStateException("Unhandled dependency: " + line);
        } else {
            return line;
        }
    }
}
