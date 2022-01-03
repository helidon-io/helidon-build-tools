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
package io.helidon.build.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.helidon.build.common.RichTextProvider.Holder;

/**
 * Rich text style.
 */
public interface RichTextStyle {

    /**
     * NO style constant.
     */
    RichTextStyle NONE = new RichTextStyle() {
    };

    /**
     * Returns a style composed of the given styles, or {@link #NONE} if empty.
     *
     * @param styles The styles.
     * @return The style.
     */
    static RichTextStyle of(RichTextStyle... styles) {
        if (styles.length == 0) {
            return NONE;
        } else if (styles.length == 1) {
            return styles[0];
        } else {
            return new StyleList(styles);
        }
    }

    /**
     * Invoke {@link RichTextProvider#styleOf(String...)} for the loaded provider.
     *
     * @param names The names
     * @return The style
     * @see RichTextProvider#styleOf(String...)
     */
    static RichTextStyle of(String... names) {
        return Holder.INSTANCE.styleOf(names);
    }

    /**
     * Apply this style against the given {@link RichText} instance.
     *
     * @param richText rich text to apply
     * @return the rich text passed as input
     */
    default RichText apply(RichText richText) {
        return richText;
    }

    /**
     * Returns this style applied to the given text.
     *
     * @param text The text.
     * @return The new text.
     */
    default String apply(Object text) {
        return apply(Holder.INSTANCE.richText())
                .append(String.valueOf(text))
                .reset()
                .text();
    }

    /**
     * Reset the given rich text instance.
     *
     * @param richText rich text to reset
     * @return the rich text passed as input
     */
    default RichText reset(RichText richText) {
        return richText;
    }

    /**
     * Rich text style list.
     */
    class StyleList implements RichTextStyle {

        private final List<RichTextStyle> styles = new ArrayList<>();

        /**
         * Create a new instance initialized with styles of the given names.
         *
         * @param names style names
         */
        public StyleList(String... names) {
            for (String name : names) {
                add(name);
            }
        }

        /**
         * Create a new instance initialized with the given objects and function.
         *
         * @param function function used to map each object to a style
         * @param objects  objects to be mapped as style
         */
        @SafeVarargs
        public <T> StyleList(Function<T, RichTextStyle> function, T... objects) {
            for (T obj : objects) {
                add(function.apply(obj));
            }
        }

        /**
         * Create a new instance initialized with the given styles.
         *
         * @param styles style
         */
        public StyleList(RichTextStyle... styles) {
            for (RichTextStyle style : styles) {
                add(style);
            }
        }

        /**
         * Add a style by name.
         *
         * @param name style name
         * @return this instance
         */
        public StyleList add(String name) {
            add(Holder.INSTANCE.styleOf(name));
            return this;
        }

        /**
         * Add a style.
         *
         * @param style style
         * @return this instance
         */
        public StyleList add(RichTextStyle style) {
            styles.add(style);
            return this;
        }

        /**
         * Get the list size.
         *
         * @return list size
         */
        public int size() {
            return styles.size();
        }

        /**
         * Pop the last style from the list.
         *
         * @return removed style
         */
        public RichTextStyle pop() {
            if (styles.isEmpty()) {
                return RichTextStyle.NONE;
            } else {
                return styles.remove(size() - 1);
            }
        }

        @Override
        public RichText apply(RichText ansi) {
            for (RichTextStyle style : styles) {
                style.apply(ansi);
            }
            return ansi;
        }

        @Override
        public RichText reset(RichText ansi) {
            return ansi.reset();
        }

        @Override
        public String toString() {
            return styles.toString();
        }
    }
}
