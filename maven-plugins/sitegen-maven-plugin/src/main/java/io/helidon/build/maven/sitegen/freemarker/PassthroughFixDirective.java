/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.freemarker;
import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.ContentNode;

/**
 * A freemarker directive to fix asciidoctor passthrough not being substituted.
 */
public class PassthroughFixDirective implements TemplateDirectiveModel {

    private static final String PLACEHOLDER = "\u00960\u0097";
    private static final String EMPHASIS = "_";
    private static final String STRONG = "*";
    private static final String MONOSPACE = "`";
    private static final String SUPERSCRIPT = "^";
    private static final String SUBSCRIPT = "~";
    private static final String DOUBLE_QUOTES = "\"`";
    private static final String SINGLE_QUOTES = "'`";
    private static final String SINGLE_PLUS = "+";
    private static final String TRIPLE_PLUS = "+++";
    private static final String PASS_MACRO_C_BEGIN = "pass:c[";
    private static final String PASS_MACRO_BEGIN = "pass:[";
    private static final String PASS_MACRO_END = "]";

    @Override
    public void execute(Environment env,
            Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body)
            throws TemplateException, IOException {

        if (loopVars.length != 0) {
                throw new TemplateModelException(
                    "This directive does not allow loop variables.");
        }

        // Check if no parameters were given:
        if (body != null) {
            throw new TemplateModelException(
                    "This directive does not allow body content.");
        }

        TemplateModel textVar = env.getVariable("text");
        if (!(textVar instanceof TemplateScalarModel)) {
            throw new TemplateModelException(
                    "text variable is not a TemplateScalarModel");
        }

        String text = ((TemplateScalarModel) textVar).getAsString();

        if (PLACEHOLDER.equals(text)) {

            TemplateModel parentVar = env.getVariable("parent");
            if (!(parentVar instanceof ContentNodeHashModel)) {
                throw new TemplateModelException(
                        "pareant variable is not a ContentNodeHashModel");
            }

            ContentNode parent = ((ContentNodeHashModel) parentVar).getContentNode();

            String source;
            if (parent instanceof Block) {
                source = ((Block) parent).getSource();
            } else if (parent instanceof Cell) {
                source = ((Cell) parent).getSource();
            } else {
                throw new TemplateModelException(
                        "parent is not a Block or a Cell");
            }

            if (source == null || source.isEmpty()) {
                throw new TemplateModelException(
                        "source is null or empty");
            }

            String fixed = formatInlineSource(source);
            env.getOut().write(fixed);

        } else {
            // nothing to do, just write the text out
            env.getOut().write(text);
        }
    }

    private static String formatInlineSource(String source)
            throws TemplateModelException {

        if (isTextDelimitedBy(source, EMPHASIS, EMPHASIS)) {
            return formatInlineText(source, EMPHASIS, EMPHASIS);
        }

        if (isTextDelimitedBy(source, STRONG, STRONG)) {
            return formatInlineText(source, STRONG, STRONG);
        }

        if (isTextDelimitedBy(source, MONOSPACE, MONOSPACE)) {
            return formatInlineText(source, MONOSPACE, MONOSPACE);
        }

        if (isTextDelimitedBy(source, SUBSCRIPT, SUBSCRIPT)) {
            return formatInlineText(source, SUBSCRIPT, SUBSCRIPT);
        }

        if (isTextDelimitedBy(source, SUPERSCRIPT, SUPERSCRIPT)) {
            return formatInlineText(source, SUPERSCRIPT, SUPERSCRIPT);
        }

        if (isTextDelimitedBy(source, DOUBLE_QUOTES, DOUBLE_QUOTES)) {
            return formatInlineText(source, DOUBLE_QUOTES, DOUBLE_QUOTES);
        }

        if (isTextDelimitedBy(source, SINGLE_QUOTES, SINGLE_QUOTES)) {
            return formatInlineText(source, SINGLE_QUOTES, SINGLE_QUOTES);
        }

        return formatInlineText(source, "", "");
    }

    private static String formatInlineText(String text,
                                           String beginDelimiter,
                                           String endDelimiter) {

        String enclosed = getEnclosedText(text, beginDelimiter, endDelimiter);

        if (isTextDelimitedBy(enclosed, TRIPLE_PLUS, TRIPLE_PLUS)) {

            // if hard passthrough just return enclosed as is
            return getEnclosedText(enclosed, TRIPLE_PLUS, TRIPLE_PLUS);

        } else if (isTextDelimitedBy(enclosed, PASS_MACRO_BEGIN, PASS_MACRO_END)) {

            // if hard passthrough just return enclosed as is
            return getEnclosedText(enclosed, PASS_MACRO_BEGIN, PASS_MACRO_END);

        } else if (isTextDelimitedBy(enclosed, SINGLE_PLUS, SINGLE_PLUS)) {

            // if soft passthrough, do html encoding on the enclosed text
            String enclosedPassthrough = getEnclosedText(enclosed,
                    SINGLE_PLUS, SINGLE_PLUS);
            return escapeSpecialCharacters(enclosedPassthrough);

        } else if (isTextDelimitedBy(text, PASS_MACRO_C_BEGIN, PASS_MACRO_END)) {

            // if soft passthrough, do html encoding on the enclosed text
            String enclosedPassthrough =
                    getEnclosedText(enclosed, PASS_MACRO_C_BEGIN, PASS_MACRO_END);
            return escapeSpecialCharacters(enclosedPassthrough);

        } else {

            // TODO substitute attributes?
            return enclosed;
        }
    }

    private static String escapeSpecialCharacters(String text) {
        return text.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("&", "&amp;");
    }

    private static String getEnclosedText(String text, String begin, String end) {
        return text.substring(begin.length(), text.length() - end.length());
    }

    private static boolean isTextDelimitedBy(String text, String begin, String end) {
        return text.startsWith(begin)
                && text.endsWith(end)
                && text.length() > begin.length() + end.length();
    }
}
