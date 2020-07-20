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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Rich text styles.
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public class Style {
    private static final Style NONE = new Style();
    private static final Style PLAIN = new Emphasis(Attribute.RESET);
    private static final Style BOLD = new Emphasis(Attribute.INTENSITY_BOLD);
    private static final Style ITALIC = new Emphasis(Attribute.ITALIC);
    private static final Style FAINT = new Emphasis(Attribute.INTENSITY_FAINT);
    private static final Style BOLD_ITALIC = new StyleList(BOLD).add(ITALIC);
    private static final Style NEGATIVE = new Emphasis(Attribute.NEGATIVE_ON);
    private static final boolean ENABLED = AnsiConsoleInstaller.install();
    private static final String ANSI_ESCAPE_BEGIN = "\033[";
    private static final Map<String, Style> STYLES = stylesByName();
    private static final Lock STRIP_LOCK = new ReentrantReadWriteLock().writeLock();
    private static final ByteArrayOutputStream STRIP_BYTES = new ByteArrayOutputStream();
    private static final AnsiOutputStream STRIP = new AnsiOutputStream(STRIP_BYTES);

    /**
     * Return all styles, by name.
     *
     * @return The styles. Not immutable, so may be (carefully!) modified.
     */
    public static Map<String, Style> styles() {
        return STYLES;
    }

    /**
     * Returns a no-op style.
     *
     * @return The style.
     */
    public static Style none() {
        return NONE;
    }

    /**
     * Returns the style for the given name.
     * <p></p>
     * <h3>Text Color Names</h3>
     * <ul>
     *     <li>{@code red}</li>
     *     <li>{@code yellow}</li>
     *     <li>{@code green}</li>
     *     <li>{@code cyan}</li>
     *     <li>{@code blue}</li>
     *     <li>{@code magenta}</li>
     *     <li>{@code white}</li>
     *     <li>{@code black}</li>
     *     <li>{@code default}</li>
     *     <li>{@code bold}</li>
     *     <li>{@code negative}</li>
     * </ul>
     * <p></p>
     * See Portability below for more on {@code default}, {@code bold} and {@code negative}.
     * <p></p>
     * <h3>Background Color Names</h3>
     * <ul>
     *     <li>{@code bg_red}</li>
     *     <li>{@code bg_yellow}</li>
     *     <li>{@code bg_green}</li>
     *     <li>{@code bg_cyan}</li>
     *     <li>{@code bg_blue}</li>
     *     <li>{@code bg_magenta}</li>
     *     <li>{@code bg_white}</li>
     *     <li>{@code bg_black}</li>
     *     <li>{@code bg_default}</li>
     *     <li>{@code bg_negative}</li>
     * </ul>
     * <p></p>
     * <h3>Emphasis Names</h3>
     * <ul>
     *     <li>{@code italic}</li>
     *     <li>{@code bold}</li>
     *     <li>{@code faint}</li>
     *     <li>{@code plain}</li>
     *     <li>{@code underline}</li>
     *     <li>{@code strikethrough}</li>
     *     <li>{@code negative}</li>
     *     <li>{@code conceal}</li>
     *     <li>{@code blink}</li>
     * </ul>
     * <p></p>
     * <h3>Aliases</h3>
     * <p></p>
     * Every text color has the following aliases:
     * <ul>
     *      <li>Bold variant with an uppercase name (e.g. {@code RED})</li>
     *      <li>Bold variant with {@code '*'} prefix and suffix (e.g. {@code *red*})</li>
     *      <li>Italic variant with {@code '_'} prefix and suffix (e.g. {@code _red_})</li>
     *      <li>Bold italic variant with {@code '_*'} prefix and {@code '*_'} suffix (e.g. {@code _*red*_} or {@code *_red_*})</li>
     *      <li>Bright variants of the color and all the above with a {@code '!'} suffix
     *      (e.g. {@code red!}, {@code RED!}, {@code *red*!}, {@code _red_!}</li>
     * </ul>
     * <p></p>
     * Every background color has the following aliases:
     * <ul>
     *     <li> Bright variants with a {@code '!'} suffix (e.g. {@code bg_yellow!})</li>
     * </ul>
     * <p></p>
     * The {@code bold,italic} combination has the following aliases:
     * <ul>
     *     <li>{@code _bold_}</li>
     *     <li>{@code *italic*}</li>
     *     <li>{@code ITALIC}</li>
     * </ul>
     * <p></p>
     * When {@code bold} is used without any other color it is an alias for {@code default,bold}.
     * <p></p>
     * The {@code negative} text color and the {@code bg_negative} background color are identical: they invert *both* the default
     * text color and the background color.
     * <p></p>
     * <h3>Portability</h3>
     * <p></p>
     * Most terminals provide mappings between the standard color names used here and what they actually render. So, for example,
     * you may declare {@code red} but a terminal <em>could</em> be configured to render it as blue; generally, though, themes
     * will use a reasonably close variant of the pure color.
     * <p></p>
     * Where things get interesting is when a color matches (or closely matches) the terminal background color: any use of that
     * color will fade or disappear entirely. The common cases are with {@code white} or {@code bg_white} on a light theme and
     * {@code black} or {@code bg_black} on a dark theme. While explicit use of {@code white} may work well in <em>your</em>
     * terminal, it won't work for everyone; if this matters in your use case...
     * <p></p>
     * The portability problem can be addressed by using these special colors in place of any white or black style:
     *  <ul>
     *      <li>{@code default} selects the default text color in the current theme</li>
     *      <li>{@code bold} selects the bold variant of the default text color</li>
     *      <li>{@code negative} inverts the default text <em>and</em> background colors</li>
     *      <li>{@code bg_negative} an alias for {@code negative}</li>
     *      <li>{@code bg_default} selects the default background color in the current theme</li>
     *  </ul>
     * <p></p>
     * Finally, {@code strikethrough}, (the really annoying) {@code blink} and {@code conceal} may not be enabled or supported in
     * every terminal and may do nothing. For {@code conceal}, presumably you can just leave out whatever you don't want shown; for
     * the other two best to assume they don't work and use them only as <em>additional</em> emphasis.
     *
     * @param name The name.
     * @return The style or {@link #none} if styles are disabled.
     */
    public static Style named(String name) {
        return named(name, false);
    }

    /**
     * Returns the style for the given name, or optionally fails if not present.
     *
     * @param name The name.
     * @param required {@code true} if required.
     * @return The style, or {@link #none()} if styles are disabled of if name not found and not required.
     * @throws IllegalArgumentException If required and name is not found.
     */
    public static Style named(String name, boolean required) {
        final Style style = STYLES.get(name);
        if (style == null) {
            if (required) {
                throw new IllegalArgumentException("Unknown style: " + name);
            } else {
                return NONE;
            }
        }
        return ENABLED ? style : NONE;
    }

    /**
     * Returns a list of all color names.
     *
     * @return The names.
     */
    public static List<String> colorNames() {
        return List.of("red", "yellow", "green", "cyan", "blue", "magenta", "white", "black", "default", "bold", "negative");
    }

    /**
     * Returns a list of all background color names.
     *
     * @return The names.
     */
    public static List<String> backgroundColorNames() {
        return List.of("bg_red", "bg_yellow", "bg_green", "bg_cyan", "bg_blue", "bg_magenta", "bg_white", "bg_black",
                       "bg_default", "bg_negative");
    }

    /**
     * Returns a list of all emphasis names.
     *
     * @return The names.
     */
    public static List<String> emphasisNames() {
        return List.of("italic", "bold", "faint", "plain", "underline", "strikethrough", "negative", "conceal", "blink");
    }

    /**
     * Returns a style composed from the given names, or {@link #none} if empty.
     *
     * @param names The names.
     * @return The style.
     */
    public static Style of(String... names) {
        if (names.length == 0) {
            return NONE;
        } else if (names.length == 1) {
            return Style.named(names[0]);
        } else {
            return new StyleList(names);
        }
    }

    /**
     * Returns a style from the given color and attributes.
     *
     * @param color The color.
     * @param background {@code true} if background color.
     * @param bright {@code true} if bright color.
     * @return The style.
     */
    public static Style of(Color color, boolean background, boolean bright) {
        return new Hue(color, background, bright);
    }

    /**
     * Returns a style composed from the given attributes, or {@link #none} if empty.
     *
     * @param attributes The attributes.
     * @return The style.
     */
    public static Style of(Attribute... attributes) {
        if (attributes.length == 0) {
            return NONE;
        } else if (attributes.length == 1) {
            return new Emphasis(attributes[0]);
        } else {
            return new StyleList(attributes);
        }
    }

    /**
     * Returns a style composed from the given styles, or {@link #none} if empty.
     *
     * @param styles The styles.
     * @return The style.
     */
    public static Style of(Style... styles) {
        if (styles.length == 0) {
            return NONE;
        } else if (styles.length == 1) {
            return styles[0];
        } else {
            return new StyleList(styles);
        }
    }

    /**
     * Tests whether or not the given text contains an Ansi escape sequence.
     *
     * @param text The text.
     * @return {@code true} if an Ansi escape sequence found.
     */
    public static boolean isStyled(String text) {
        return text != null && text.contains(ANSI_ESCAPE_BEGIN);
    }

    /**
     * Strips any styles from the given string.
     *
     * @param input The string.
     * @return The stripped string.
     */
    public static String strip(String input) {
        STRIP_LOCK.lock();
        try {
            STRIP_BYTES.reset();
            STRIP.write(input.getBytes(UTF_8));
            return new String(STRIP_BYTES.toByteArray(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            STRIP_LOCK.unlock();
        }
    }


    /**
     * Log styles either as a complete list (including aliases) or a summary table.
     *
     * @param args The arguments: {@code --list | --table}. Defaults to table.
     */
    public static void main(String... args) {
        boolean list = false;
        if (args.length == 1) {
            if (args[0].equals("--list")) {
                list = true;
            } else if (!args[0].equals("--table")) {
                throw new IllegalArgumentException("Unknown argument: " + args[0]);
            }
        }

        if (list) {
            styles().forEach((name, style) -> Log.info("%14s [ %s ]", name, style.apply("example")));
        } else {
            logSummaryTables();
        }
    }

    /**
     * Log a summary tables of text and background colors and styles.
     */
    public static void logSummaryTables() {
        Log.info();
        logTextSummaryTable();
        Log.info();
        logBackgroundSummaryTable();
        Log.info();
    }

    /**
     * Log a summary table of text colors and styles.
     */
    public static void logTextSummaryTable() {
        logTable(colorNames(), false);
    }

    /**
     * Log a summary table of background colors and styles.
     */
    public static void logBackgroundSummaryTable() {
        logTable(backgroundColorNames(), true);
    }

    private static void logTable(List<String> names, boolean background) {
        String header = background ? "Background Color" : "Text Color";
        String example = " Example 1234 !@#$% ";
        String rowFormat = "│ %-19s│ %22s │ %22s │ %22s │ %22s │";
        Log.info("┌────────────────────┬──────────────────────┬──────────────────────┬──────────────────────┬───────────"
                 + "───────────┐");
        Log.info("│ %-19s│        Plain         │        Italic        │         Bold         │    Italic & Bold     │",
                 header);
        Log.info("├────────────────────┼──────────────────────┼──────────────────────┼──────────────────────┼───────────"
                 + "───────────┤");
        names.forEach(name -> {
            String textColor = background ? "default" : name;
            String backgroundColor = background ? name : "bg_default";

            String textColorBright = background ? textColor : textColor + "!";
            String backgroundColorBright = background ? backgroundColor + "!" : backgroundColor;

            String plain = Style.of(backgroundColor, textColor).apply(example);
            String italic = Style.of(backgroundColor, textColor, "italic").apply(example);
            String bold = Style.of(backgroundColor, textColor, "bold").apply(example);
            String italicBold = Style.of(backgroundColor, textColor, "ITALIC").apply(example);

            String plainBright = Style.of(backgroundColorBright, textColorBright).apply(example);
            String italicBright = Style.of(backgroundColorBright, textColorBright, "italic").apply(example);
            String boldBright = Style.of(backgroundColorBright, textColorBright, "bold").apply(example);
            String italicBoldBright = Style.of(backgroundColorBright, textColorBright, "ITALIC").apply(example);

            Log.info(rowFormat, name, plain, italic, bold, italicBold);
            Log.info(rowFormat, name + "!", plainBright, italicBright, boldBright, italicBoldBright);
        });
        Log.info("└────────────────────┴──────────────────────┴──────────────────────┴──────────────────────┴────────────"
                 + "──────────┘");
    }

    /**
     * Returns this style applied to the given text.
     *
     * @param text The text.
     * @return The new text.
     */
    public String apply(Object text) {
        return apply(ansi()).a(text).reset().toString();
    }

    /**
     * Applies this style to the given ansi instance.
     *
     * @param ansi The instance.
     * @return The instance, for chaining.
     */
    public Ansi apply(Ansi ansi) {
        return ansi;
    }

    /**
     * Reset an ansi instance.
     *
     * @param ansi The instance.
     * @return The instance, for chaining.
     */
    public Ansi reset(Ansi ansi) {
        return ansi;
    }

    @Override
    public String toString() {
        return "none";
    }

    static class StyleList extends Style {
        private final List<Style> styles = new ArrayList<>();

        StyleList(String... names) {
            for (String name : names) {
                add(name);
            }
        }

        StyleList(Attribute... attributes) {
            for (Attribute attribute : attributes) {
                add(attribute);
            }
        }

        StyleList(Style... styles) {
            for (Style style : styles) {
                add(style);
            }
        }

        StyleList add(String name) {
            add(Style.named(name));
            return this;
        }

        StyleList add(Attribute attribute) {
            add(new Emphasis(attribute));
            return this;
        }

        StyleList add(Style style) {
            styles.add(style);
            return this;
        }

        int size() {
            return styles.size();
        }

        Style pop() {
            if (styles.isEmpty()) {
                return none();
            } else {
                return styles.remove(size() - 1);
            }
        }

        @Override
        public Ansi apply(Ansi ansi) {
            for (Style style : styles) {
                style.apply(ansi);
            }
            return ansi;
        }

        @Override
        public Ansi reset(Ansi ansi) {
            return ansi.reset();
        }

        @Override
        public String toString() {
            return styles.toString();
        }
    }

    static class Hue extends Style {
        private final Color color;
        private final boolean background;
        private final boolean bright;

        Hue(Color color, boolean background, boolean bright) {
            this.color = requireNonNull(color);
            this.background = background;
            this.bright = bright;
        }

        @Override
        public Ansi apply(Ansi ansi) {
            if (background) {
                if (bright) {
                    ansi.bgBright(color);
                } else {
                    ansi.bg(color);
                }
            } else {
                if (bright) {
                    ansi.fgBright(color);
                } else {
                    ansi.fg(color);
                }
            }
            return ansi;
        }

        @Override
        public Ansi reset(Ansi ansi) {
            return ansi.reset();
        }

        @Override
        public String toString() {
            return color
                   + ", background="
                   + background
                   + ", bright="
                   + bright;
        }
    }

    static class Emphasis extends Style {
        private final Attribute attribute;

        Emphasis(Attribute attribute) {
            this.attribute = requireNonNull(attribute);
        }

        @Override
        public Ansi apply(Ansi ansi) {
            ansi.a(attribute);
            return ansi;
        }

        @Override
        public Ansi reset(Ansi ansi) {
            return ansi.reset();
        }

        @Override
        public String toString() {
            return attribute.toString();
        }
    }

    private static Map<String, Style> stylesByName() {
        final Map<String, Style> styles = new LinkedHashMap<>();

        // None

        styles.put("none", Style.none());
        styles.put("bg_none", Style.none());

        // Hues and aliases

        colorNames().stream().filter(name -> !name.equals("bold")).forEach(lowerName -> {

            // Text colors and aliases

            final boolean negative = lowerName.equals("negative");
            final String upperName = lowerName.toUpperCase(Locale.ENGLISH);
            final Color color = negative ? null : Color.valueOf(upperName);
            final Style basic = negative ? NEGATIVE : Style.of(color, false, false);
            final Style bright = negative ? NEGATIVE : Style.of(color, false, true);
            final Style bold = Style.of(BOLD, basic);
            final Style italic = Style.of(ITALIC, basic);
            final Style italicBold = Style.of(BOLD_ITALIC, basic);
            final Style boldBright = Style.of(BOLD, bright);
            final Style italicBright = Style.of(bright, ITALIC);
            final Style italicBoldBright = Style.of(BOLD, ITALIC, bright);

            styles.put(lowerName, basic);

            styles.put("*" + lowerName + "*", italic);
            styles.put("_" + lowerName + "_", italic);

            styles.put("**" + lowerName + "**", bold);
            styles.put("__" + lowerName + "__", bold);
            styles.put(upperName, bold);

            styles.put("**_" + lowerName + "_**", italicBold);
            styles.put("__*" + lowerName + "*__", italicBold);
            styles.put("_" + upperName + "_", italicBold);
            styles.put("*" + upperName + "*", italicBold);

            styles.put(lowerName + "!", bright);

            styles.put("*" + lowerName + "*!", italicBright);
            styles.put("_" + lowerName + "_!", italicBright);

            styles.put("**" + lowerName + "**!", boldBright);
            styles.put("__" + lowerName + "__!", boldBright);
            styles.put(upperName + "!", boldBright);

            styles.put("**_" + lowerName + "_**!", italicBoldBright);
            styles.put("__*" + lowerName + "*__!", italicBoldBright);
            styles.put("_" + upperName + "_!", italicBoldBright);
            styles.put("*" + upperName + "*!", italicBoldBright);

            // Background colors

            styles.put("bg_" + lowerName, negative ? NEGATIVE : Style.of(color, true, false));
            styles.put("bg_" + lowerName + "!", negative ? NEGATIVE : Style.of(color, true, true));
        });

        // Emphasis and aliases

        styles.put("bold", BOLD);
        styles.put("BOLD", BOLD);

        styles.put("italic", ITALIC);

        styles.put("*bold*", BOLD_ITALIC);
        styles.put("_bold_", BOLD_ITALIC);
        styles.put("*BOLD*", BOLD_ITALIC);
        styles.put("_BOLD_", BOLD_ITALIC);
        styles.put("**italic**", BOLD_ITALIC);
        styles.put("__italic__", BOLD_ITALIC);
        styles.put("ITALIC", BOLD_ITALIC);

        styles.put("plain", PLAIN);
        styles.put("faint", FAINT);
        styles.put("underline", Style.of(Attribute.UNDERLINE));
        styles.put("strikethrough", Style.of(Attribute.STRIKETHROUGH_ON));
        styles.put("blink", Style.of(Attribute.BLINK_SLOW));
        styles.put("conceal", Style.of(Attribute.CONCEAL_ON));

        return styles;
    }
}
