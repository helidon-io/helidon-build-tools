/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.io.InputStream;

import io.helidon.build.cli.harness.CommandContext.ExitStatus;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;

import io.helidon.build.common.ansi.AnsiTextStyle;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.LogWriter;
import org.junit.jupiter.api.AfterAll;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.ansi.AnsiTextStyle.strip;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test the built-in commands and options.
 */
public class ExecTest {

    static final CommandRegistry REGISTRY = new TestCommandRegistry();
    static final String CLI_USAGE = resourceAsString("cli-usage.txt");
    static final String HELP_CMD_HELP = resourceAsString("help-cmd-help.txt");
    static final String SIMPLE_CMD_HELP = resourceAsString("simple-cmd-help.txt");
    static final LogRecorder RECORDER = LogRecorder.create();

    static CommandContext context() {
        return new CommandContext(REGISTRY, null, false);
    }

    static String resourceAsString(String name) {
        InputStream is = ExecTest.class.getResourceAsStream(name);
        if (is != null) {
            return AnsiTextStyle.strip(Strings.normalizeNewLines(Strings.read(is)));
        }
        return null;
    }

    @BeforeAll
    static void beforeAllTests() {
        LogWriter.addRecorder(RECORDER);
    }

    @AfterAll
    static void afterAllTests() {
        LogWriter.removeRecorder(RECORDER);
    }

    @BeforeEach
    void beforeEachTest() {
        RECORDER.clear();
    }

    @AfterEach
    void afterEachTest() {
        RECORDER.clear();
    }

    String exec(CommandContext context, String... args) {
        RECORDER.clear();
        CommandRunner.execute2(context, args);
        String out = Strings.normalizeNewLines(RECORDER.output());
        return strip(out);
    }

    String exec(String... args) {
        return AnsiTextStyle.strip(exec(context(), args));
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
        assertThat(exec("simple"), is("noop\n"));
        assertThat(exec("simple", "--foo"), is("foo\n"));
        assertThat(exec("simple", "--bar"), is("bar\n"));
    }

    @Test
    public void testCommonOptions() {
        assertThat(exec("common", "--key", "value"), is("value\n"));
        assertThat(exec("common", "--foo", "--key", "value"), is(equalTo("foo: value\n")));
        CommandContext context = context();
        exec(context, "common");
        assertThat(context.exitAction().status(), is(ExitStatus.FAILURE));
        assertThat(Strings.normalizeNewLines(context.exitAction().message()),
                is("Missing required option: key\nSee 'test-cli common --help'"));
    }

    private static final class TestCommandRegistry extends CommandRegistry {

        public TestCommandRegistry() {
            super("", "test-cli", "A test cli");
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

        private static final KeyValueInfo<String> KEY_OPTION = new KeyValueInfo<>(
                String.class,
                "key",
                "key option",
                null,
                true);

        private CommonOptionsInfo() {
            super(CommonOptions.class, KEY_OPTION);
        }

        @Override
        public CommonOptions resolve(CommandParser.Resolver resolver) {
            return new CommonOptions(resolver.resolve(KEY_OPTION));
        }
    }
}
