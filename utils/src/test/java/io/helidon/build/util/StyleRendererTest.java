/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link StyleRenderer}.
 */
class StyleRendererTest {
    static {
        System.setProperty("jansi.force", "true");
        Ansi.setEnabled(true);
        AnsiConsoleInstaller.ensureInstalled();
    }

    @Test
    void testUnknownCode() {
        assertThrows(IllegalArgumentException.class, () -> StyleRenderer.render("$(bogus text)"));
    }

    @Test
    void testUnclosed() {
        assertThat(StyleRenderer.render("$(red text"), is("$(red text"));
    }

    @Test
    void testMalformed() {
        assertThat(StyleRenderer.render("$(red)"), is("$(red)"));
    }

    @Test
    void testOnlyFormatted() {
        assertMatching("@|italic,bold ITALIC BOLD|@",
                       "$(italic,bold ITALIC BOLD)");
    }

    @Test
    void testEscaped() {
        assertMatching("@|cyan (CYAN)|@",
                       "$(cyan (CYAN\\))");

        assertMatching("prefix @|cyan (CYAN)))|@ suffix",
                       "prefix $(cyan (CYAN\\)\\)\\)) suffix");
    }

    @Test
    void testMixed() {
        assertMatching("This is @|bold,red BOLD RED|@ text, and this is @|cyan CYAN|@ text.",
                       "This is $(RED BOLD RED) text, and this is $(cyan CYAN) text.");
    }

    @Test
    void testExample1() {
        String e1 = "This is a bold $(bold example).";
        assertMatching(e1.replace("$(", "@|").replace(")", "|@"), e1);
    }

    @Test
    void testExample2() {
        String e2 = "This is a red $(red example containing an escaped \\) close paren).";
        assertMatching(e2.replace("$(", "@|").replace("\\)", ")").replace("n)", "n|@"), e2);
    }

    @Test
    void testExample3() {
        String e3 = "This is a bright bold cyan $(CYAN! example).";
        String expected = Ansi.ansi()
                              .a("This is a bright bold cyan ")
                              .bold()
                              .fgBright(Ansi.Color.CYAN)
                              .a("example")
                              .reset()
                              .a(".")
                              .toString();
        assertMatching(expected, e3);
    }

    @Test
    void testExample4() {
        String e4 = "This is a bright green background $(bg_green! example).";
        String expected = Ansi.ansi()
                              .a("This is a bright green background ")
                              .bgBright(Ansi.Color.GREEN)
                              .a("example")
                              .reset()
                              .a(".")
                              .toString();
        assertMatching(expected, e4);
    }

    @Test
    void testExample5() {
        String e5 = "This is a bold blue underlined $(BLUE,underline example).";
        String expected = Ansi.ansi()
                              .a("This is a bold blue underlined ")
                              .bold()
                              .fgBlue()
                              .a(Ansi.Attribute.UNDERLINE)
                              .a("example")
                              .reset()
                              .a(".")
                              .toString();
        assertMatching(expected, e5);
    }

    @Test
    void testExample6() {
        String e6 = "This is a bold red $(RED,BG_YELLOW! example) on a bright bold yellow background.";
        String expected = Ansi.ansi()
                              .a("This is a bold red ")
                              .bold()
                              .fgRed()
                              .bold()
                              .bgBright(Ansi.Color.YELLOW)
                              .a("example")
                              .reset()
                              .a(" on a bright bold yellow background.")
                              .toString();
        assertMatching(expected, e6);
    }

    private static void assertMatching(String render, String format) {
        String expected = AnsiRenderer.render(render);
        String rendered = StyleRenderer.render(format);
        System.out.println("expected: " + expected);
        System.out.println("rendered: " + rendered);
        assertThat(rendered, is(expected));
    }
}
