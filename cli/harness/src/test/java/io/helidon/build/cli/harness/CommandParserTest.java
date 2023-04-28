/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.cli.harness.CommandModel.FlagInfo;
import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandParser.CommandParserException;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.testResourcePath;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test {@link CommandParser}.
 */
public class CommandParserTest {

    @Test
    public void testTrim() {
        CommandParser parser;
        CommandParser.Resolver resolver;
        CommandParameters cmd = new CommandParameters(
                new CommandModel.KeyValueInfo<>(String.class, "foo", "Foo", null, true));

        parser = CommandParser.create("  command  ", " --foo ", "   bar ");
        resolver = parser.parseCommand(cmd);

        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(resolver.params().size(), is(1));
        assertThat(resolver.params(), hasKey("foo"));
        assertThat(resolver.params().get("foo"), is(instanceOf(CommandParser.KeyValueParam.class)));
        assertThat(((CommandParser.KeyValueParam) resolver.params().get("foo")).value(), is("bar"));
    }

    @Test
    public void testCase() {
        CommandParser parser;
        CommandParser.Resolver resolver;

        parser = CommandParser.create("COMMAND", "--HELP");

        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.globalResolver().params().size(), is(1));
        assertThat(parser.globalResolver().params(), hasKey("help"));
        assertThat(parser.globalResolver().params().get("help"), is(instanceOf(CommandParser.FlagParam.class)));

        parser = CommandParser.create("CoMmAnD", "--HeLp");

        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.globalResolver().params().size(), is(1));
        assertThat(parser.globalResolver().params(), hasKey("help"));
        assertThat(parser.globalResolver().params().get("help"), is(instanceOf(CommandParser.FlagParam.class)));

        CommandParameters cmd = new CommandParameters(
                new CommandModel.KeyValueInfo<>(String.class, "foo", "Foo", null, true));
        parser = CommandParser.create("cOmMaNd", "--fOo", "bAR");
        resolver = parser.parseCommand(cmd);

        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(resolver.params().size(), is(1));
        assertThat(resolver.params(), hasKey("foo"));
        assertThat(resolver.params().get("foo"), is(instanceOf(CommandParser.KeyValueParam.class)));
        assertThat(((CommandParser.KeyValueParam) resolver.params().get("foo")).value(), is("bAR"));
    }

    @Test
    public void testCommandNames() {
        CommandParser parser;

        parser = CommandParser.create("-command", "--help");

        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": -command"));

        parser = CommandParser.create("command-", "--help");

        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": command-"));

        parser = CommandParser.create("great-command", "--help");

        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": great-command"));

        parser = CommandParser.create("great_command", "--help");

        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": great_command"));
    }

    @Test
    public void testOptionNames() {
        CommandParser parser;
        CommandParser.Resolver resolver;

        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "---help").parseCommand(),
                CommandParser.INVALID_OPTION_NAME + ": -help");

        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "--help-").parseCommand(),
                CommandParser.INVALID_OPTION_NAME + ": help-");

        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "--please_help").parseCommand(),
                CommandParser.INVALID_OPTION_NAME + ": please_help");

        CommandParameters cmd = new CommandParameters(
                new CommandModel.FlagInfo("please-help", "Please Help", true));
        parser = CommandParser.create("command", "--please-help");
        resolver = parser.parseCommand(cmd);

        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(resolver.params().size(), is(1));
        assertThat(resolver.params(), hasKey("please-help"));
        assertThat(resolver.params().get("please-help"), is(instanceOf(CommandParser.FlagParam.class)));
    }

    @Test
    public void testKeyValues() {
        CommandParameters cmd = new CommandParameters(
                new CommandModel.KeyValuesInfo<>(String.class, "foo", "Foo", true));

        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "--foo", "--foo", "bar").parseCommand(cmd),
                CommandParser.INVALID_REPEATING_OPTION + ": foo");

        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "--foo", "1", "--foo").parseCommand(cmd),
                CommandParser.INVALID_REPEATING_OPTION + ": foo");

        CommandParser parser;
        CommandParser.Resolver resolver;

        parser = CommandParser.create("command", "--foo", "bar1", "--foo", "bar2");
        resolver = parser.parseCommand(cmd);

        assertThat(resolver.params().size(), is(1));
        assertThat(resolver.params(), hasKey("foo"));
        assertThat(resolver.params().get("foo"), is(instanceOf(CommandParser.KeyValuesParam.class)));
        assertThat(((CommandParser.KeyValuesParam) resolver.params().get("foo")).values(), contains("bar1", "bar2"));

        parser = CommandParser.create("command", "--foo", "bar1,bar2", "--foo", "bar3");
        resolver = parser.parseCommand(cmd);

        assertThat(resolver.params().size(), is(1));
        assertThat(resolver.params(), hasKey("foo"));
        assertThat(resolver.params().get("foo"), is(instanceOf(CommandParser.KeyValuesParam.class)));
        assertThat(((CommandParser.KeyValuesParam) resolver.params().get("foo")).values(), contains("bar1", "bar2", "bar3"));
    }

    @Test
    public void testProperties() {
        CommandParser parser;
        CommandParser.Resolver resolver;

        parser = CommandParser.create("command", "-Dfoo=bar", "-Dbar=foo");
        resolver = parser.parseCommand(new CommandParameters());

        assertThat(parser.globalResolver().properties().isEmpty(), is(true));
        assertThat(resolver.properties().get("foo"), is("bar"));
        assertThat(resolver.properties().get("bar"), is("foo"));

        CommandParser noValuePropParser = CommandParser.create("command", "-Dfoo");

        assertThat(noValuePropParser.commandName().isPresent(), is(true));
        assertThat(noValuePropParser.commandName().get(), is("command"));
        assertThat(parser.globalResolver().properties().isEmpty(), is(true));
        resolver = noValuePropParser.parseCommand(new CommandParameters());
        assertThat(resolver.properties().get("foo"), is(""));

        parser = CommandParser.create("command", "-DfOo=Bar", " -DBAR=FOO ");
        resolver = parser.parseCommand(new CommandParameters());

        assertThat(parser.globalResolver().properties().isEmpty(), is(true));
        assertThat(resolver.properties().get("fOo"), is("Bar"));
        assertThat(resolver.properties().get("BAR"), is("FOO"));

        parser = CommandParser.create("-Dfoo=bar", "command");
        assertThat(parser.globalResolver().properties().get("foo"), is("bar"));

        KeyValueInfo<String> param = new KeyValueInfo<>(String.class, "key1", "Key1", null, true);
        parser = CommandParser.create("-Dfoo=bar", "command", "--key1", "-Dbob=alice");
        resolver = parser.parseCommand(new CommandParameters(param));

        assertThat(parser.globalResolver().properties().get("foo"), is("bar"));
        assertThat(resolver.resolve(param), is("-Dbob=alice"));
    }

    enum DAY {
        MONDAY,
    }

    @Test
    public void testResolve() {
        KeyValueInfo<DAY> param = new KeyValueInfo<>(DAY.class, "day", "day of the week", null, false);
        CommandParameters cmd = new CommandParameters(param);

        assertThat(CommandParser.create("command", "--day", "MONDAY").parseCommand(cmd).resolve(param), is(DAY.MONDAY));
        assertThat(CommandParser.create("command", "--day", "monday").parseCommand(cmd).resolve(param), is(DAY.MONDAY));
        assertThat(CommandParser.create("command", "--day", "mOnDaY").parseCommand(cmd).resolve(param), is(DAY.MONDAY));
        assertThrows(CommandParserException.class,
                () -> CommandParser.create("command", "--day", "xxx").parseCommand(cmd).resolve(param),
                CommandParser.INVALID_CHOICE + ": " + "xxx");
    }

    @Test
    public void testGlobalOptions() throws IOException {
        CommandParser parser;
        CommandParser.Resolver resolver;
        GlobalOptions options;
        String propsFile = Files.createTempFile("empty", null).toString();

        parser = CommandParser.create("command", "-Dfoo=bar", "--help", "--props-file", propsFile, "--args-file", propsFile);
        resolver = parser.parseCommand();
        options = new GlobalOptions(resolver.params());

        assertThat(options.help(), is(true));
        assertThat(options.version(), is(false));
        assertThat(options.propsFile(), is(propsFile));
        assertThat(options.argsFile(), is(propsFile));
        assertThat(parser.globalResolver().params().containsKey("help"), is(true));
        assertThat(resolver.params().containsKey("help"), is(true));
        assertThat(parser.globalResolver().properties().isEmpty(), is(true));
        assertThat(resolver.properties().getProperty("foo"), is("bar"));
    }

    @Test
    public void testVersionFlag() {
        CommandParser parser = CommandParser.create("--version");
        assertThat(parser.commandName().get(), is("version"));
    }

    @Test
    public void testDuplicateOptions() {
        FlagInfo verboseFlag1 = new FlagInfo("verbose", "Verbose log level", false);
        FlagInfo verboseFlag2 = new FlagInfo("verbose", "Verbose log level", false);
        CommandParameters cmd = new CommandParameters(verboseFlag1, verboseFlag2);

        CommandParser parser;
        CommandParser.Resolver resolver;

        parser = CommandParser.create("command", "--verbose");
        resolver = parser.parseCommand(cmd);
        assertThat(resolver.params().containsKey("verbose"), is(true));
        assertThat(resolver.params().get("verbose"), is(instanceOf(CommandParser.FlagParam.class)));
        assertThat(resolver.resolve(verboseFlag1), is(true));
        assertThat(resolver.resolve(verboseFlag2), is(true));
    }

    @Test
    public void testArgsFileOptionWithExistingFile() throws URISyntaxException {
        KeyValueInfo<String> param = new KeyValueInfo<>(String.class, "flavor", "flavor", null, false);
        CommandParameters cmd = new CommandParameters(param);

        URI argsFilePath = getClass().getResource("args.txt").toURI();
        CommandParser parser = CommandParser.create("command", "--args-file", Paths.get(argsFilePath).toString());
        CommandParser.Resolver resolver = parser.parseCommand(cmd);

        Map<String, CommandParser.Parameter> params = resolver.params();
        assertThat(params.containsKey("help"), is(true));
        assertThat(((CommandParser.KeyValueParam) params.get("flavor")).value(), is("se"));

        Properties properties = resolver.properties();
        assertThat(properties.get("foo"), is("bar"));
    }

    @Test
    public void testArgsFileOptionWithIncorrectFile() {
        UncheckedIOException e = assertThrows(
                UncheckedIOException.class,
                () -> CommandParser.create("command", "--args-file", "not_existing_file.txt")
        );
        assertThat(e.getMessage(), containsString("java.nio.file.NoSuchFileException: not_existing_file.txt"));
    }

    @Test
    public void testPropsFileOptionWithExistingFile() {
        String packagePath = getClass().getPackageName().replaceAll("\\.", "/");
        String argsFilePath = testResourcePath(getClass(), packagePath + "/test-props-file.properties").toString();
        KeyValueInfo<String> propsFileOption = new KeyValueInfo<>(String.class, "props-file", "properties file", null, false);
        CommandParameters cmd = new CommandParameters(propsFileOption);

        CommandParser parser = CommandParser.create("command", "--props-file", argsFilePath);
        CommandParser.Resolver resolver = parser.parseCommand(cmd);

        Properties properties = resolver.properties();
        assertThat(properties.get("security.atz"), is("abac"));
        assertThat(properties.get("flavor"), is("mp"));
        assertThat(properties.get("media"), is("json,multipart"));
    }

    @Test
    public void testPropsFileOptionWithIncorrectFile() {
        KeyValueInfo<String> propsFileOption = new KeyValueInfo<>(String.class, "props-file", "properties file", null, false);
        CommandParameters cmd = new CommandParameters(propsFileOption);

        CommandParser parser = CommandParser.create("command", "--props-file", "not_existing_props_file.txt");

        UncheckedIOException e = assertThrows(
                UncheckedIOException.class,
                () -> parser.parseCommand(cmd));
        assertThat(e.getMessage(), containsString("NoSuchFileException: not_existing_props_file.txt"));
    }
}
