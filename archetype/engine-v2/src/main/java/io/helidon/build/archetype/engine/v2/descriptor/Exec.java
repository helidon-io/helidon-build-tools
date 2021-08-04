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
 * Archetype exec.
 */
public class Exec {

    private final String url;
    private final String src;

    Exec(String url, String src) {
        this.url = url;
        this.src = src;
    }

    /**
     * Get the url.
     *
     * @return url as a String
     */
    public String url() {
        return url;
    }

    /**
     * Get the source.
     *
     * @return source as a String
     */
    public String src() {
        return src;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Exec e = (Exec) o;
        return url.equals(e.url)
                && src.equals(e.src);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, src);
    }

    @Override
    public String toString() {
        return "Exec{"
                + "url=" + url()
                + "src=" + src()
                + '}';
    }
}
