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

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link KramdownExtension}, Markdown parser and rendering Markdown text to {@code html}.
 */
class MarkdownSupportTest {

    private static final Set<Extension> EXTENSIONS = Collections.singleton(KramdownExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    @Test
    public void coloredTextNode() {
        assertRendering("foo {::primary}This is an info colored text{:/} boo",
                "<p>foo <div style=\"background-color:#cce5ff; color:#004085; border-color:#b8daff;\">This is an info colored " +
                        "text</div> boo</p>\n");
    }

    @Test
    public void mixedMarkdownTextNode() {
        assertRendering("foo **bold text** {::primary}This is an info colored text{:/} boo _italic_ \n" +
                        "```java\n" +
                        "request.content().as(JsonObject.class).thenAccept(json -> {\n" +
                        "    System.output.println(json);\n" +
                        "    response.send(\"OK\");\n" +
                        "});\n" +
                        "```\n" +
                        "[The link](https://example.com)\n" +
                        "baz `code` bar",
                "<p>foo <strong>bold text</strong> <div style=\"background-color:#cce5ff; color:#004085; border-color:#b8daff;" +
                        "\">This is an info colored text</div> boo <em>italic</em></p>\n" +
                        "<pre><code class=\"language-java\">request.content().as(JsonObject.class).thenAccept(json -&gt; {\n" +
                        "    System.output.println(json);\n" +
                        "    response.send(&quot;OK&quot;);\n" +
                        "});\n" +
                        "</code></pre>\n" +
                        "<p><a href=\"https://example.com\">The link</a>\n" +
                        "baz <code>code</code> bar</p>\n"
        );
    }

    @Test
    public void nonexistentKramdownNode() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            render("foo {::nonexistent}This is an info colored text{:/} boo");
        });
        assertThat(e.getMessage(), containsString(
                "KramdownNode with name nonexistent does not exist"));
    }

    @Test
    public void otherTextNode() {
        assertRendering("foo boo",
                "<p>foo boo</p>\n");
    }

    private String render(String source) {
        Node parse = PARSER.parse(source);
        return RENDERER.render(PARSER.parse(source));
    }

    private void assertRendering(String source, String expectedResult) {
        String renderedContent = render(source);

        // include source for better assertion errors
        String expected = showTabs(expectedResult + "\n\n" + source);
        String actual = showTabs(renderedContent + "\n\n" + source);
        assertEquals(expected, actual);
    }

    private static String showTabs(String s) {
        // Tabs are shown as "rightwards arrow" for easier comparison
        return s.replace("\t", "\u2192");
    }
}