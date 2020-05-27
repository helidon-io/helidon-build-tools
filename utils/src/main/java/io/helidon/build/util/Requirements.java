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
     * Conditionally throws a {@code RequirementFailure} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param condition The condition.
     * @param message The message.
     * @param args The message args.
     * @throws RequirementFailure if the condition returns {@code false}.
     */
    public static void require(Supplier<Boolean> condition, String message, Object... args) {
        require(condition.get(), message, args);
    }

    /**
     * Conditionally throws a {@code RequirementFailure} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param condition The condition.
     * @param message The message.
     * @param args The message args.
     * @throws RequirementFailure if the condition is {@code false}.
     */
    public static void require(boolean condition, String message, Object... args) {
        if (!condition) {
            failed(message, args);
        }
    }

    /**
     * Throws a {@code RequirementFailure} with a message formatted via {@link Style#render(String, Object...)}.
     *
     * @param message The message.
     * @param args The message args.
     * @throws RequirementFailure always.
     */
    public static void failed(String message, Object... args) {
        throw new RequirementFailure(message, args);
    }

    /**
     * Convert the given error to a {@link RequirementFailure} if it is or was caused by this type.
     *
     * @param error The error.
     * @return The optional {@link RequirementFailure}.
     */
    public static Optional<RequirementFailure> toFailure(Throwable error) {
        if (error == null) {
            return Optional.empty();
        } else if (error instanceof RequirementFailure) {
            return Optional.of((RequirementFailure) error);
        }
        // Recurse
        return toFailure(error.getCause()).or(Optional::empty);
    }

    private Requirements() {
    }
}
