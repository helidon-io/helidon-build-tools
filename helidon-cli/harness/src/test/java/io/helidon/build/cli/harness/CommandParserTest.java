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

import io.helidon.build.cli.harness.CommandModel.KeyValueInfo;
import io.helidon.build.cli.harness.CommandParser.CommandParserException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test {@link CommandParser}.
 */
public class CommandParserTest {

    @Test
    public void testTrim() {
        CommandParser parser = CommandParser.create("  cli  ", " --foo ", "   bar ");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("foo"));
        assertThat(parser.params().get("foo"), is(instanceOf(CommandParser.KeyValueParam.class)));
        assertThat(((CommandParser.KeyValueParam) parser.params().get("foo")).value(), is("bar"));
    }

    @Test
    public void testUpperCase() {
        CommandParser parser = CommandParser.create("CLI", "--HELP");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("help"));
        assertThat(parser.params().get("help"), is(instanceOf(CommandParser.FlagParam.class)));

        parser = CommandParser.create("cLi", "--HeLp");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("help"));
        assertThat(parser.params().get("help"), is(instanceOf(CommandParser.FlagParam.class)));

        parser = CommandParser.create("cLi", "--fOo", "bAR");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("foo"));
        assertThat(parser.params().get("foo"), is(instanceOf(CommandParser.KeyValueParam.class)));
        assertThat(((CommandParser.KeyValueParam) parser.params().get("foo")).value(), is("bAR"));
    }

    @Test
    public void testInvalidCommandNames() {
        CommandParser parser = CommandParser.create("-cli", "--help");
        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": -cli"));

        parser = CommandParser.create("cli-", "--help");
        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": cli-"));

        parser = CommandParser.create("great-cli", "--help");
        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": great-cli"));

        parser = CommandParser.create("great_cli", "--help");
        assertThat(parser.commandName().isEmpty(), is(true));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_COMMAND_NAME + ": great_cli"));
    }

    @Test
    public void testInvalidOptionName() {
        CommandParser parser = CommandParser.create("cli", "---help");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_OPTION_NAME + ": -help"));

        parser = CommandParser.create("cli", "--help-");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_OPTION_NAME + ": help-"));

        parser = CommandParser.create("cli", "--please_help");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_OPTION_NAME + ": please_help"));

        parser = CommandParser.create("cli", "--please-help");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("please-help"));
        assertThat(parser.params().get("please-help"), is(instanceOf(CommandParser.FlagParam.class)));
    }

    @Test
    public void testKeyValues() {
        CommandParser parser = CommandParser.create("cli", "--foo", "--foo", "bar");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_REPEATING_OPTION + ": foo"));

        parser = CommandParser.create("cli", "--foo", "1", "--foo");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_REPEATING_OPTION + ": foo"));

        parser = CommandParser.create("cli", "--foo", "bar1", "--foo", "bar2");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("foo"));
        assertThat(parser.params().get("foo"), is(instanceOf(CommandParser.KeyValuesParam.class)));
        assertThat(((CommandParser.KeyValuesParam) parser.params().get("foo")).values(), contains("bar1", "bar2"));

        parser = CommandParser.create("cli", "--foo", "bar1,bar2", "--foo", "bar3");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("cli"));
        assertThat(parser.error().isEmpty(), is(true));
        assertThat(parser.params().size(), is(1));
        assertThat(parser.params(), hasKey("foo"));
        assertThat(parser.params().get("foo"), is(instanceOf(CommandParser.KeyValuesParam.class)));
        assertThat(((CommandParser.KeyValuesParam) parser.params().get("foo")).values(), contains("bar1", "bar2", "bar3"));
    }

    @Test
    public void testProperties() {
        CommandParser parser = CommandParser.create("command", "-Dfoo=bar", "-Dbar=foo");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.error().isPresent(), is(false));
        assertThat(parser.properties().get("foo"), is("bar"));
        assertThat(parser.properties().get("bar"), is("foo"));

        parser = CommandParser.create("command", "-Dfoo");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.error().isPresent(), is(true));
        assertThat(parser.error().get(), is(CommandParser.INVALID_PROPERTY + ": foo"));

        parser = CommandParser.create("command", "-DfOo=Bar", " -DBAR=FOO ");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.error().isPresent(), is(false));
        assertThat(parser.properties().get("fOo"), is("Bar"));
        assertThat(parser.properties().get("BAR"), is("FOO"));

        parser = CommandParser.create("-Dfoo=bar", "command");
        assertThat(parser.commandName().isPresent(), is(true));
        assertThat(parser.commandName().get(), is("command"));
        assertThat(parser.properties().get("foo"), is("bar"));
    }

    enum DAY {
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    }

    @Test
    public void testResolve() {
        KeyValueInfo<DAY> keyValueInfo = new KeyValueInfo<>(DAY.class, "day", "day of the week", null, false);
        assertThat(CommandParser.create("cli", "--day", "MONDAY").resolve(keyValueInfo), is(DAY.MONDAY));
        assertThat(CommandParser.create("cli", "--day", "monday").resolve(keyValueInfo), is(DAY.MONDAY));
        assertThat(CommandParser.create("cli", "--day", "mOnDaY").resolve(keyValueInfo), is(DAY.MONDAY));
        CommandParserException ex = assertThrows(CommandParserException.class,
                () -> CommandParser.create("cli", "--day" ,"xxx").resolve(keyValueInfo));
        assertThat(ex.getMessage(), is(CommandParser.INVALID_CHOICE + ": " + "xxx"));
    }
}
