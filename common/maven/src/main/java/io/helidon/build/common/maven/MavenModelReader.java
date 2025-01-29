/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven;

import java.io.IOException;
import java.io.InputStream;

import io.helidon.build.common.xml.XMLReader;

/**
 * Maven model reader.
 */
final class MavenModelReader implements AutoCloseable {

    private final MavenModel.Builder builder = new MavenModel.Builder();
    private final XMLReader reader;
    private int state;

    MavenModelReader(InputStream is) {
        reader = new XMLReader(is);
    }

    MavenModel read() {
        String name = reader.readName();
        if (!name.equals("project")) {
            throw new IllegalStateException(String.format(
                    "Invalid root element: %s, location=%d:%d",
                    name, reader.lineNumber(), reader.colNumber()));
        }
        reader.read(this::readProject);
        return builder.build();
    }

    private boolean readProject(String name) {
        switch (name) {
            case "parent":
                reader.read(this::readParent);
                state++;
                break;
            case "groupId":
                builder.groupId(reader.readText());
                state++;
                break;
            case "artifactId":
                builder.artifactId(reader.readText());
                state++;
                break;
            case "version":
                builder.version(reader.readText());
                state++;
                break;
            case "name":
                builder.name(reader.readText());
                state++;
                break;
            case "description":
                builder.description(reader.readText());
                state++;
                break;
            default:
                // ignore
                reader.read(n -> true);
        }
        return state < 6;
    }

    private boolean readParent(String name) {
        switch (name) {
            case "groupId":
                builder.parentGroupId(reader.readText());
                break;
            case "artifactId":
                builder.parentArtifactId(reader.readText());
                break;
            case "version":
                builder.parentVersion(reader.readText());
                break;
            default:
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
