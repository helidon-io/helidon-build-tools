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

package io.helidon.build.archetype.engine.v2.interpreter;

abstract class VisitorEmptyImpl<T> implements Visitor<T> {

    @Override
    public void visit(XmlDescriptor v, T arg) {

    }

    @Override
    public void visit(Visitable input, T arg) {

    }

    @Override
    public void visit(StepAST step, T arg) {

    }

    @Override
    public void visit(InputAST input, T arg) {

    }

    @Override
    public void visit(InputBooleanAST input, T arg) {

    }

    @Override
    public void visit(InputEnumAST input, T arg) {

    }

    @Override
    public void visit(InputListAST input, T arg) {

    }

    @Override
    public void visit(InputTextAST input, T arg) {

    }

    @Override
    public void visit(ExecAST exec, T arg) {

    }

    @Override
    public void visit(SourceAST source, T arg) {

    }

    @Override
    public void visit(ContextAST context, T arg) {

    }

    @Override
    public void visit(ContextBooleanAST contextBoolean, T arg) {

    }

    @Override
    public void visit(ContextEnumAST contextEnum, T arg) {

    }

    @Override
    public void visit(ContextListAST contextList, T arg) {

    }

    @Override
    public void visit(ContextTextAST contextText, T arg) {

    }

    @Override
    public void visit(OptionAST option, T arg) {

    }

    @Override
    public void visit(OutputAST output, T arg) {

    }

    @Override
    public void visit(TransformationAST transformation, T arg) {

    }

    @Override
    public void visit(FileSetsAST fileSets, T arg) {

    }

    @Override
    public void visit(FileSetAST fileSet, T arg) {

    }

    @Override
    public void visit(TemplateAST template, T arg) {

    }

    @Override
    public void visit(TemplatesAST templates, T arg) {

    }

    @Override
    public void visit(ModelAST model, T arg) {

    }

    @Override
    public void visit(IfStatement statement, T arg) {

    }

    @Override
    public void visit(ModelKeyValueAST value, T arg) {

    }

    @Override
    public void visit(ValueTypeAST value, T arg) {

    }

    @Override
    public void visit(ModelKeyListAST list, T arg) {

    }

    @Override
    public void visit(MapTypeAST map, T arg) {

    }

    @Override
    public void visit(ListTypeAST list, T arg) {

    }

    @Override
    public void visit(ModelKeyMapAST map, T arg) {

    }
}
