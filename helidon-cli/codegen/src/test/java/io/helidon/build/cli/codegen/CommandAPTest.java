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
package io.helidon.build.cli.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.Diagnostic.Kind;

import io.helidon.build.cli.codegen.CompilerHelper.JavaSourceFromString;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.codegen.Unchecked.unchecked;
import static io.helidon.build.common.Strings.normalizeNewLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link CommandAP}.
 */
class CommandAPTest {

    private static final List<String> COMPILER_OPTS = List.of(
            "-implicit:class",
            "-Xlint:unchecked",
            "-Xplugin:file-header",
            "-Werror");

    @Test
    public void testCli1() throws IOException {
        e2e("com/acme/cli/TestOptions.java",
                "com/acme/cli/TestCommand1.java",
                "com/acme/cli/TestCommand2.java",
                "com/acme/cli/TestCli1.java");
    }

    @Test
    public void testCliWithNestedCommandClasses() throws IOException {
        e2e("com/acme/cli/TestCliNested.java");
    }

    @Test
    public void testCommandWithoutExecution() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("CommandWithoutExecution", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class CommandWithoutExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    CommandWithoutExecution(@Option.Argument(description = \"xxx\") String arg) {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.MISSING_COMMAND_EXECUTION));
    }

    @Test
    public void testCommandWithoutCreator() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("CommandWithoutCreator", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class CommandWithoutCreator implements CommandExecution {\n"
                        + "\n"
                        + "    CommandWithoutCreator(@Option.Argument(description = \"xxx\") String arg) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.MISSING_CREATOR_ANNOTATION));
    }

    @Test
    public void testMissingCommandAnnotation() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("MissingCommandAnnotation", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(\n"
                        + "        name = \"xxx\",\n"
                        + "        description = \"xxx\",\n"
                        + "        commands = {\n"
                        + "                MissingCommandAnnotation.Cmd.class,\n"
                        + "        })\n"
                        + "class MissingCommandAnnotation {\n"
                        + "\n"
                        + "    static class Cmd implements CommandExecution {\n"
                        + "\n"
                        + "        @Override\n"
                        + "        public void execute(CommandContext context) throws Exception {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.MISSING_COMMAND_ANNOTATION));
    }

    @Test
    public void testMissingFragmentAnnotation() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("MissingFragmentAnnotation", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(\n"
                        + "        name = \"xxx\",\n"
                        + "        description = \"xxx\",\n"
                        + "        commands = {\n"
                        + "                MissingFragmentAnnotation.Cmd.class,\n"
                        + "        })\n"
                        + "class MissingFragmentAnnotation {\n"
                        + "\n"
                        + "    static class MyOptions {\n"
                        + "\n"
                        + "        @Creator\n"
                        + "        MyOptions(@Option.KeyValue(name = \"name\", description = \"xxx\") String name) {\n"
                        + "\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    @Command(name = \"xxx\", description = \"xxx\")\n"
                        + "    static class Cmd implements CommandExecution {\n"
                        + "\n"
                        + "        @Creator\n"
                        + "        Cmd(@Option.KeyValue(name = \"name\", description = \"xxx\") String name, MyOptions options) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Override\n"
                        + "        public void execute(CommandContext context) throws Exception {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.MISSING_FRAGMENT_ANNOTATION));
    }

    @Test
    public void testFragmentDuplicatedOptions() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("FragmentDuplicatedOptions", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(\n"
                        + "        name = \"xxx\",\n"
                        + "        description = \"xxx\",\n"
                        + "        commands = {\n"
                        + "                FragmentDuplicatedOptions.Cmd.class,\n"
                        + "        })\n"
                        + "class FragmentDuplicatedOptions {\n"
                        + "\n"
                        + "    @CommandFragment\n"
                        + "    static class MyOptions {\n"
                        + "\n"
                        + "        @Creator\n"
                        + "        MyOptions(@Option.KeyValue(name = \"name\", description = \"xxx\") String name) {\n"
                        + "\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    @Command(name = \"xxx\", description = \"xxx\")\n"
                        + "    static class Cmd implements CommandExecution {\n"
                        + "\n"
                        + "        @Creator\n"
                        + "        Cmd(@Option.KeyValue(name = \"name\", description = \"xxx\") String name, MyOptions options) {\n"
                        + "        }\n"
                        + "\n"
                        + "        @Override\n"
                        + "        public void execute(CommandContext context) throws Exception {\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.FRAGMENT_OPTION_DUPLICATES));
    }

    @Test
    public void testOptionAlreadyDefined() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("OptionAlreadyDefined", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class OptionAlreadyDefined implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    OptionAlreadyDefined(\n"
                        + "            @Option.KeyValue(name = \"name\", description = \"xxx\") String name1,\n"
                        + "            @Option.KeyValue(name = \"name\", description = \"xxx\") String name2) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.OPTION_ALREADY_DEFINED));
    }

    @Test
    public void testInvalidArgumentType() throws IOException {
            CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                    new JavaSourceFromString("com/acme/cli/InvalidArgumentType.java", ""
                            + "package com.acme.cli;\n"
                            + "\n"
                            + "import java.util.Date;\n"
                            + "\n"
                            + "import io.helidon.build.cli.harness.*;\n"
                            + "\n"
                            + "@Command(name = \"xxx\", description = \"xxx\")\n"
                            + "class InvalidArgumentType implements CommandExecution {\n"
                            + "\n"
                            + "    @Creator\n"
                            + "    InvalidArgumentType(@Option.Argument(description = \"xxx\") Date date) {\n"
                            + "    }\n"
                            + "\n"
                            + "    @Override\n"
                            + "    public void execute(CommandContext context) throws Exception {\n"
                            + "    }\n"
                            + "}"));
            assertThat(compiler.call(true), is(false));
            assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_ARGUMENT_TYPE));
    }

    @Test
    public void testInvalidFlagType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("com/acme/cli/InvalidFlagType.java", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class InvalidFlagType implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidFlagType(@Option.Flag(name = \"dry-run\", description = \"xxx\") String dryRun) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_FLAG_TYPE));
    }

    @Test
    public void testInvalidKeyValueType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("com/acme/cli/InvalidKeyValueType.java", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import java.util.Date;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class InvalidKeyValueType implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidKeyValueType(@Option.KeyValue(name = \"date\", description = \"xxx\") Date date) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_KEY_VALUE_TYPE));
    }

    @Test
    public void testEnumKeyValue() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("com/acme/cli/EnumKeyValue.java", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import java.util.Date;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class EnumKeyValue implements CommandExecution {\n"
                        + "\n"
                        + "    enum Color { RED, BLUE }\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    EnumKeyValue(@Option.KeyValue(name = \"color\", description = \"xxx\") Color color) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(true));
        assertThat(compiler.diagnostics(Kind.ERROR), is(empty()));
    }

    @Test
    public void testInvalidKeyValuesTypeParam() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidKeyValuesTypeParam", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import java.util.Date;\n"
                        + "import java.util.Collection;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class InvalidKeyValuesTypeParam implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidKeyValuesTypeParam(@Option.KeyValues(name = \"dates\", description = \"xxx\") Collection<Date> dates) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_KEY_VALUES_TYPE_PARAMETER));
    }

    @Test
    public void testInvalidKeyValuesType() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidKeyValuesType", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import java.util.Set;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class InvalidKeyValuesType implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidKeyValuesType(@Option.KeyValues(name = \"names\", description = \"xxx\") Set<String> names) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_KEY_VALUES_TYPE));
    }

    @Test
    public void testEmptyOptionName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyOptionName", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class EmptyOptionName implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    EmptyOptionName(@Option.KeyValue(name = \"\", description = \"xxx\") String name) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testInvalidOptionName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidOptionName", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class InvalidOptionName implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidOptionName(@Option.KeyValue(name = \"-opt-\", description = \"xxx\") String name) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testEmptyOptionDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyOptionDescription", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"xxx\")\n"
                        + "class EmptyOptionDescription implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    EmptyOptionDescription(@Option.KeyValue(name = \"xxx\", description = \"\") String name) {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_DESCRIPTION));
    }

    @Test
    public void testEmptyCommandName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCommandName", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"\", description = \"xxx\")\n"
                        + "class EmptyCommandName implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    EmptyCommandName() {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testInvalidCommandName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("InvalidCommandName", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"-cmd-\", description = \"xxx\")\n"
                        + "class InvalidCommandName implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    InvalidCommandName() {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testEmptyCommandDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCommandDescription", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@Command(name = \"xxx\", description = \"\")\n"
                        + "class EmptyCommandDescription implements CommandExecution {\n"
                        + "\n"
                        + "    @Creator\n"
                        + "    EmptyCommandDescription() {\n"
                        + "    }\n"
                        + "\n"
                        + "    @Override\n"
                        + "    public void execute(CommandContext context) throws Exception {\n"
                        + "    }\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_DESCRIPTION));
    }

    @Test
    public void testEmptyCliName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("EmptyCliName", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(name = \"\", description = \"xxx\", commands = {})\n"
                        + "class EmptyCliName {\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testInvalidCliName() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("com/acme/cli/InvalidCliName.java", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(name = \"-cli-\", description = \"xxx\", commands = {})\n"
                        + "class InvalidCliName {\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_NAME));
    }

    @Test
    public void testEmptyCliDescription() throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS,
                new JavaSourceFromString("com/acme/cli/EmptyCliDescription.java", ""
                        + "package com.acme.cli;\n"
                        + "\n"
                        + "import io.helidon.build.cli.harness.*;\n"
                        + "\n"
                        + "@CommandLineInterface(name = \"xxx\", description = \"\", commands = {})\n"
                        + "class EmptyCliDescription {\n"
                        + "}"));
        assertThat(compiler.call(true), is(false));
        assertThat(compiler.diagnostics(Kind.ERROR), contains(Visitor.INVALID_DESCRIPTION));
    }

    private static void e2e(String... resources) throws IOException {
        CompilerHelper compiler = new CompilerHelper(new CommandAP(), COMPILER_OPTS, resources);
        assertThat(compiler.call(true), is(true));
        Path outputDir = compiler.outputDir();
        Files.list(outputDir.resolve("com/acme/cli"))
             .filter(path -> path.getFileName().toString().endsWith(".java"))
             .forEach(unchecked(path -> {
                 String resourcePath = outputDir.relativize(path).toString();
                 InputStream inputStream = CommandAPTest.class.getClassLoader().getResourceAsStream(resourcePath);
                 assertThat(inputStream, is(notNullValue()));
                 assertThat(Files.readString(path),
                         is(normalizeNewLines(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))));
             }));
    }
}
