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

package io.helidon.build.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Time utilities.
 */
public class TimeUtils {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withZone(ZONE);

    /**
     * Returns the time as a date time string.
     *
     * @param millis The time in milliseconds since the epoch (e.g. {@link System#currentTimeMillis()}).
     * @return The formatted time.
     */
    public static String toDateTime(final long millis) {
        return FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    private TimeUtils() {
    }
}
