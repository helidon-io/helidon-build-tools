/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.cli.harness;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import io.helidon.build.cli.harness.CommandContext.ExitStatus;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.util.Log;

import io.helidon.build.util.OSType;
import org.junit.jupiter.api.Test;

import static io.helidon.build.util.Style.strip;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test the built-in commands and options.
 */
public class ExecTest {

    static final String EOL = OSType.currentOS() == OSType.Windows ? "\r\n" : "\n";
    static final CommandRegistry REGISTRY = new TestCommandRegistry();
    static final String CLI_USAGE = resourceAsString("cli-usage.txt");
    static final String HELP_CMD_HELP = resourceAsString("help-cmd-help.txt");
    static final String SIMPLE_CMD_HELP = resourceAsString("simple-cmd-help.txt");
    static final CLIDefinition TEST_CLI = CLIDefinition.create("test-cli", "A test cli");

    static CommandContext context() {
        return CommandContext.create(REGISTRY, TEST_CLI);
    }

    static String resourceAsString(String name) {
        InputStream is = ExecTest.class.getResourceAsStream(name);
        try {
            String str = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (OSType.currentOS() == OSType.Windows) {
                str = str.replaceAll("\n", "\r\n");
            }
            return str;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static String exec(CommandContext context, String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stdout = System.out;
        try {
            System.setOut(new PrintStream(baos));
            CommandRunner.execute(context, args);
        } finally {
            System.setOut(stdout);
        }
        return strip(new String(baos.toByteArray(), StandardCharsets.UTF_8));
    }

    static String exec(String... args) {
        return exec(context(), args);
    }

    @Test
    public void testUsage() {
        assertThat(exec("--help"), is(CLI_USAGE));
        assertThat(exec("help"), is(CLI_USAGE));
        assertThat(exec(), is(CLI_USAGE));
    }

    @Test
    public void testHelp() {
        assertThat(exec("help", "--help"), is(HELP_CMD_HELP));
        assertThat(exec("help", "help"), is(HELP_CMD_HELP));
        assertThat(exec("help", "simple"), is(SIMPLE_CMD_HELP));
        assertThat(exec("simple", "--help"), is(SIMPLE_CMD_HELP));
    }

    @Test
    public void testCmd() {
        assertThat(exec("simple"), is("noop" + EOL));
        assertThat(exec("simple", "--foo"), is("foo" + EOL));
        assertThat(exec("simple", "--bar"), is("bar" + EOL));
    }

    @Test
    public void testCommonOptions() {
        assertThat(exec("common", "--key", "value"), is("value" + EOL));
        assertThat(exec("common", "--foo", "--key", "value"), is(equalTo("foo: value" + EOL)));
        CommandContext context = context();
        exec(context, "common");
        assertThat(context.exitAction().status(), is(ExitStatus.FAILURE));
        assertThat(context.exitAction().message(), is("Missing required option: key" + EOL + "See 'test-cli common --help'"));
    }

    private static final class TestCommandRegistry extends CommandRegistry {

        public TestCommandRegistry() {
            super(/* pkg */"");
            register(new CommandWithCommonOptions());
            register(new SimpleCommand());
        }
    }

    private static final class SimpleCommand extends CommandModel {

        private static final FlagInfo FOO = new FlagInfo("foo", "Foo option");
        private static final FlagInfo BAR = new FlagInfo("bar", "Bar option");
        private static final CommandInfo CMD = new CommandInfo("simple", "A simple test command");

        SimpleCommand() {
            super(CMD, FOO, BAR);
        }

        @Override
        public CommandExecution createExecution(CommandParser.Resolver resolver) {
            return (context) -> {
                if (resolver.resolve(FOO)) {
                    Log.info("foo");
                } else if (resolver.resolve(BAR)) {
                    Log.info("bar");
                } else {
                    Log.info("noop");
                }
            };
        }
    }

    private static final class CommandWithCommonOptions extends CommandModel {

        private static final CommonOptionsInfo COMMON_OPTIONS = new CommonOptionsInfo();
        private static final FlagInfo FOO = new FlagInfo("foo", "Turn on foo mode");
        private static final CommandInfo CMD = new CommandInfo("common", "A test command with common options");

        CommandWithCommonOptions() {
            super(CMD, COMMON_OPTIONS, FOO);
        }

        @Override
        public CommandExecution createExecution(CommandParser.Resolver resolver) {
            return (context) -> {
                String key = COMMON_OPTIONS.resolve(resolver).key;
                if (resolver.resolve(FOO)) {
                    Log.info("foo: " + key);
                } else {
                    Log.info(key);
                }
            };
        }
    }

    private static final class CommonOptions {

        final String key;

        CommonOptions(String key) {
            this.key = key;
        }
    }

    private static final class CommonOptionsInfo extends CommandParameters.CommandFragmentInfo<CommonOptions> {

        private static final KeyValueInfo<String> KEY_OPTION = new KeyValueInfo<>(String.class, "key", "key option", null, true);

        private CommonOptionsInfo() {
            super(CommonOptions.class, KEY_OPTION);
        }

        @Override
        public CommonOptions resolve(CommandParser.Resolver resolver) {
            return new CommonOptions(resolver.resolve(KEY_OPTION));
        }
    }
}
