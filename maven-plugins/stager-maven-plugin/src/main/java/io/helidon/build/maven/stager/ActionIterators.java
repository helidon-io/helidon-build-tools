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
package io.helidon.build.maven.stager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Internal model for action iterators.
 */
final class ActionIterators extends LinkedList<ActionIterator> implements StagingElement, Joinable {

    static final String ELEMENT_NAME = "iterators";
    private final boolean join;

    ActionIterators(List<ActionIterator> iterators, Map<String, String> attrs) {
        super();
        join = attrs != null && Boolean.parseBoolean(attrs.get("join"));
        addAll(iterators);
    }

    @Override
    public String elementName() {
        return ELEMENT_NAME;
    }

    @Override
    public boolean join() {
        return join;
    }
}
