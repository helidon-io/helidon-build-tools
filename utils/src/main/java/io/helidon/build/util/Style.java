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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;

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
    private static final Style DEFAULT_BRIGHT = new Hue(Ansi.Color.DEFAULT, false, true);
    private static final Style BOLD_ITALIC = new StyleList(BOLD).add(ITALIC);
    private static final Style DEFAULT_BRIGHT_ITALIC = new StyleList(DEFAULT_BRIGHT).add(ITALIC);
    private static final Style DEFAULT_BOLD_BRIGHT = new StyleList(BOLD).add(DEFAULT_BRIGHT);
    private static final Style DEFAULT_BOLD_BRIGHT_ITALIC = new StyleList(BOLD).add(DEFAULT_BRIGHT).add(ITALIC);
    private static final boolean ENABLED = AnsiConsoleInstaller.install();
    private static final String ANSI_ESCAPE_BEGIN = "\033[";
    private static final Map<String, Style> STYLES = stylesByName();

    /**
     * Log all styles, by name.
     * @param args Arguments. None supported.
     */
    public static void main(String[] args) {
        styles().forEach((name, style) -> Log.info("%14s [ %s ]", name, style.apply("example")));
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
     *     <li>{@code green}</li>
     *     <li>{@code yellow}</li>
     *     <li>{@code blue}</li>
     *     <li>{@code magenta}</li>
     *     <li>{@code cyan}</li>
     *     <li>{@code white}</li>
     *     <li>{@code black}</li>
     *     <li>{@code default}</li>
     * </ul>
     * <p></p>
     * <h3>Background Color Names</h3>
     * <ul>
     *     <li>{@code bg_red}</li>
     *     <li>{@code bg_green}</li>
     *     <li>{@code bg_yellow}</li>
     *     <li>{@code bg_blue}</li>
     *     <li>{@code bg_magenta}</li>
     *     <li>{@code bg_cyan}</li>
     *     <li>{@code bg_white}</li>
     *     <li>{@code bg_black}</li>
     *     <li>{@code bg_default}</li>
     *     <li>{@code bg_negative}</li>
     * </ul>
     * <p></p>
     * <h3>Emphasis Names</h3>
     * <ul>
     *     <li>{@code bold}</li>
     *     <li>{@code faint}</li>
     *     <li>{@code plain}</li>
     *     <li>{@code italic}</li>
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
     * The portability problem can be addressed by using these special styles in place of any white or black style:
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
    public static Style byName(String name) {
        if (ENABLED) {
            final Style style = STYLES.get(name);
            return style == null ? NONE : style;
        } else {
            return NONE;
        }
    }

    /**
     * Returns the style for the given name, or fails if not present.
     *
     * @param name The name.
     * @return The style.
     * @throws IllegalArgumentException If name is not found.
     */
    public static Style byRequiredName(String name) {
        final Style style = STYLES.get(name);
        if (style == null) {
            throw new IllegalArgumentException("Unknown style: " + name);
        }
        return style;
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
            return Style.byName(names[0]);
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
    public static Style of(Ansi.Color color, boolean background, boolean bright) {
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
            add(Style.byName(name));
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
        private final Ansi.Color color;
        private final boolean background;
        private final boolean bright;

        Hue(Ansi.Color color, boolean background, boolean bright) {
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

        // Hues and aliases

        for (Ansi.Color color : Ansi.Color.values()) {

            // Text colors

            final Style basic = Style.of(color, false, false);
            final Style bright = Style.of(color, false, true);
            final String lowerName = color.name().toLowerCase(Locale.ENGLISH);
            final String upperName = lowerName.toUpperCase(Locale.ENGLISH);
            addWithAliases(lowerName, upperName, basic, bright, styles);

            // Background colors

            styles.put("bg_" + lowerName, Style.of(color, true, false));
            styles.put("bg_" + lowerName + "!", Style.of(color, true, true));
        }

        // Emphasis and aliases

        styles.put("bold", BOLD);
        styles.put("BOLD", BOLD);
        styles.put("italic", ITALIC);

        styles.put("_bold_", BOLD_ITALIC);
        styles.put("*italic*", BOLD_ITALIC);
        styles.put("ITALIC", BOLD_ITALIC);

        styles.put("plain", PLAIN);
        styles.put("faint", FAINT);
        styles.put("underline", Style.of(Attribute.UNDERLINE));
        styles.put("strikethrough", Style.of(Attribute.STRIKETHROUGH_ON));
        styles.put("blink", Style.of(Attribute.BLINK_SLOW));
        styles.put("conceal", Style.of(Attribute.CONCEAL_ON));

        // Negative text/background

        final Style negative = Style.of(Attribute.NEGATIVE_ON);
        styles.put("negative", negative);
        styles.put("bg_negative", negative);

        return styles;
    }

    private static void addWithAliases(String lowerName, String upperName, Style style, Style bright, Map<String, Style> styles) {
        final Style bold = style == BOLD ? BOLD : Style.of(BOLD, style);
        final Style italic = style == ITALIC ? ITALIC : Style.of(ITALIC, style);
        final Style boldItalic = style == BOLD || style == ITALIC ? BOLD_ITALIC : Style.of(BOLD_ITALIC, style);
        final Style boldBright = style == BOLD ? DEFAULT_BOLD_BRIGHT : Style.of(BOLD, bright);
        final Style brightItalic = style == ITALIC ? DEFAULT_BRIGHT_ITALIC : Style.of(bright, ITALIC);
        final Style boldBrightItalic = style == BOLD || style == ITALIC ? DEFAULT_BOLD_BRIGHT_ITALIC
                : Style.of(BOLD, ITALIC, bright);

        styles.put(lowerName, style);
        styles.put(lowerName + "!", bright);

        styles.put("*" + lowerName + "*", bold);
        styles.put(upperName, bold);

        styles.put("*" + lowerName + "*!", boldBright);
        styles.put(upperName + "!", boldBright);

        styles.put("_" + lowerName + "_", italic);
        styles.put("_" + lowerName + "_!", brightItalic);

        styles.put("_*" + lowerName + "*_", boldItalic);
        styles.put("*_" + lowerName + "_*", boldItalic);
        styles.put("_" + upperName + "_", boldItalic);

        styles.put("_*" + lowerName + "*_!", boldBrightItalic);
        styles.put("*_" + lowerName + "_*!", boldBrightItalic);
        styles.put("_" + upperName + "_!", boldBrightItalic);
    }
}
