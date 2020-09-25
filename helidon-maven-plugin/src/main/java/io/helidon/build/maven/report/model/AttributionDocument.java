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
