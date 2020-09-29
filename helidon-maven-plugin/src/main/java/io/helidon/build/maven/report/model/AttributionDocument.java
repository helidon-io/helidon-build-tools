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
package io.helidon.build.maven.report.model;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="attribution-document")
@XmlAccessorType(XmlAccessType.FIELD)
public class AttributionDocument {

    @XmlElementWrapper(name="dependencies")
    @XmlElement(name="dependency")
    private List<AttributionDependency> dependencies = new ArrayList<>();

    @XmlElementWrapper(name="licenses")
    @XmlElement(name="license")
    private List<AttributionLicense> licenses = new ArrayList<>();

    @Override
    public String toString() {
        return "AttributionDocument{" +
                "dependencies=" + dependencies +
                '}';
    }

    public void setDependencies(List<AttributionDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public void setLicenses(List<AttributionLicense> licenses) {
        this.licenses = licenses;
    }

    public List<AttributionDependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(AttributionDependency dependency) {
        this.dependencies.add(dependency);
    }

    public List<AttributionLicense> getLicenses() {
        return licenses;
    }

    public void addLicense(AttributionLicense license) {
        this.licenses.add(license);
    }
}
