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

import java.util.Locale;

/**
 * Helidon variants.
 */
public enum HelidonVariant {
    /**
     * Helidon SE.
     */
    SE("se"),

    /**
     * Helidon MP.
     */
    MP("mp");

    private final String variant;

    HelidonVariant(String variant) {
        this.variant = variant;
    }

    /**
     * Returns the variant for the given name, regardles of case.
     *
     * @param variantName The name.
     * @return The variant.
     */
    public static HelidonVariant parse(String variantName) {
        return valueOf(variantName.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public String toString() {
        return variant;
    }
}
