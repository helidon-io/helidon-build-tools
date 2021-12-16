/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.lsp.server.service.metadata;

/**
 * Option kind.
 */
public enum ConfiguredOptionKind {

    /**
     * Option is a single value (leaf node).
     * Example: server port
     */
    VALUE,
    /**
     * Option is a list of values (either primitive, String or object nodes).
     * Example: cipher suite in SSL, server sockets
     */
    LIST,
    /**
     * Option is a map of strings to primitive type or String.
     * Example: tags in tracing, CDI configuration
     */
    MAP
}
