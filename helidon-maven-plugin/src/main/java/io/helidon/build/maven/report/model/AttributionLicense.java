package io.helidon.build.maven.report.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class AttributionLicense {
    private String name;
    private String text;

    public AttributionLicense() {
    }

    public AttributionLicense(String name, String text) {
        this.name = name;
        this.text = text;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

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
