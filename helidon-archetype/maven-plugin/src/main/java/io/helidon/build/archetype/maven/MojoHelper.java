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
package io.helidon.build.archetype.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.archetype.engine.Maps;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheVisitor;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.ValueCode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Maven mojo helper class.
 */
final class MojoHelper {

    /**
     * The plugin groupId.
     */
    static final String PLUGIN_GROUP_ID = "io.helidon.build-tools.archetype";

    /**
     * The plugin artifactId.
     */
    static final String PLUGIN_ARTIFACT_ID = "helidon-archetype-maven-plugin";

    /**
     * The resource name for the {@code pom.properties} file included in the plugin JAR file.
     */
    static final String POM_PROPERTIES_RESOURCE_NAME = "/META-INF/maven/"
            + PLUGIN_GROUP_ID + "/"
            + PLUGIN_ARTIFACT_ID + "/pom.properties";

    /**
     * The plugin version.
     */
    static final String PLUGIN_VERSION = getPluginVersion();

    /**
     * The file extension of mustache template files.
     */
    static final String MUSTACHE_EXT = ".mustache";

    /**
     * Mustache factory.
     */
    private static final MustacheFactory MUSTACHE_FACTORY = new MustacheFactoryImpl();

    private MojoHelper() {
    }

    /**
     * Make a properties map to be used as a mustache scope object.
     *
     * @param properties               base properties to include
     * @param includeProjectProperties {@code true} if project properties should be included
     * @param project                  maven project to get the project properties, {@code project.*} properties are emulated
     * @return Properties map
     */
    static Map<String, String> templateProperties(Map<String, String> properties,
                                                  boolean includeProjectProperties,
                                                  MavenProject project) {

        Map<String, String> props = new HashMap<>();
        props.putAll(properties);
        if (includeProjectProperties) {
            Properties projectProperties = project.getProperties();
            props.putAll(Maps.fromProperties(project.getProperties()));
            props.put("project.groupId", project.getGroupId());
            props.put("project.artifactId", project.getArtifactId());
            props.put("project.version", project.getVersion());
            props.put("project.name", project.getName());
            props.put("project.description", project.getDescription());
        }
        return props;
    }

    /**
     * Render a mustache template.
     *
     * @param templateFile template to render
     * @param name         name of the template
     * @param target       target file to create
     * @param scope        the scope for the template
     * @throws MojoExecutionException if an IO error occurs
     */
    static void renderMustacheTemplate(File templateFile, String name, Path target, Object scope)
            throws MojoExecutionException {

        try {
            renderMustacheTemplate(new FileInputStream(templateFile), name, target, scope);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Render a mustache template.
     *
     * @param is     input stream for the template to render
     * @param name   name of the template
     * @param target target file to create
     * @param scope  the scope for the template
     * @throws MojoExecutionException if an IO error occurs
     */
    static void renderMustacheTemplate(InputStream is, String name, Path target, Object scope)
            throws MojoExecutionException {

        Mustache m = MUSTACHE_FACTORY.compile(new InputStreamReader(is), name);

        try {
            Files.createDirectories(target.getParent());
            try (Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                m.execute(writer, scope).flush();
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    /**
     * Get the plugin version from the maven plugin JAR file.
     *
     * @return version, never {@code null}
     * @throws IllegalStateException if the version is {@code null} or if an IO error occurs
     */
    private static String getPluginVersion() {
        try {
            Properties props = new Properties();
            props.load(JarMojo.class.getResourceAsStream(POM_PROPERTIES_RESOURCE_NAME));
            String version = props.getProperty("version");
            if (version == null) {
                throw new IllegalStateException("Unable to resolve engine version");
            }
            return version;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Mustache object that shall not be encoded.
     */
    interface NotEncoded {
    }

    /**
     * Mustache string that shall not be encoded.
     */
    static final class RawString implements NotEncoded {

        private final String value;

        RawString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class MustacheFactoryImpl extends  DefaultMustacheFactory {
        @Override
        public MustacheVisitor createMustacheVisitor() {
            return new DefaultMustacheVisitor(this) {
                @Override
                public void value(TemplateContext tc, String variable, boolean encoded) {
                    list.add(new ValueCode(tc, df, variable, encoded) {
                        @Override
                        public Writer execute(Writer writer, List<Object> scopes) {
                            try {
                                final Object object = get(scopes);
                                if (object instanceof NotEncoded) {
                                    writer.write(oh.stringify(object));
                                    return appendText(run(writer, scopes));
                                } else {
                                    return super.execute(writer, scopes);
                                }
                            } catch (Exception e) {
                                throw new MustacheException("Failed to get value for " + name, e, tc);
                            }
                        }
                    });
                }
            };
        }
    }
}
