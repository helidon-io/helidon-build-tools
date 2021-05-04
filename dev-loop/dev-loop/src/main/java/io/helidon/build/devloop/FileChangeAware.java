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

package io.helidon.build.devloop;

import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Optional;

import io.helidon.build.common.FileUtils;

/**
 * A type that can detect file modification time changes.
 */
public interface FileChangeAware {

    /**
     * Returns whether or not this file has a changed time.
     *
     * @return {@code true} if changed.
     */
    default boolean hasChanged() {
        return changedTime().isPresent();
    }

    /**
     * Returns the most recent change time, if any.
     *
     * @return The time if changed.
     */
    Optional<FileTime> changedTime();

    /**
     * Returns the most recent time if there is a change in any element of the collection.
     *
     * @param <T> The collection type.
     * @param collection The collection.
     * @return The time if changed.
     */
    static <T extends FileChangeAware> Optional<FileTime> changedTimeOf(Collection<T> collection) {
        FileTime changed = null;
        for (T target : collection) {
            final Optional<FileTime> changeTime = target.changedTime();
            if (changeTime.isPresent() && FileUtils.newerThan(changeTime.get(), changed)) {
                changed = changeTime.get();
            }
        }
        return Optional.ofNullable(changed);
    }

}
