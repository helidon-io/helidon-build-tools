/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.inclusivenaming;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * XML Inclusive Naming.
 *
 */
@XmlRootElement(name = "root")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlInclusiveNaming {

    @XmlElement(name = "data")
    private XmlData[] data;

    /**
     * Default constructor.
     */
    public XmlInclusiveNaming() {
    }

    /**
     * Get the data.
     *
     * @return the data
     */
    public XmlData[] getData() {
        return data;
    }

    /**
     * Set the data.
     *
     * @param data
     */
    public void setData(XmlData[] data) {
        this.data = data;
    }
}
