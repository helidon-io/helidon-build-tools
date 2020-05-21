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

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Assertions with message strings formatted via {@link Style#render(String, Object...)}.
 */
public class Requirements {

    /**
     * Conditionally throws a {@code Failure} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param condition The condition.
     * @param message The message.
     * @param args The message args.
     * @throws RequirementsFailure if the condition returns {@code false}.
     */
    public static void requires(Supplier<Boolean> condition, String message, Object... args) {
        requires(condition.get(), message, args);
    }

    /**
     * Conditionally throws a {@code CommandException} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param condition The condition.
     * @param message The message.
     * @param args The message args.
     * @throws RequirementsFailure if the condition is {@code false}.
     */
    public static void requires(boolean condition, String message, Object... args) {
        if (!condition) {
            failed(message, args);
        }
    }

    /**
     * Throws a {@code CommandException} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param message The message.
     * @param args The message args.
     * @throws RequirementsFailure always.
     */
    public static void failed(String message, Object... args) {
        throw new RequirementsFailure(message, args);
    }

    /**
     * Convert the given error to a {@link RequirementsFailure} if it is or was caused by this type.
     *
     * @param error The error.
     * @return The optional {@link RequirementsFailure}.
     */
    public static Optional<RequirementsFailure> toFailure(Throwable error) {
        if (error == null) {
            return Optional.empty();
        } else if (error instanceof RequirementsFailure) {
            return Optional.of((RequirementsFailure) error);
        }
        // Recurse
        return toFailure(error.getCause()).or(Optional::empty);
    }

    private Requirements() {
    }
}
