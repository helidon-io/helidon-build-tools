/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.ansi;

import io.helidon.build.common.RichTextRenderer;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link RichTextRenderer} using {@link AnsiText}.
 */
class AnsiTextRendererTest {

    static {
        System.setProperty("jansi.force", "true");
        Ansi.setEnabled(true);
    }

    @BeforeEach
    void checkAnsi() {
        Assumptions.assumeTrue(AnsiTextProvider.ANSI_ENABLED);
    }

    @Test
    void testUnknownStyle() {
        assertThat(RichTextRenderer.render("$(bogus text)"), is("text"));
    }

    @Test
    void testUnclosed() {
        assertThat(RichTextRenderer.render("$(red text"), is("$(red text"));
    }

    @Test
    void testMalformed() {
        assertThat(RichTextRenderer.render("$(red)"), is("$(red)"));
    }

    @Test
    void testEmphasisRender() {
        assertMatching("@|reset,bold PLAIN BOLD|@",
                       "$(plain,bold PLAIN BOLD)");

        assertMatching("@|faint,bold FAINT BOLD|@",
                       "$(faint,bold FAINT BOLD)");

        assertMatching("@|italic,bold ITALIC BOLD|@",
                       "$(italic,bold ITALIC BOLD)");

        assertMatching("@|underline,bold UNDERLINE BOLD|@",
                       "$(underline,bold UNDERLINE BOLD)");

        assertMatching("@|blink_slow,bold BLINK BOLD|@",
                       "$(blink,bold BLINK BOLD)");

        assertMatching("@|negative_on,bold NEGATIVE BOLD|@",
                       "$(negative,bold NEGATIVE BOLD)");

        assertMatching("@|conceal_on,bold CONCEAL BOLD|@",
                       "$(conceal,bold CONCEAL BOLD)");

        assertMatching("@|blue,bold BLUE BOLD|@",
                       "$(blue,bold BLUE BOLD)");
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
        String e6 = "This is a bold red $(RED,bg_yellow! example) on a bright yellow background.";
        String expected = Ansi.ansi()
                              .a("This is a bold red ")
                              .bold()
                              .fgRed()
                              .bgBright(Ansi.Color.YELLOW)
                              .a("example")
                              .reset()
                              .a(" on a bright yellow background.")
                              .toString();
        assertMatching(expected, e6);
    }

    @Test
    void testNestedColorsOneLevel() {
        String nested = "a$(red b$(blue c)d)e";
        // a = plain
        // b = red
        // c = blue
        // d = red
        // e = plain
        String expected = Ansi.ansi()
                              .a("a")
                              .fg(Ansi.Color.RED)
                              .a("b")
                              .fg(Ansi.Color.BLUE)
                              .a("c")
                              .reset()
                              .fg(Ansi.Color.RED)
                              .a("d")
                              .reset()
                              .a("e")
                              .toString();
        assertMatching(expected, nested);
    }

    @Test
    void testNestedMixedTwoLevels() {
        String nested = "|1|$(red 2|$(italic 3|$(CYAN! 4)|5)|6)|7|";
        // 1 = plain
        // 2 = red
        // 3 =   italic,red
        // 4 =     bold,bright,italic,cyan
        // 5 =   italic,red
        // 6 = red
        // 7 = plain
        String expected = Ansi.ansi()
                              .a("|")
                              .a("1")
                              .a("|")
                              .fg(Ansi.Color.RED)
                              .a("2")
                              .a("|")
                              .a(Ansi.Attribute.ITALIC)
                              .a("3")
                              .a("|")
                              .a(Ansi.Attribute.INTENSITY_BOLD)
                              .fgBright(Ansi.Color.CYAN)
                              .a("4")
                              .reset()
                              .fg(Ansi.Color.RED)
                              .a(Ansi.Attribute.ITALIC)
                              .a("|")
                              .a("5")
                              .reset()
                              .fg(Ansi.Color.RED)
                              .a("|")
                              .a("6")
                              .reset()
                              .a("|")
                              .a("7")
                              .a("|")
                              .toString();
        assertMatching(expected, nested);
    }

    private static void assertMatching(String renderAnsi, String renderStyle) {
        String expected = AnsiRenderer.render(renderAnsi);
        String rendered = RichTextRenderer.render(renderStyle);
        System.out.println("expected: " + expected);
        System.out.println("rendered: " + rendered);
        assertThat(rendered, is(expected));
    }
}
