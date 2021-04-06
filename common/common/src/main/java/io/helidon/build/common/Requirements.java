/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common;

import java.util.Optional;

/**
 * Assertions with formatted message strings.
 */
public class Requirements {
    /**
     * Throws a {@code RequirementFailure} with a formatted message formatted if the given instance is {@code null}.
     *
     * @param instance The instance.
     * @param message The message.
     * @param args The message args.
     * @param <T> The instance type.
     * @return The instance.
     * @throws RequirementFailure if the condition returns {@code false}.
     */
    public static <T> T requireNonNull(T instance, String message, Object... args) {
        require(instance != null, message, args);
        return instance;
    }

    /**
     * Conditionally throws a {@code RequirementFailure} with formatted message.
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
     * Throws a {@code RequirementFailure} with a formatted message.
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
