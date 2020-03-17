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
package io.helidon.build.cli.impl;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.cli.impl.TestUtils.resourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * CLI main class test.
 */
public class MainTest {

    static final String CLI_USAGE = resourceAsString("cli-usage.txt");

    @Test
    public void testUsage() throws IOException, InterruptedException {
        TestUtils.ExecResult res = exec("--help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));

        res = exec("help");
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));

        res = exec();
        assertThat(res.code, is(equalTo(0)));
        assertThat(res.output, is(equalTo(CLI_USAGE)));
    }

    @Test
    public void testHelp() throws IOException, InterruptedException {
        TestUtils.ExecResult res = exec("build" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help" ,"build");
        assertThat(res.code, is(equalTo(0)));

        res = exec("dev" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help" ,"dev");
        assertThat(res.code, is(equalTo(0)));

        res = exec("features" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help", "features");
        assertThat(res.code, is(equalTo(0)));

        res = exec("info" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help", "info");
        assertThat(res.code, is(equalTo(0)));

        res = exec("init" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help", "init");
        assertThat(res.code, is(equalTo(0)));

        res = exec("version" ,"--help");
        assertThat(res.code, is(equalTo(0)));

        res = exec("help", "version");
        assertThat(res.code, is(equalTo(0)));
    }
}
