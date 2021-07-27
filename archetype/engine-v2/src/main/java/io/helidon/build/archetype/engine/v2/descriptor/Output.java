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

public class Output extends Conditional {

    private Model model;
    private final LinkedList<Transformation> transformations = new LinkedList<>();
    private final LinkedList<FileSets> filesList = new LinkedList<>();
    private final LinkedList<FileSet> fileList = new LinkedList<>();
    private final LinkedList<Template> template = new LinkedList<>();
    private final LinkedList<Templates> templates = new LinkedList<>();

    Output(String ifProperties) {
        super(ifProperties);
    }

    public LinkedList<Transformation> transformations() {
        return transformations;
    }

    public LinkedList<FileSets> filesList() {
        return filesList;
    }

    public LinkedList<FileSet> fileList() {
        return fileList;
    }

    public LinkedList<Template> template() {
        return template;
    }

    public LinkedList<Templates> templates() {
        return templates;
    }

    public Model model() {
        return model;
    }

    public void model(Model model) {
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Output that = (Output) o;
        return model.equals(that.model)
                && transformations.equals(that.transformations)
                && filesList.equals(that.filesList)
                && fileList.equals(that.fileList)
                && template.equals(that.template)
                && templates.equals(that.templates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), model, transformations, filesList, fileList, template, templates);
    }

    @Override
    public String toString() {
        return "Output{"
                + "model=" + model()
                + ", transformations=" + transformations()
                + ", filesList=" + filesList()
                + ", fileList=" + fileList()
                + ", template=" + template()
                + ", templates=" + templates()
                + '}';
    }

}
