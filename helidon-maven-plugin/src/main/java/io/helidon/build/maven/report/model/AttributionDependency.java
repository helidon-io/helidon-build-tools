package io.helidon.build.maven.report.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

@XmlRootElement(name="dependency")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={"id", "name", "version", "licensor", "licenseName", "consumers", "attribution"})
public class AttributionDependency {

    private String id;
    private String name;
    private String version;
    private String licenseName;
    private String licensor;
    private String attribution;

    @XmlElementWrapper(name="consumers")
    @XmlElement(name="consumer")
    private SortedSet<String> consumers;

    public AttributionDependency() {
    }

    @Override
    public String toString() {
        return "AttributionDependency{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", licenseName='" + licenseName + '\'' +
                ", licensor='" + licensor + '\'' +
                ", consumers=" + consumers +
                '}';
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicensor() {
        return licensor;
    }

    public void setLicensor(String licensor) {
        this.licensor = licensor;
    }

    public String getAttribution() {
        return attribution;
    }

    public String getId() {
        return id;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public Set<String> getConsumers() {
        return consumers;
    }

    public void setConsumers(SortedSet<String> consumers) {
        this.consumers = consumers;
    }

    public void setId(String id) {
        this.id = id;
    }
}
