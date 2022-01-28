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
import java.util.Set;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Attribution dependency.
 */
@XmlRootElement(name = "dependency")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "name", "version", "licensor", "licenseName", "consumers", "attribution"})
public class AttributionDependency {

    private String id;
    private String name;
    private String version;
    private String licenseName;
    private String licensor;
    private String attribution;

    @XmlElementWrapper(name = "consumers")
    @XmlElement(name = "consumer")
    private SortedSet<String> consumers;

    /**
     * Default constructor.
     */
    public AttributionDependency() {
    }

    @Override
    public String toString() {
        return "AttributionDependency{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", version='" + version + '\''
                + ", licenseName='" + licenseName + '\''
                + ", licensor='" + licensor + '\''
                + ", consumers=" + consumers
                + '}';
    }

    /**
     * Construct with the given id.
     * @param id unique identifier for dependency.
     */
    public AttributionDependency(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributionDependency that = (AttributionDependency) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Get the attribution name.
     *
     * @return attribution name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the attribution name.
     *
     * @param name attribution name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the version.
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version.
     *
     * @param version version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the license name.
     *
     * @return license name
     */
    public String getLicenseName() {
        return licenseName;
    }

    /**
     * Set the license name.
     *
     * @param licenseName license name
     */
    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    /**
     * Get the licensor.
     *
     * @return licensor
     */
    public String getLicensor() {
        return licensor;
    }

    /**
     * Set the licensor.
     *
     * @param licensor licensor
     */
    public void setLicensor(String licensor) {
        this.licensor = licensor;
    }

    /**
     * Get the attribution.
     *
     * @return attribution
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * Set the attribution.
     *
     * @param attribution attribution
     */
    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    /**
     * Get the consumers.
     *
     * @return consumers
     */
    public Set<String> getConsumers() {
        return consumers;
    }

    /**
     * Set the consumers.
     *
     * @param consumers consumers
     */
    public void setConsumers(SortedSet<String> consumers) {
        this.consumers = consumers;
    }

    /**
     * Get the id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     *
     * @param id id
     */
    public void setId(String id) {
        this.id = id;
    }
}
