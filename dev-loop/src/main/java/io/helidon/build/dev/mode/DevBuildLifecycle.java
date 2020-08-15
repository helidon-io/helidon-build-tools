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
package io.helidon.build.dev.mode;

import java.util.List;
import java.util.Objects;

/**
 * Configuration beans for the build lifecycle.
 */
public class DevBuildLifecycle {
    private static final String DEFAULT_FULL_BUILD_GOAL = "process-classes";

    private String fullBuildGoal;
    private IncrementalBuild incrementalBuild;

    /* TODO REMOVE

    <profiles>
        <profile>
            <id>helidon-cli</id>
            <activation>
                <property>
                    <name>helidon.cli</name>
                    <value>true</value>
                </property>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>io.helidon.build-tools</groupId>
                        <artifactId>helidon-cli-maven-plugin</artifactId>
                        <extensions>true</extensions>
                        <executions>
                            <execution>
                                <id>default-cli</id> <!-- must use this id! -->
                                <goals>
                                    <goal>dev</goal>
                                </goals>
                                <configuration>
                                    <devLoop>
                                        <fullBuildGoal>process-lots-of-stuff</fullBuildGoal>
                                        <incrementalBuild>
                                            <resources>
                                                <goals>
                                                    <goal>process-just-resources</goal>
                                                </goals>
                                                <includes>**</includes>
                                            </resources>
                                        </incrementalBuild>
                                    </devLoop>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>


     */



    public DevBuildLifecycle() {
        this.fullBuildGoal = DEFAULT_FULL_BUILD_GOAL;
        this.incrementalBuild = new IncrementalBuild();
    }

    public String getFullBuildGoal() {
        return fullBuildGoal;
    }

    public void setFullBuildGoal(String fullBuildGoal) {
        this.fullBuildGoal = fullBuildGoal;
    }

    public IncrementalBuild getIncrementalBuild() {
        return incrementalBuild;
    }

    public void setIncrementalBuild(IncrementalBuild incrementalBuild) {
        this.incrementalBuild = incrementalBuild;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DevBuildLifecycle)) return false;
        final DevBuildLifecycle that = (DevBuildLifecycle) o;
        return Objects.equals(getFullBuildGoal(), that.getFullBuildGoal()) &&
               Objects.equals(getIncrementalBuild(), that.getIncrementalBuild());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullBuildGoal(), getIncrementalBuild());
    }

    /**
     * TODO: Describe
     */
    public static class IncrementalBuild {
        private static final String DEFAULT_SOURCES_GOAL = "process-classes";
        private static final String DEFAULT_RESOURCES_GOAL = "process-resources";

        private Component resources;
        private Component sources;

        public IncrementalBuild() {
            resources = new Component(List.of(DEFAULT_RESOURCES_GOAL), "**", "**/*.class,**/*.swp,**/*~,**/\\.*");
            sources = new Component(List.of(DEFAULT_SOURCES_GOAL), "**/*.java", "");
        }

        public Component getResources() {
            return resources;
        }

        public void setResources(Component resources) {
            this.resources = resources;
        }

        public Component getSources() {
            return sources;
        }

        public void setSources(Component sources) {
            this.sources = sources;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IncrementalBuild)) return false;
            final IncrementalBuild that = (IncrementalBuild) o;
            return Objects.equals(getResources(), that.getResources()) &&
                   Objects.equals(getSources(), that.getSources());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getResources(), getSources());
        }

        public static class Component {
            private List<String> goals;
            private String includes;
            private String excludes;

            public Component() {
            }

            public Component(List<String> goals, String includes, String excludes) {
                this.goals = goals;
                this.includes = includes;
                this.excludes = excludes;
            }

            public List<String> getGoals() {
                return goals;
            }

            public void setGoals(List<String> goals) {
                this.goals = goals;
            }

            public String getIncludes() {
                return includes;
            }

            public void setIncludes(String includes) {
                this.includes = includes;
            }

            public String getExcludes() {
                return excludes;
            }

            public void setExcludes(String excludes) {
                this.excludes = excludes;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Component)) return false;
                final Component component = (Component) o;
                return Objects.equals(getGoals(), component.getGoals()) &&
                       Objects.equals(getIncludes(), component.getIncludes()) &&
                       Objects.equals(getExcludes(), component.getExcludes());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getGoals(), getIncludes(), getExcludes());
            }
        }

        private static class CustomComponent {
            private List<Path> paths;
            private List<String> goals;

            public List<Path> getPaths() {
                return paths;
            }

            public void setPaths(List<Path> paths) {
                this.paths = paths;
            }

            public List<String> getGoals() {
                return goals;
            }

            public void setGoals(List<String> goals) {
                this.goals = goals;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof CustomComponent)) return false;
                final CustomComponent that = (CustomComponent) o;
                return Objects.equals(getPaths(), that.getPaths()) &&
                       Objects.equals(getGoals(), that.getGoals());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getPaths(), getGoals());
            }
        }

        public static class Path {
            private String path;
            private String includes;
            private String excludes;

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public String getIncludes() {
                return includes;
            }

            public void setIncludes(String includes) {
                this.includes = includes;
            }

            public String getExcludes() {
                return excludes;
            }

            public void setExcludes(String excludes) {
                this.excludes = excludes;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Path)) return false;
                final Path path1 = (Path) o;
                return Objects.equals(getPath(), path1.getPath()) &&
                       Objects.equals(getIncludes(), path1.getIncludes()) &&
                       Objects.equals(getExcludes(), path1.getExcludes());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getPath(), getIncludes(), getExcludes());
            }
        }
    }


    /* TODO REMOVE

     <configuration>
      <!-- See https://github.com/eclipse-ee4j/glassfish-spec-version-maven-plugin -->
      <!-- For an example of a Java bean used for this style of configuration -->
      <!-- devLoop is a list of ordered "component" -->
      <!-- The resulting build steps are ordered by their order in this map -->
      <devLoop>
        <!-- this particular entry is not order dependent -->
        <projectfile>
          <steps>
            <step>process-classes</step>
          </steps>
        </projectfile>
        <resources>
          <includes />
          <excludes />
          <steps>
            <step>resources:process-resources</step>
            <!-- see example execution on the next snippet below -->
            <step>exec:exec@compile-sass</step>
            <!-- can also be a phase -->
            <!-- <step>process-resources</step> -->
          </steps>
        </resources>
        <!-- monitor a non conventional directory that is not registered as project compile source or as project resource -->
        <custom>
          <paths>
            <path>
              <value>src/main/foo</value>
              <!-- Comma separated list of file patterns to exclude -->
              <!-- See io.helidon.build.util.SourcePath -->
              <includes />
              <!-- Same syntax -->
              <excludes />
            </path>
            <path>
              <value>src/main/foo</value>
              <includes />
              <excludes />
            </path>
          </paths>
          <steps>
            <step>foo:bar</step>
          </steps>
        <custom>
        <compileSources>
          <includes />
          <excludes />
          <steps>
            <step>compiler:compile</step>
            <!-- can also be a phase -->
            <!-- <step>compile</step> -->
          </steps>
        </compileSources>
      </devLoop>
    </configuration>


     */

}
