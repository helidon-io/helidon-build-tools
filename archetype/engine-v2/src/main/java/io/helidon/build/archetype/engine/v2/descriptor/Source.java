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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.Objects;

/**
 * Archetype source.
 */
public class Source {

    private final String src;
    private final String url;

    /**
     * Constructor.
     * @param url The url.
     * @param source The source path.
     */
    public Source(String url, String source) {
        this.src = source;
        this.url = url;
    }

    /**
     * Get the source attribute.
     *
     * @return source
     */
    public String source() {
        return src;
    }

    /**
     * Get the url attribute.
     *
     * @return url
     */
    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Source s = (Source) o;
        return src.equals(s.src)
                && url.equals(s.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), src, url);
    }

    @Override
    public String toString() {
        return "Source{"
                + "src=" + source()
                + ", url=" + url()
                + '}';
    }
}
