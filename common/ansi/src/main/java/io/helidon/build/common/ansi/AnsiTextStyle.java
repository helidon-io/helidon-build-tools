/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.helidon.build.common.Log;
import io.helidon.build.common.RichText;
import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.RichTextStyle;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

import static io.helidon.build.common.ansi.AnsiTextProvider.ANSI_ENABLED;
import static java.util.Objects.requireNonNull;

/**
 * Ansi rich text styles.
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public class AnsiTextStyle implements RichTextStyle {
    private static final AnsiTextStyle NONE = new AnsiTextStyle();
    private static final AnsiTextStyle PLAIN = new Emphasis(Attribute.RESET);
    private static final AnsiTextStyle BOLD = new Emphasis(Attribute.INTENSITY_BOLD);
    private static final AnsiTextStyle ITALIC = new Emphasis(Attribute.ITALIC);
    private static final AnsiTextStyle FAINT = new Emphasis(Attribute.INTENSITY_FAINT);
    private static final StyleList BOLD_ITALIC = new StyleList(BOLD).add(ITALIC);
    private static final AnsiTextStyle NEGATIVE = new Emphasis(Attribute.NEGATIVE_ON);
    private static final Map<String, RichTextStyle> STYLES = stylesByName();

    private static final char ESC_CH1 = '\033';
    private static final char ESC_CH2 = '[';
    private static final char CMD_CH2 = ']';
    private static final char BEL = 7;
    private static final char ST_CH2 = '\\';
    private static final char CHARSET0_CH2 = '(';
    private static final char CHARSET1_CH2 = ')';
    private static final String ANSI_ESCAPE_BEGIN = "" + ESC_CH1 + ESC_CH2;

    private enum AnsiState {
        ESC1,
        ESC2,
        NEXT_ARG,
        STR_ARG_END,
        INT_ARG_END,
        CMD,
        CMD_END,
        CMD_PARAM,
        ST,
        CHARSET
    }

    /**
     * Return all styles, by name.
     *
     * @return The styles. Not immutable, so may be (carefully!) modified.
     */
    public static Map<String, RichTextStyle> styles() {
        return STYLES;
    }

    /**
     * Returns a no-op style.
     *
     * @return The style.
     */
    public static AnsiTextStyle none() {
        return NONE;
    }

    /**
     * Returns the style for the given name.
     * <br><br>
     * <h4>Text Color Names</h4>
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
     * <br><br>
     * See Portability below for more on {@code default}, {@code bold} and {@code negative}.
     * <br><br>
     * <h4>Background Color Names</h4>
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
     * <br><br>
     * <h4>Emphasis Names</h4>
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
     * <br><br>
     * <h4>Aliases</h4>
     * <br><br>
     * Every text color has the following aliases:
     * <ul>
     *      <li>Bold variant with an uppercase name (e.g. {@code RED})</li>
     *      <li>Bold variant with {@code '*'} prefix and suffix (e.g. {@code *red*})</li>
     *      <li>Italic variant with {@code '_'} prefix and suffix (e.g. {@code _red_})</li>
     *      <li>Bold italic variant with {@code '_*'} prefix and {@code '*_'} suffix (e.g. {@code _*red*_} or {@code *_red_*})</li>
     *      <li>Bright variants of the color and all the above with a {@code '!'} suffix
     *      (e.g. {@code red!}, {@code RED!}, {@code *red*!}, {@code _red_!}</li>
     * </ul>
     * <br><br>
     * Every background color has the following aliases:
     * <ul>
     *     <li> Bright variants with a {@code '!'} suffix (e.g. {@code bg_yellow!})</li>
     * </ul>
     * <br><br>
     * The {@code bold,italic} combination has the following aliases:
     * <ul>
     *     <li>{@code _bold_}</li>
     *     <li>{@code *italic*}</li>
     *     <li>{@code ITALIC}</li>
     * </ul>
     * <br><br>
     * When {@code bold} is used without any other color it is an alias for {@code default,bold}.
     * <br><br>
     * The {@code negative} text color and the {@code bg_negative} background color are identical: they invert *both* the default
     * text color and the background color.
     * <br><br>
     * <h4>Portability</h4>
     * <br><br>
     * Most terminals provide mappings between the standard color names used here and what they actually render. So, for example,
     * you may declare {@code red} but a terminal <em>could</em> be configured to render it as blue; generally, though, themes
     * will use a reasonably close variant of the pure color.
     * <br><br>
     * Where things get interesting is when a color matches (or closely matches) the terminal background color: any use of that
     * color will fade or disappear entirely. The common cases are with {@code white} or {@code bg_white} on a light theme and
     * {@code black} or {@code bg_black} on a dark theme. While explicit use of {@code white} may work well in <em>your</em>
     * terminal, it won't work for everyone; if this matters in your use case...
     * <br><br>
     * The portability problem can be addressed by using these special colors in place of any white or black style:
     *  <ul>
     *      <li>{@code default} selects the default text color in the current theme</li>
     *      <li>{@code bold} selects the bold variant of the default text color</li>
     *      <li>{@code negative} inverts the default text <em>and</em> background colors</li>
     *      <li>{@code bg_negative} an alias for {@code negative}</li>
     *      <li>{@code bg_default} selects the default background color in the current theme</li>
     *  </ul>
     * <br><br>
     * Finally, {@code strikethrough}, (the really annoying) {@code blink} and {@code conceal} may not be enabled or supported in
     * every terminal and may do nothing. For {@code conceal}, presumably you can just leave out whatever you don't want shown; for
     * the other two best to assume they don't work and use them only as <em>additional</em> emphasis.
     *
     * @param name The name.
     * @return The style or {@link RichTextStyle#NONE} if styles are disabled.
     */
    public static RichTextStyle named(String name) {
        return named(name, false);
    }

    /**
     * Returns the style for the given name, or optionally fails if not present.
     *
     * @param name     The name.
     * @param required {@code true} if required.
     * @return The style, or {@link RichTextStyle#NONE} if styles are disabled of if name not found and not required.
     * @throws IllegalArgumentException If required and name is not found.
     */
    public static RichTextStyle named(String name, boolean required) {
        final RichTextStyle style = STYLES.get(name);
        if (style == null) {
            if (required) {
                throw new IllegalArgumentException("Unknown style: " + name);
            } else {
                return NONE;
            }
        }
        return ANSI_ENABLED.instance() ? style : NONE;
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
    @SuppressWarnings("unused")
    public static List<String> emphasisNames() {
        return List.of("italic", "bold", "faint", "plain", "underline", "strikethrough", "negative", "conceal", "blink");
    }

    /**
     * Returns a style from the given color and attributes.
     *
     * @param color      The color.
     * @param background {@code true} if background color.
     * @param bright     {@code true} if bright color.
     * @return The style.
     */
    public static AnsiTextStyle of(Color color, boolean background, boolean bright) {
        return new Hue(color, background, bright);
    }

    /**
     * Returns a style composed of the given attributes, or {@link #none} if empty.
     *
     * @param attributes The attributes.
     * @return The style.
     */
    public static RichTextStyle of(Attribute... attributes) {
        if (attributes.length == 0) {
            return NONE;
        } else if (attributes.length == 1) {
            return new Emphasis(attributes[0]);
        } else {
            return new StyleList(Emphasis::new, attributes);
        }
    }

    /**
     * Tests if the given text contains an Ansi escape sequence.
     *
     * @param text The text.
     * @return {@code true} if an Ansi escape sequence found.
     */
    public static boolean isStyled(String text) {
        return text != null && (text.contains(ANSI_ESCAPE_BEGIN) || text.contains(RichTextRenderer.START_TOKEN));
    }

    /**
     * Strips any styles from the given string.
     *
     * @param input The string.
     * @return The stripped string.
     */
    public static String strip(String input) {
        AnsiState state = AnsiState.ESC1;
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[100];
        int pos = 0;
        int index;
        for (index = 0; index < input.length(); index++) {
            char c = input.charAt(index);
            switch (state) {
                case ESC1:
                    if (c == ESC_CH1) {
                        buffer[pos++] = c;
                        state = AnsiState.ESC2;
                    } else {
                        sb.append(c);
                    }
                    break;

                case ESC2:
                    buffer[pos++] = c;
                    if (c == ESC_CH2) {
                        state = AnsiState.NEXT_ARG;
                    } else if (c == CMD_CH2) {
                        state = AnsiState.CMD;
                    } else if (c == CHARSET0_CH2) {
                        state = AnsiState.CHARSET;
                    } else if (c == CHARSET1_CH2) {
                        state = AnsiState.CHARSET;
                    } else {
                        sb.append(buffer, 0, pos);
                        pos = 0;
                        state = AnsiState.ESC1;
                    }
                    break;

                case NEXT_ARG:
                    buffer[pos++] = c;
                    if ('"' == c) {
                        state = AnsiState.STR_ARG_END;
                    } else if ('0' <= c && c <= '9') {
                        state = AnsiState.INT_ARG_END;
                    } else if (c != ';' && c != '?' && c != '=') {
                        pos = 0;
                        state = AnsiState.ESC1;
                    }
                    break;

                case INT_ARG_END:
                    buffer[pos++] = c;
                    if (!('0' <= c && c <= '9')) {
                        if (c == ';') {
                            state = AnsiState.NEXT_ARG;
                        } else {
                            pos = 0;
                            state = AnsiState.ESC1;
                        }
                    }
                    break;

                case STR_ARG_END:
                    buffer[pos++] = c;
                    if ('"' != c) {
                        if (c == ';') {
                            state = AnsiState.NEXT_ARG;
                        } else {
                            pos = 0;
                            state = AnsiState.ESC1;
                        }
                    }
                    break;

                case CMD:
                    buffer[pos++] = c;
                    if ('0' <= c && c <= '9') {
                        state = AnsiState.CMD_END;
                    } else {
                        sb.append(buffer, 0, pos);
                        pos = 0;
                        state = AnsiState.ESC1;
                    }
                    break;

                case CMD_END:
                    buffer[pos++] = c;
                    if (';' == c) {
                        state = AnsiState.CMD_PARAM;
                    } else if (!('0' <= c && c <= '9')) {
                        // oops, did not expect this
                        sb.append(buffer, 0, pos);
                        pos = 0;
                        state = AnsiState.ESC1;
                    }
                    break;

                case CMD_PARAM:
                    buffer[pos++] = c;
                    if (BEL == c) {
                        pos = 0;
                        state = AnsiState.ESC1;
                    } else if (ESC_CH1 == c) {
                        state = AnsiState.ST;
                    }
                    break;

                case ST:
                    buffer[pos++] = c;
                    if (ST_CH2 == c) {
                        pos = 0;
                        state = AnsiState.ESC1;
                    } else {
                        state = AnsiState.CMD_PARAM;
                    }
                    break;

                case CHARSET:
                    pos = 0;
                    state = AnsiState.ESC1;
                    break;

                default:
                    break;
            }

            // Is it just too long?
            if (index >= buffer.length) {
                sb.append(buffer, 0, pos);
                pos = 0;
                state = AnsiState.ESC1;
            }
        }
        return sb.toString();
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

            //noinspection DuplicatedCode
            String plain = RichTextStyle.of(backgroundColor, textColor).apply(example);
            String italic = RichTextStyle.of(backgroundColor, textColor, "italic").apply(example);
            String bold = RichTextStyle.of(backgroundColor, textColor, "bold").apply(example);
            String italicBold = RichTextStyle.of(backgroundColor, textColor, "ITALIC").apply(example);

            //noinspection DuplicatedCode
            String plainBright = RichTextStyle.of(backgroundColorBright, textColorBright).apply(example);
            String italicBright = RichTextStyle.of(backgroundColorBright, textColorBright, "italic").apply(example);
            String boldBright = RichTextStyle.of(backgroundColorBright, textColorBright, "bold").apply(example);
            String italicBoldBright = RichTextStyle.of(backgroundColorBright, textColorBright, "ITALIC").apply(example);

            Log.info(rowFormat, name, plain, italic, bold, italicBold);
            Log.info(rowFormat, name + "!", plainBright, italicBright, boldBright, italicBoldBright);
        });
        Log.info("└────────────────────┴──────────────────────┴──────────────────────┴──────────────────────┴────────────"
                + "──────────┘");
    }

    @Override
    public String toString() {
        return "none";
    }

    @Override
    public RichText apply(RichText richText) {
        return richText;
    }

    @Override
    public RichText reset(RichText richText) {
        return richText;
    }

    static class Hue extends AnsiTextStyle {
        private final Color color;
        private final boolean background;
        private final boolean bright;

        Hue(Color color, boolean background, boolean bright) {
            this.color = requireNonNull(color);
            this.background = background;
            this.bright = bright;
        }

        @Override
        public RichText apply(RichText richText) {
            Ansi ansi = ((AnsiText) richText).ansi();
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
            return richText;
        }

        @Override
        public RichText reset(RichText richText) {
            return richText.reset();
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

    static class Emphasis extends AnsiTextStyle {
        private final Attribute attribute;

        Emphasis(Attribute attribute) {
            this.attribute = requireNonNull(attribute);
        }

        @Override
        public RichText apply(RichText richText) {
            Ansi ansi = ((AnsiText) richText).ansi();
            ansi.a(attribute);
            return richText;
        }

        @Override
        public RichText reset(RichText ansi) {
            return ansi.reset();
        }

        @Override
        public String toString() {
            return attribute.toString();
        }
    }

    private static Map<String, RichTextStyle> stylesByName() {
        final Map<String, RichTextStyle> styles = new LinkedHashMap<>();

        // None

        styles.put("none", AnsiTextStyle.none());
        styles.put("bg_none", AnsiTextStyle.none());

        // Hues and aliases

        colorNames().stream().filter(name -> !name.equals("bold")).forEach(lowerName -> {

            // Text colors and aliases

            final boolean negative = lowerName.equals("negative");
            final String upperName = lowerName.toUpperCase(Locale.ENGLISH);
            final Color color = negative ? null : Color.valueOf(upperName);
            final RichTextStyle basic = negative ? NEGATIVE : AnsiTextStyle.of(color, false, false);
            final RichTextStyle bright = negative ? NEGATIVE : AnsiTextStyle.of(color, false, true);
            final RichTextStyle bold = RichTextStyle.of(BOLD, basic);
            final RichTextStyle italic = RichTextStyle.of(ITALIC, basic);
            final RichTextStyle italicBold = RichTextStyle.of(BOLD_ITALIC, basic);
            final RichTextStyle boldBright = RichTextStyle.of(BOLD, bright);
            final RichTextStyle italicBright = RichTextStyle.of(bright, ITALIC);
            final RichTextStyle italicBoldBright = RichTextStyle.of(BOLD, ITALIC, bright);

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

            styles.put("bg_" + lowerName, negative ? NEGATIVE : AnsiTextStyle.of(color, true, false));
            styles.put("bg_" + lowerName + "!", negative ? NEGATIVE : AnsiTextStyle.of(color, true, true));
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
        styles.put("underline", AnsiTextStyle.of(Attribute.UNDERLINE));
        styles.put("strikethrough", AnsiTextStyle.of(Attribute.STRIKETHROUGH_ON));
        styles.put("blink", AnsiTextStyle.of(Attribute.BLINK_SLOW));
        styles.put("conceal", AnsiTextStyle.of(Attribute.CONCEAL_ON));

        return styles;
    }
}
