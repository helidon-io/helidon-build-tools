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

import io.helidon.build.archetype.engine.v2.descriptor.ContextBoolean;
import io.helidon.build.archetype.engine.v2.descriptor.ContextEnum;
import io.helidon.build.archetype.engine.v2.descriptor.ContextList;
import io.helidon.build.archetype.engine.v2.descriptor.ContextText;
import io.helidon.build.archetype.engine.v2.descriptor.FileSet;
import io.helidon.build.archetype.engine.v2.descriptor.FileSets;
import io.helidon.build.archetype.engine.v2.descriptor.ModelKeyValue;
import io.helidon.build.archetype.engine.v2.descriptor.Template;
import io.helidon.build.archetype.engine.v2.descriptor.Templates;
import io.helidon.build.archetype.engine.v2.descriptor.Transformation;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Visitor for the  script interpreter.
 *
 * @param <A> argument
 */
public interface Visitor<A> {

    /**
     * Process {@code Visitable} element.
     *
     * @param v   Visitable
     * @param arg argument
     */
    void visit(Visitable v, A arg);

    void visit(Flow v, A arg);

    void visit(XmlDescriptor v, A arg);

    /**
     * Process {@code StepAST} element.
     *
     * @param step StepAST
     * @param arg  argument
     */
    void visit(StepAST step, A arg);

    /**
     * Process {@code InputAST} element.
     *
     * @param input InputAST
     * @param arg   argument
     */
    void visit(InputAST input, A arg);

    /**
     * Process {@code InputBooleanAST} element.
     *
     * @param input InputBooleanAST
     * @param arg   argument
     */
    void visit(InputBooleanAST input, A arg);

    /**
     * Process {@code InputEnumAST} element.
     *
     * @param input InputEnumAST
     * @param arg   argument
     */
    void visit(InputEnumAST input, A arg);

    /**
     * Process {@code InputListAST} element.
     *
     * @param input InputListAST
     * @param arg   argument
     */
    void visit(InputListAST input, A arg);

    /**
     * Process {@code InputTextAST} element.
     *
     * @param input InputTextAST
     * @param arg   argument
     */
    void visit(InputTextAST input, A arg);

    /**
     * Process {@code ExecAST} element.
     *
     * @param exec ExecAST
     * @param arg  argument
     */
    void visit(ExecAST exec, A arg);

    /**
     * Process {@code SourceAST} element.
     *
     * @param source SourceAST
     * @param arg    argument
     */
    void visit(SourceAST source, A arg);

    /**
     * Process {@code ContextAST} element.
     *
     * @param context ContextAST
     * @param arg     argument
     */
    void visit(ContextAST context, A arg);

    /**
     * Process {@code ContextBooleanAST} element.
     *
     * @param contextBoolean ContextBooleanAST
     * @param arg            argument
     */
    void visit(ContextBooleanAST contextBoolean, A arg);

    /**
     * Process {@code ContextEnumAST} element.
     *
     * @param contextEnum ContextEnumAST
     * @param arg         argument
     */
    void visit(ContextEnumAST contextEnum, A arg);

    /**
     * Process {@code ContextListAST} element.
     *
     * @param contextList ContextListAST
     * @param arg         argument
     */
    void visit(ContextListAST contextList, A arg);

    /**
     * Process {@code ContextTextAST} element.
     *
     * @param contextText ContextTextAST
     * @param arg         argument
     */
    void visit(ContextTextAST contextText, A arg);

    /**
     * Process {@code OptionAST} element.
     *
     * @param option OptionAST
     * @param arg    argument
     */
    void visit(OptionAST option, A arg);

    /**
     * Process {@code OutputAST} element.
     *
     * @param output OutputAST
     * @param arg    argument
     */
    void visit(OutputAST output, A arg);

    /**
     * Process {@code TransformationAST} element.
     *
     * @param transformation TransformationAST
     * @param arg            argument
     */
    void visit(TransformationAST transformation, A arg);

    /**
     * Process {@code FileSetsAST} element.
     *
     * @param fileSets FileSetsAST
     * @param arg      argument
     */
    void visit(FileSetsAST fileSets, A arg);

    /**
     * Process {@code FileSetAST} element.
     *
     * @param fileSet FileSetAST
     * @param arg     argument
     */
    void visit(FileSetAST fileSet, A arg);

    /**
     * Process {@code TemplateAST} element.
     *
     * @param template TemplateAST
     * @param arg      argument
     */
    void visit(TemplateAST template, A arg);

    /**
     * Process {@code TemplatesAST} element.
     *
     * @param templates TemplatesAST
     * @param arg       argument
     */
    void visit(TemplatesAST templates, A arg);

    /**
     * Process {@code ModelAST} element.
     *
     * @param model ModelAST
     * @param arg   argument
     */
    void visit(ModelAST model, A arg);

    /**
     * Process {@code IfStatement} element.
     *
     * @param statement IfStatement
     * @param arg       argument
     */
    void visit(IfStatement statement, A arg);

    /**
     * Process {@code ModelKeyValueAST} element.
     *
     * @param value ModelKeyValueAST
     * @param arg   argument
     */
    void visit(ModelKeyValueAST value, A arg);

    /**
     * Process {@code ValueTypeAST} element.
     *
     * @param value ValueTypeAST
     * @param arg   argument
     */
    void visit(ValueTypeAST value, A arg);

    /**
     * Process {@code ModelKeyListAST} element.
     *
     * @param list ModelKeyListAST
     * @param arg  argument
     */
    void visit(ModelKeyListAST list, A arg);

    /**
     * Process {@code MapTypeAST} element.
     *
     * @param map MapTypeAST
     * @param arg argument
     */
    void visit(MapTypeAST map, A arg);

    /**
     * Process {@code ListTypeAST} element.
     *
     * @param list ListTypeAST
     * @param arg  argument
     */
    void visit(ListTypeAST list, A arg);

    /**
     * Process {@code ModelKeyMapAST} element.
     *
     * @param map ModelKeyMapAST
     * @param arg argument
     */
    void visit(ModelKeyMapAST map, A arg);
}
