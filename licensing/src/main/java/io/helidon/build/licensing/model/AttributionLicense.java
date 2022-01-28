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
package io.helidon.build.licensing.model;

import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Attribution license.
 */
@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class AttributionLicense {
    private String name;
    private String text;

    /**
     * Default constructor.
     */
    public AttributionLicense() {
    }

    /**
     * Construction license with given name and text.
     * @param name name of license
     * @param text text of license
     */
    public AttributionLicense(String name, String text) {
        this.name = name;
        this.text = text;
    }

    /**
     * Set the name.
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the text.
     *
     * @param text text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get the text.
     *
     * @return text
     */
    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributionLicense license = (AttributionLicense) o;
        return getName().equals(license.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
