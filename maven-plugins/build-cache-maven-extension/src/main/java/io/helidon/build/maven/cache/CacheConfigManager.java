/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.maven.cache.CacheConfig.LifecycleConfig;
import io.helidon.build.maven.cache.CacheConfig.ReactorRule;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.common.Strings.normalizePath;

/**
 * Cache config manager.
 */
@Named
@SessionScoped
public class CacheConfigManager {

    private final MavenSession session;
    private final CacheConfig cacheConfig;
    private final ReactorRule reactorRule;
    private final Map<MavenProject, LifecycleConfig> lifecycleConfigCache = new ConcurrentHashMap<>();

    /**
     * Create a new instance.
     *
     * @param session Maven session
     */
    @Inject
    public CacheConfigManager(MavenSession session) {
        this.session = session;
        this.cacheConfig = initCacheConfig();
        this.reactorRule = initReactorRule();
    }

    /**
     * Get the cache configuration.
     *
     * @return CacheConfig
     */
    public CacheConfig cacheConfig() {
        return cacheConfig;
    }

    /**
     * Get the life-cycle config for a project.
     *
     * @param project project
     * @return LifecycleConfig, never {@code null}
     */
    public LifecycleConfig lifecycleConfig(MavenProject project) {
        return lifecycleConfigCache.computeIfAbsent(project, p -> {
            String projectPath = normalizePath(root().relativize(p.getFile().toPath().toAbsolutePath()));
            return cacheConfig().lifecycleConfig().stream()
                    .filter(c -> c.matches(projectPath))
                    .findFirst()
                    .orElse(LifecycleConfig.EMPTY);
        });
    }

    /**
     * Get the configured reactor rule.
     *
     * @return ReactorRule or {@code null} if none configured
     */
    public ReactorRule reactorRule() {
        return reactorRule;
    }

    private CacheConfig initCacheConfig() {
        try {
            Path configFile = root().resolve(".mvn/cache-config.xml");
            if (Files.exists(configFile)) {
                XMLElement elt = XMLElement.parse(Files.newInputStream(configFile));
                return new CacheConfig(elt, session.getSystemProperties(), session.getUserProperties());
            }
            return CacheConfig.EMPTY;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ReactorRule initReactorRule() {
        String name = cacheConfig.reactorRule();
        if (name == null || name.isEmpty()) {
            return null;
        }
        return cacheConfig.reactorRules().stream()
                .filter(r -> r.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ReactorRule not found: " + name));
    }

    private Path root() {
        return session.getRequest().getMultiModuleProjectDirectory().toPath();
    }
}
