/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.LinkedList;
import java.util.Objects;

public class Templates extends Conditional {

    private Model model;
    private String directory;
    private final LinkedList<String> includes = new LinkedList<>();
    private final LinkedList<String> excludes = new LinkedList<>();
    private final String engine;
    private final String transformation;

    Templates(String engine, String transformation, String ifProperties) {
        super(ifProperties);
        this.engine = engine;
        this.transformation = transformation;
    }

    public Model model() {
        return model;
    }

    public String engine() {
        return engine;
    }

    public String directory() {
        return directory;
    }

    public String transformation() {
        return transformation;
    }

    public LinkedList<String> includes() {
        return includes;
    }

    public LinkedList<String> excludes() {
        return excludes;
    }

    public void directory(String directory) {
        this.directory = directory;
    }

    public void model(Model model) {
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Templates that = (Templates) o;
        return model.equals(that.model)
                && directory.equals(that.directory)
                && includes.equals(that.includes)
                && excludes.equals(that.excludes)
                && engine.equals(that.engine)
                && transformation.equals(that.transformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), model, directory, includes, excludes, engine, transformation);
    }

    @Override
    public String toString() {
        return "Templates{"
                + ", transformation=" + transformation()
                + ", engine=" + engine()
                + ", directory=" + directory()
                + ", includes=" + includes()
                + ", excludes=" + excludes()
                + ", model=" + model()
                + '}';
    }
}
