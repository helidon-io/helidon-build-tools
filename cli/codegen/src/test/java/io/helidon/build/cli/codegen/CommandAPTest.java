/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.cli.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.helidon.build.cli.codegen.CompilerHelper.JavaSourceFromString;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.Unchecked.unchecked;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static io.helidon.build.common.Strings.read;
import static java.nio.file.Files.list;
import static java.nio.file.Files.readString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link CommandAP}.
 */
class CommandAPTest {

    @SuppressWarnings("SpellCheckingInspection")
    private static final List<String> COMPILER_OPTS = List.of(
            "--release", "17",
            "-implicit:class",
            "-Xlint:unchecked",
            "-Xplugin:file-header",
            "-Werror");

    @Test
    void testCli1() throws IOException {
        e2e("com/acme/cli/TestOptions.java",
                "com/acme/cli/TestCommand1.java",
                "com/acme/cli/TestCommand2.java",
                "com/acme/cli/TestCli1.java");
    }

    @Test
    void testCliWithNestedCommandClasses() throws IOException {
        e2e("com/acme/cli/TestCliNested.java");
    }

    @Test
    void testCommandWithoutExecution() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("CommandWithoutExecution", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class CommandWithoutExecution {
                        
                            @Creator
                            CommandWithoutExecution(@Option.Argument(description = "xxx") String arg) {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.MISSING_COMMAND_EXECUTION));
    }

    @Test
    void testCommandWithoutCreator() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("CommandWithoutCreator", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class CommandWithoutCreator implements CommandExecution {
                        
                            CommandWithoutCreator(@Option.Argument(description = "xxx") String arg) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.MISSING_CREATOR_ANNOTATION));
    }

    @Test
    void testMissingCommandAnnotation() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("MissingCommandAnnotation", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(
                                name = "xxx",
                                description = "xxx",
                                commands = {
                                        MissingCommandAnnotation.Cmd.class,
                                })
                        class MissingCommandAnnotation {
                        
                            static class Cmd implements CommandExecution {
                        
                                @Override
                                public void execute(CommandContext context) throws Exception {
                                }
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.MISSING_COMMAND_ANNOTATION));
    }

    @Test
    void testMissingFragmentAnnotation() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("MissingFragmentAnnotation", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(
                                name = "xxx",
                                description = "xxx",
                                commands = {
                                        MissingFragmentAnnotation.Cmd.class,
                                })
                        class MissingFragmentAnnotation {
                        
                            static class MyOptions {
                        
                                @Creator
                                MyOptions(@Option.KeyValue(name = "name", description = "xxx") String name) {
                        
                                }
                            }
                        
                            @Command(name = "xxx", description = "xxx")
                            static class Cmd implements CommandExecution {
                        
                                @Creator
                                Cmd(@Option.KeyValue(name = "name", description = "xxx") String name, MyOptions options) {
                                }
                        
                                @Override
                                public void execute(CommandContext context) throws Exception {
                                }
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.MISSING_FRAGMENT_ANNOTATION));
    }

    @Test
    void testFragmentDuplicatedOptions() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("FragmentDuplicatedOptions", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(
                                name = "xxx",
                                description = "xxx",
                                commands = {
                                        FragmentDuplicatedOptions.Cmd.class,
                                })
                        class FragmentDuplicatedOptions {
                        
                            @CommandFragment
                            static class MyOptions {
                        
                                @Creator
                                MyOptions(@Option.KeyValue(name = "name", description = "xxx") String name) {
                        
                                }
                            }
                        
                            @Command(name = "xxx", description = "xxx")
                            static class Cmd implements CommandExecution {
                        
                                @Creator
                                Cmd(@Option.KeyValue(name = "name", description = "xxx") String name, MyOptions options) {
                                }
                        
                                @Override
                                public void execute(CommandContext context) throws Exception {
                                }
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.FRAGMENT_OPTION_DUPLICATES));
    }

    @Test
    void testOptionAlreadyDefined() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("OptionAlreadyDefined", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class OptionAlreadyDefined implements CommandExecution {
                        
                            @Creator
                            OptionAlreadyDefined(
                                    @Option.KeyValue(name = "name", description = "xxx") String name1,
                                    @Option.KeyValue(name = "name", description = "xxx") String name2) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.OPTION_ALREADY_DEFINED));
    }

    @Test
    void testInvalidArgumentType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidArgumentType", """
                        package com.acme.cli;
                        
                        import java.util.Date;
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidArgumentType implements CommandExecution {
                        
                            @Creator
                            InvalidArgumentType(@Option.Argument(description = "xxx") Date date) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_ARGUMENT_TYPE));
    }

    @Test
    void testInvalidFlagType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidFlagType", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidFlagType implements CommandExecution {
                        
                            @Creator
                            InvalidFlagType(@Option.Flag(name = "dry-run", description = "xxx") String dryRun) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_FLAG_TYPE));
    }

    @Test
    void testInvalidKeyValueType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidKeyValueType", """
                        package com.acme.cli;
                        
                        import java.util.Date;
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidKeyValueType implements CommandExecution {
                        
                            @Creator
                            InvalidKeyValueType(@Option.KeyValue(name = "date", description = "xxx") Date date) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_KEY_VALUE_TYPE));
    }

    @Test
    void testEnumKeyValue() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EnumKeyValue", """
                        package com.acme.cli;
                        
                        import java.util.Date;
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class EnumKeyValue implements CommandExecution {
                        
                            enum Color { RED, BLUE }
                        
                            @Creator
                            EnumKeyValue(@Option.KeyValue(name = "color", description = "xxx") Color color) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(true));
        assertThat(compiler.diagnostics(), is(empty()));
    }

    @Test
    void testInvalidKeyValuesTypeParam() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidKeyValuesTypeParam", """
                        package com.acme.cli;
                        
                        import java.util.Date;
                        import java.util.Collection;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidKeyValuesTypeParam implements CommandExecution {
                        
                            @Creator
                            InvalidKeyValuesTypeParam(@Option.KeyValues(name = "dates", description = "xxx") Collection<Date> dates) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_KEY_VALUES_TYPE_PARAMETER));
    }

    @Test
    void testInvalidKeyValuesType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidKeyValuesType", """
                        package com.acme.cli;
                        
                        import java.util.Set;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidKeyValuesType implements CommandExecution {
                        
                            @Creator
                            InvalidKeyValuesType(@Option.KeyValues(name = \
                        "names", description = "xxx") Set<String> names) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws \
                        Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_KEY_VALUES_TYPE));
    }

    @Test
    void testEmptyOptionName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyOptionName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class EmptyOptionName implements CommandExecution {
                        
                            @Creator
                            EmptyOptionName(@Option.KeyValue(name = "", description = "xxx") String name) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testInvalidOptionName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidOptionName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class InvalidOptionName implements CommandExecution {
                        
                            @Creator
                            InvalidOptionName(@Option.KeyValue(name = "-opt-", description = "xxx") String name) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testEmptyOptionDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyOptionDescription", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "xxx")
                        class EmptyOptionDescription implements CommandExecution {
                        
                            @Creator
                            EmptyOptionDescription(@Option.KeyValue(name = "xxx", description = "") String name) {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_DESCRIPTION));
    }

    @Test
    void testEmptyCommandName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCommandName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "", description = "xxx")
                        class EmptyCommandName implements CommandExecution {
                        
                            @Creator
                            EmptyCommandName() {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testInvalidCommandName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidCommandName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "-cmd-", description = "xxx")
                        class InvalidCommandName implements CommandExecution {
                        
                            @Creator
                            InvalidCommandName() {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testEmptyCommandDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCommandDescription", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @Command(name = "xxx", description = "")
                        class EmptyCommandDescription implements CommandExecution {
                        
                            @Creator
                            EmptyCommandDescription() {
                            }
                        
                            @Override
                            public void execute(CommandContext context) throws Exception {
                            }
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_DESCRIPTION));
    }

    @Test
    void testEmptyCliName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCliName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(name = "", description = "xxx", commands = {})
                        class EmptyCliName {
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testInvalidCliName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidCliName", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(name = "-cli-", description = "xxx", commands = {})
                        class InvalidCliName {
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_NAME));
    }

    @Test
    void testEmptyCliDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCliDescription", """
                        package com.acme.cli;
                        
                        import io.helidon.build.cli.harness.*;
                        
                        @CommandLineInterface(name = "xxx", description = "", commands = {})
                        class EmptyCliDescription {
                        }"""));
        assertThat(compiler.call(), is(false));
        assertThat(compiler.diagnostics(), contains(Visitor.INVALID_DESCRIPTION));
    }

    private static void e2e(String... resources) throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS, resources);
        assertThat(compiler.call(), is(true));
        Path outputDir = compiler.outputDir();
        try (Stream<Path> stream = list(outputDir.resolve("com/acme/cli"))) {
            stream.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(unchecked(path -> {
                        String resourcePath = outputDir.relativize(path).toString();
                        InputStream inputStream = CommandAPTest.class.getClassLoader().getResourceAsStream(resourcePath);
                        assertThat(inputStream, is(notNullValue()));
                        assertThat(readString(path), is(normalizeNewLines(read(inputStream))));
                    }));
        }
    }
}
