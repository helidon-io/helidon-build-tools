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

/**
 * Visitor that processes the input and returns a value.
 *
 * @param <T> type of the returned value
 * @param <A> type of the additional argument
 */
public interface GenericVisitor<T, A> {

    /**
     * Process {@code InputEnumAST} element.
     *
     * @param input InputEnumAST
     * @param arg   argument
     * @return value
     */
    T visit(InputEnumAST input, A arg);

    /**
     * Process {@code InputListAST} element.
     *
     * @param input InputListAST
     * @param arg   argument
     * @return value
     */
    T visit(InputListAST input, A arg);

    /**
     * Process {@code InputBooleanAST} element.
     *
     * @param input InputBooleanAST
     * @param arg   argument
     * @return value
     */
    T visit(InputBooleanAST input, A arg);

    /**
     * Process {@code InputTextAST} element.
     *
     * @param input InputTextAST
     * @param arg   argument
     * @return value
     */
    T visit(InputTextAST input, A arg);

    /**
     * Process {@code ContextBooleanAST} element.
     *
     * @param input ContextBooleanAST
     * @param arg   argument
     * @return value
     */
    T visit(ContextBooleanAST input, A arg);

    /**
     * Process {@code ContextEnumAST} element.
     *
     * @param input ContextEnumAST
     * @param arg   argument
     * @return value
     */
    T visit(ContextEnumAST input, A arg);

    /**
     * Process {@code ContextListAST} element.
     *
     * @param input ContextListAST
     * @param arg   argument
     * @return value
     */
    T visit(ContextListAST input, A arg);

    /**
     * Process {@code ContextTextAST} element.
     *
     * @param input ContextTextAST
     * @param arg   argument
     * @return value
     */
    T visit(ContextTextAST input, A arg);

    /**
     * Process {@code StepAST} element.
     *
     * @param input StepAST
     * @param arg   argument
     * @return value
     */
    T visit(StepAST input, A arg);

    /**
     * Process {@code InputAST} element.
     *
     * @param input InputAST
     * @param arg   argument
     * @return value
     */
    T visit(InputAST input, A arg);

    /**
     * Process {@code ExecAST} element.
     *
     * @param input ExecAST
     * @param arg   argument
     * @return value
     */
    T visit(ExecAST input, A arg);

    /**
     * Process {@code SourceAST} element.
     *
     * @param input SourceAST
     * @param arg   argument
     * @return value
     */
    T visit(SourceAST input, A arg);

    /**
     * Process {@code ContextAST} element.
     *
     * @param input ContextAST
     * @param arg   argument
     * @return value
     */
    T visit(ContextAST input, A arg);

    /**
     * Process {@code OptionAST} element.
     *
     * @param input OptionAST
     * @param arg   argument
     * @return value
     */
    T visit(OptionAST input, A arg);

    /**
     * Process {@code OutputAST} element.
     *
     * @param input OutputAST
     * @param arg   argument
     * @return value
     */
    T visit(OutputAST input, A arg);

    /**
     * Process {@code TransformationAST} element.
     *
     * @param input TransformationAST
     * @param arg   argument
     * @return value
     */
    T visit(TransformationAST input, A arg);

    /**
     * Process {@code FileSetsAST} element.
     *
     * @param input FileSetsAST
     * @param arg   argument
     * @return value
     */
    T visit(FileSetsAST input, A arg);

    /**
     * Process {@code FileSetAST} element.
     *
     * @param input FileSetAST
     * @param arg   argument
     * @return value
     */
    T visit(FileSetAST input, A arg);

    /**
     * Process {@code TemplateAST} element.
     *
     * @param input TemplateAST
     * @param arg   argument
     * @return value
     */
    T visit(TemplateAST input, A arg);

    /**
     * Process {@code TemplatesAST} element.
     *
     * @param input TemplatesAST
     * @param arg   argument
     * @return value
     */
    T visit(TemplatesAST input, A arg);

    /**
     * Process {@code ModelAST} element.
     *
     * @param input ModelAST
     * @param arg   argument
     * @return value
     */
    T visit(ModelAST input, A arg);

    /**
     * Process {@code IfStatement} element.
     *
     * @param input IfStatement
     * @param arg   argument
     * @return value
     */
    T visit(IfStatement input, A arg);

    /**
     * Process {@code ModelKeyValueAST} element.
     *
     * @param input ModelKeyValueAST
     * @param arg   argument
     * @return value
     */
    T visit(ModelKeyValueAST input, A arg);

    /**
     * Process {@code ValueTypeAST} element.
     *
     * @param input ValueTypeAST
     * @param arg   argument
     * @return value
     */
    T visit(ValueTypeAST input, A arg);

    /**
     * Process {@code ModelKeyListAST} element.
     *
     * @param input ModelKeyListAST
     * @param arg   argument
     * @return value
     */
    T visit(ModelKeyListAST input, A arg);

    /**
     * Process {@code MapTypeAST} element.
     *
     * @param input MapTypeAST
     * @param arg   argument
     * @return value
     */
    T visit(MapTypeAST input, A arg);

    /**
     * Process {@code ListTypeAST} element.
     *
     * @param input ListTypeAST
     * @param arg   argument
     * @return value
     */
    T visit(ListTypeAST input, A arg);

    /**
     * Process {@code ModelKeyMapAST} element.
     *
     * @param input ModelKeyMapAST
     * @param arg   argument
     * @return value
     */
    T visit(ModelKeyMapAST input, A arg);

    /**
     * Process {@code XmlDescriptor} element.
     *
     * @param input XmlDescriptor
     * @param arg   argument
     * @return value
     */
    T visit(XmlDescriptor input, A arg);

    /**
     * Process {@code UserInputAST} element.
     *
     * @param input UserInputAST
     * @param arg   argument
     * @return value
     */
    T visit(UserInputAST input, A arg);

    /**
     * Process {@code Visitable} element.
     *
     * @param input Visitable
     * @param arg   argument
     * @return value
     */
    T visit(Visitable input, A arg);
}
