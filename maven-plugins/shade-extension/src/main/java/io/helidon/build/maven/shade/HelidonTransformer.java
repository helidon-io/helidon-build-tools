/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.maven.shade;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ReproducibleResourceTransformer;

import static java.util.stream.Collectors.toMap;

/**
 * Maven Shade plugin custom transformer for merging Helidon files.
 * <p>
 * Usage:
 * <pre>{@code
 * <plugin>
 *    <groupId>org.apache.maven.plugins</groupId>
 *    <artifactId>maven-shade-plugin</artifactId>
 *    <version>3.5.1</version>
 *    <executions>
 *        <execution>
 *            <phase>package</phase>
 *            <goals>
 *                <goal>shade</goal>
 *            </goals>
 *            <configuration>
 *                <transformers>
 *                    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
 *                    <transformer implementation="io.helidon.build.maven.shade.HelidonTransformer"/>
 *                </transformers>
 *            </configuration>
 *        </execution>
 *    </executions>
 *    <dependencies>
 *        <dependency>
 *            <groupId>io.helidon.build-tools</groupId>
 *            <artifactId>helidon-shade-extensions</artifactId>
 *            <version>4.0.0</version>
 *        </dependency>
 *    </dependencies>
 * </plugin>
 * }</pre>
 */
public class HelidonTransformer implements ReproducibleResourceTransformer {

    static final String SERVICE_REGISTRY_PATH = "META-INF/helidon/service-registry.json";
    static final String CONFIG_METADATA_PATH = "META-INF/helidon/config-metadata.json";
    static final String FEATURE_METADATA_PATH = "META-INF/helidon/feature-registry.json";

    private final Map<String, Aggregator> aggregators = Stream
            .of(
                    new JsonArrayAggregator(SERVICE_REGISTRY_PATH),
                    new JsonArrayAggregator(CONFIG_METADATA_PATH),
                    new JsonArrayAggregator(FEATURE_METADATA_PATH),
                    new ServiceLoaderAggregator(),
                    new SerialConfigAggregator()
            )
            .collect(toMap(Aggregator::path, Function.identity()));

    @Override
    public boolean canTransformResource(String resource) {
        return aggregators.containsKey(resource);
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
        processResource(resource, is, relocators, 0);
    }

    @Override
    public boolean hasTransformedResource() {
        return aggregators.values().stream().anyMatch(Aggregator::hasTransformedResource);
    }

    @Override
    public void modifyOutputStream(JarOutputStream jarOutputStream) throws IOException {
        for (Aggregator a : aggregators.values()) {
            a.writeToJar(jarOutputStream);
        }
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators, long time) throws IOException {
        aggregators.get(resource).aggregate(is);
    }
}
