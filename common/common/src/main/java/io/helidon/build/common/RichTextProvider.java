/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Rich text provider.
 */
public interface RichTextProvider {

    /**
     * Create a new {@link RichText} instance.
     *
     * @return RichText instance
     */
    RichText richText();

    /**
     * Get the rich text style for the given style names.
     *
     * @param names style names
     * @return RichTextStyle
     */
    RichTextStyle styleOf(String... names);

    /**
     * Convert this provider into a provider of a different type.
     *
     * @param aClass provider class
     * @param <T>    provider class
     * @return Optional
     */
    default <T extends RichTextProvider> Optional<T> as(Class<T> aClass) {
        return Optional.of(this)
                       .filter(aClass::isInstance)
                       .map(aClass::cast);
    }

    /**
     * Lazy initialization for the loaded provider.
     */
    final class Holder {

        private Holder() {
        }

        /**
         * The loaded provider.
         */
        public static final RichTextProvider INSTANCE =
                ServiceLoader.load(RichTextProvider.class, RichTextProvider.class.getClassLoader())
                             .findFirst()
                             .orElse(DefaultProvider.INSTANCE);
    }

    /**
     * Default implementation of {@link RichTextProvider}.
     */
    final class DefaultProvider implements RichTextProvider {

        /**
         * Singleton instance.
         */
        static final DefaultProvider INSTANCE = new DefaultProvider();

        private DefaultProvider() {
        }

        @Override
        public RichText richText() {
            return new DefaultRichText();
        }

        @Override
        public RichTextStyle styleOf(String... names) {
            return RichTextStyle.NONE;
        }

        private static final class DefaultRichText implements RichText {

            private final StringBuilder sb = new StringBuilder();

            @Override
            public RichText append(CharSequence value, int start, int end) {
                sb.append(value, start, end);
                return this;
            }

            @Override
            public RichText append(String value) {
                sb.append(value);
                return this;
            }

            @Override
            public String text() {
                return sb.toString();
            }

            @Override
            public RichText reset() {
                return this;
            }
        }
    }
}
