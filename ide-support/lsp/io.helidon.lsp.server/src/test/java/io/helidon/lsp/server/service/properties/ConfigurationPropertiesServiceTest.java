package io.helidon.lsp.server.service.properties;

import io.helidon.lsp.server.model.ConfigurationMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class ConfigurationPropertiesServiceTest {

    @Test
    public void test() throws IOException {
        String file = "/home/aserkes/.m2/repository/io/helidon/common/helidon-common/2.2.1-SNAPSHOT/helidon-common-2.2.1-SNAPSHOT.jar";
        ConfigurationPropertiesService service = new ConfigurationPropertiesService();
        ConfigurationMetadata configMetadata = service.getConfigMetadataFromJar(file);
        System.out.println(configMetadata);
    }

    @Test
    public void test1() {
        Map<String, TestClass> map = new HashMap<>();
        map.putIfAbsent("aaa", new TestClass());
        map.putIfAbsent("aaa", new TestClass());
    }

    public static class TestClass {
        public TestClass() {
            System.out.println("constructor");
        }
    }
}