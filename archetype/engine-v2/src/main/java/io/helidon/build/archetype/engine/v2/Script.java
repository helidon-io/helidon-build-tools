/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.helidon.build.common.VirtualFileSystem.randomPath;

/**
 * Script.
 */
public final class Script {

    /**
     * Empty script.
     */
    static final Script EMPTY = new Script(Map.of());

    private final Loader loader;
    private final Source source;
    private final Map<String, Node> methods;

    /**
     * Load a script.
     *
     * @param path path
     * @return Node
     */
    public static Node load(Path path) {
        return load(() -> path.toAbsolutePath().normalize(), true);
    }

    /**
     * Load a script.
     *
     * @param source   stream source
     * @param readOnly {@code true} if read-only, {@code false} otherwise
     * @return Node
     */
    public static Node load(Source source, boolean readOnly) {
        return new ScriptLoaderImpl(readOnly).get(source);
    }

    /**
     * Create a new instance.
     *
     * @param loader  loader
     * @param source  source
     * @param methods methods
     */
    Script(Loader loader, Source source, Map<String, Node> methods) {
        this.loader = Objects.requireNonNull(loader, "loader is null");
        this.source = Objects.requireNonNull(source, "source is null");
        this.methods = Objects.requireNonNull(methods, "methods is null");
    }

    /**
     * Create a new instance.
     *
     * @param methods methods
     */
    Script(Map<String, Node> methods) {
        this.loader = Loader.EMPTY;
        this.source = () -> randomPath("unknown.xml");
        this.methods = Objects.requireNonNull(methods, "methods is null");
    }

    /**
     * Get the script loader.
     *
     * @return script loader
     */
    public Loader loader() {
        return loader;
    }

    /**
     * Get the script path.
     *
     * @return path
     */
    public Path path() {
        return source.path();
    }

    /**
     * Get the script input stream.
     *
     * @return input stream
     */
    public InputStream inputStream() {
        return source.inputStream();
    }

    /**
     * Get the methods.
     *
     * @return methods
     */
    public Map<String, Node> methods() {
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Script)) {
            return false;
        }
        Script other = (Script) o;
        return Objects.equals(path(), other.path());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Script{"
               + "path=" + source.path()
               + '}';
    }

    /**
     * Script node reader.
     */
    public interface Reader extends AutoCloseable {

        /**
         * Read a script node.
         *
         * @return Node
         */
        Node readScript();
    }

    /**
     * Script node writer.
     */
    public interface Writer extends AutoCloseable {

        /**
         * Write a script.
         *
         * @param node script
         */
        void writeScript(Node node);
    }

    /**
     * Script source.
     */
    public interface Source {

        /**
         * Create a {@link Path} backed instance.
         *
         * @param path path
         * @return Source
         */
        static Source of(Path path) {
            return (() -> path.toAbsolutePath().normalize());
        }

        /**
         * Get the input stream.
         *
         * @return InputStream
         */
        default InputStream inputStream() {
            try {
                return Files.newInputStream(path());
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        /**
         * Read a script.
         *
         * @param readOnly {@code true} if read-only, {@code false} otherwise
         * @param loader   script loader
         * @return Node
         */
        default Node readScript(boolean readOnly, Loader loader) {
            try (XMLScriptReader reader = new XMLScriptReader(this, readOnly, loader)) {
                return reader.readScript();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        /**
         * Get the path.
         *
         * @return Path
         */
        Path path();
    }

    /**
     * Script loader.
     */
    public interface Loader {

        /**
         * Empty script loader.
         */
        Loader EMPTY = (source) -> {
            throw new UnsupportedOperationException();
        };

        /**
         * Load a script.
         *
         * @param source stream source
         * @return Script
         */
        Node get(Source source);

        /**
         * Load a script.
         *
         * @param path path
         * @return Script
         */
        default Node get(Path path) {
            return get(Source.of(path));
        }
    }

    private static final class ScriptLoaderImpl implements Loader {

        private final Map<Path, Node> scripts = new HashMap<>();

        private final boolean readOnly;

        private ScriptLoaderImpl(boolean readOnly) {
            this.readOnly = readOnly;
        }

        @Override
        public Node get(Source source) {
            return scripts.computeIfAbsent(source.path(), k -> source.readScript(readOnly, this));
        }
    }
}
