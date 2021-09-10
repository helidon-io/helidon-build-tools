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
 * Archetype value used in {@link ListType}.
 */
public class ValueType extends Conditional {

    private String value;
    private final String url;
    private final String file;
    private final String template;
    private int order = 100;

    ValueType(String url,
              String file,
              String template,
              int order,
              String ifProperties) {
        super(ifProperties);
        this.url = url;
        this.file = file;
        this.template = template;
        this.order = order;
    }

    /**
     * Get the value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Get the url.
     *
     * @return url
     */
    public String url() {
        return url;
    }

    /**
     * Get the file.
     *
     * @return file
     */
    public String file() {
        return file;
    }

    /**
     * Get the template.
     *
     * @return template
     */
    public String template() {
        return template;
    }

    /**
     * Get the order.
     *
     * @return oprder
     */
    public int order() {
        return order;
    }

    /**
     * Set the order.
     *
     * @param order order value
     */
    public void order(int order) {
        this.order = order;
    }

    /**
     * Set the value.
     *
     * @param value value
     */
    public void value(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ValueType m = (ValueType) o;
        return value.equals(m.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value, url, file, template, order);
    }

    @Override
    public String toString() {
        return "ValueType{"
                + ", value=" + value()
                + ", url=" + url()
                + ", file=" + file()
                + ", template=" + template()
                + ", order=" + order()
                + '}';
    }
}
