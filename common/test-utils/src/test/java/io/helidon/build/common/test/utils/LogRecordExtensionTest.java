/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.test.utils;

import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Tests {@link LogRecordExtension}.
 */
@ExtendWith(LogRecordExtension.class)
class LogRecordExtensionTest {

    private final LogRecorder recorder;
    private LogLevel previousLevel;

    LogRecordExtensionTest(LogRecorder recorder) {
        this.recorder = recorder;
    }

    @BeforeEach
    void setUp() {
        previousLevel = LogLevel.get();
        LogLevel.set(LogLevel.WARN);
    }

    @AfterEach
    void tearDown() {
        try {
            assertThat(LogLevel.get(), is(LogLevel.WARN));
            Log.info("not recorded");
            assertThat(recorder.entries(), is(empty()));
        } finally {
            LogLevel.set(previousLevel);
        }
    }

    @Test
    void testRecord(LogRecorder methodRecorder) {
        assertRecord(methodRecorder, "recorded message");
    }

    @ParameterizedTest
    @ValueSource(strings = {"first template message", "second template message"})
    void testRecordTemplate(String message, LogRecorder methodRecorder) {
        assertRecord(methodRecorder, message);
    }

    @Test
    void testNestedRecorderUsesLowestLevel(LogRecorder methodRecorder) {
        try (LogRecorder nestedRecorder = new LogRecorder(LogLevel.WARN).start()) {
            Log.debug("outer recorder message");
            Log.warn("both recorders message");

            assertThat(nestedRecorder.entries(), contains(containsString("both recorders message")));
        }
        assertThat(methodRecorder.entries(), contains(
                containsString("outer recorder message"),
                containsString("both recorders message")));
    }

    private void assertRecord(LogRecorder methodRecorder, String message) {
        assertThat(methodRecorder, sameInstance(recorder));
        assertThat(recorder.level(), is(LogLevel.DEBUG));
        assertThat(LogLevel.get(), is(LogLevel.DEBUG));

        Log.debug("%s", message);

        assertThat(recorder.entries(), contains(containsString(message)));
    }
}
