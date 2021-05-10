package io.helidon.lsp.server.model;

import java.util.List;
import java.util.Map;

public class ConfigurationHint {

    private String name;
    private List<Value> values;

    public ConfigurationHint() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Value> getValues() {
        return values;
    }

    public void setValues(List<Value> values) {
        this.values = values;
    }

    public static class Value {
        private String value;
        private String description;

        public Value() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
