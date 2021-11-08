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

package io.helidon.build.common.markdown;

/**
 * Attempt to parse backticks, returning either a backtick code span or a literal sequence of backticks.
 */
class BackticksInlineParser implements InlineContentParser {

    @Override
    public ParsedInline tryParse(InlineParser inlineParser) {
        Scanner scanner = inlineParser.scanner();
        Position start = scanner.position();
        int openingTicks = scanner.matchMultiple('`');
        Position afterOpening = scanner.position();

        while (scanner.find('`') > 0) {
            Position beforeClosing = scanner.position();
            int count = scanner.matchMultiple('`');
            if (count == openingTicks) {
                Code node = new Code();

                String content = scanner.source(afterOpening, beforeClosing).content();
                content = content.replace('\n', ' ');

                if (content.length() >= 3
                        && content.charAt(0) == ' '
                        && content.charAt(content.length() - 1) == ' '
                        && Parsing.hasNonSpace(content)
                ) {
                    content = content.substring(1, content.length() - 1);
                }

                node.literal(content);
                return ParsedInline.of(node, scanner.position());
            }
        }

        SourceLines source = scanner.source(start, afterOpening);
        Text text = new Text(source.content());
        return ParsedInline.of(text, afterOpening);
    }
}
