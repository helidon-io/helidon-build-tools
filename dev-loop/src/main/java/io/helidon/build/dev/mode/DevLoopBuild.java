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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

/**
 * Configuration beans for the build lifecycle.
 * <p></p>
 * Example pom declaration:
 * <pre>
 *     <profiles>
 *         <profile>
 *             <id>helidon-cli</id>
 *             <activation>
 *                 <property>
 *                     <name>helidon.cli</name>
 *                     <value>true</value>
 *                 </property>
 *             </activation>
 *             <build>
 *                 <plugins>
 *                     <plugin>
 *                         <groupId>io.helidon.build-tools</groupId>
 *                         <artifactId>helidon-cli-maven-plugin</artifactId>
 *                         <extensions>true</extensions>
 *                         <executions>
 *                             <execution>
 *                                 <id>default-cli</id> <!-- must use this id! -->
 *                                 <goals>
 *                                     <goal>dev</goal>
 *                                 </goals>
 *                                 <configuration>
 *
 *                                    <!-- NOTE changes to this configuration will NOT be noticed by the helidon dev command! -->
 *
 *                                    <devLoop>
 *                                         <fullBuildGoal>process-lots-of-stuff</fullBuildGoal>
 *                                         <incrementalBuild>
 *
 *                                             <!-- directories/includes/excludes from maven-resources-plugin config -->
 *                                             <resourceGoals>
 *                                                 <goal>process-my-resources</goal>
 *                                             </resourceGoals>
 *
 *                                             <!-- directories/includes/excludes from maven-compiler-plugin config -->
 *                                             <javaSourceGoals>
 *                                                 <goal>process-my-sources</goal>
 *                                             </javaSourceGoals>
 *
 *                                             <customDirectories>
 *                                                 <directory>
 *                                                     <path>src/etc1</path>
 *                                                     <watch>
 *                                                         <includes>**&#47;*.foo,**&#47;*.bar</includes>
 *                                                     </watch>
 *                                                     <goals>
 *                                                         <goal>my-custom-goal-1</goal>
 *                                                         <goal>my-custom-goal-2</goal>
 *                                                     </goals>
 *                                                 </directory>
 *                                                 <directory>
 *                                                     <path>src/etc2</path>
 *                                                     <watch>
 *                                                         <includes>**&#47;*.baz</includes>
 *                                                     </watch>
 *                                                     <goals>
 *                                                         <goal>my-custom-goal-X</goal>
 *                                                     </goals>
 *                                                 </directory>
 *                                             </customDirectories>
 *                                         </incrementalBuild>
 *                                     </devLoop>
 *                                 </configuration>
 *                             </execution>
 *                         </executions>
 *                     </plugin>
 *                 </plugins>
 *             </build>
 *         </profile>
 *     </profiles>
 * </pre>
 */
public class DevLoopBuild {
    private static final String DEFAULT_FULL_BUILD_GOAL = "process-classes";

    private String fullBuildGoal;
    private IncrementalBuild incrementalBuild;

    public DevLoopBuild() {
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
        if (!(o instanceof DevLoopBuild)) return false;
        final DevLoopBuild that = (DevLoopBuild) o;
        return Objects.equals(getFullBuildGoal(), that.getFullBuildGoal()) &&
               Objects.equals(getIncrementalBuild(), that.getIncrementalBuild());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullBuildGoal(), getIncrementalBuild());
    }

    @Override
    public String toString() {
        return "DevBuildLifecycle{" +
               "fullBuildGoal='" + fullBuildGoal + '\'' +
               ", incrementalBuild=" + incrementalBuild +
               '}';
    }

    /**
     * TODO: Describe
     */
    public static class IncrementalBuild {
        private List<String> resourceGoals;
        private List<String> javaSourceGoals;
        private List<CustomDirectory> customDirectories;

        public IncrementalBuild() {
        }

        public List<String> getResourceGoals() {
            return resourceGoals;
        }

        public void setResourceGoals(List<String> resourceGoals) {
            this.resourceGoals = resourceGoals;
        }

        public List<String> getJavaSourceGoals() {
            return javaSourceGoals;
        }

        public void setJavaSourceGoals(List<String> javaSourceGoals) {
            this.javaSourceGoals = javaSourceGoals;
        }

        public List<CustomDirectory> getCustomDirectories() {
            return customDirectories;
        }

        public void setCustomDirectories(List<CustomDirectory> customDirectories) {
            this.customDirectories = customDirectories;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IncrementalBuild)) return false;
            final IncrementalBuild that = (IncrementalBuild) o;
            return Objects.equals(getResourceGoals(), that.getResourceGoals()) &&
                   Objects.equals(getJavaSourceGoals(), that.getJavaSourceGoals()) &&
                   Objects.equals(getCustomDirectories(), that.getCustomDirectories());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getResourceGoals(), getJavaSourceGoals(), getCustomDirectories());
        }

        @Override
        public String toString() {
            return "IncrementalBuild{" +
                   "resourceGoals=" + resourceGoals +
                   ", javaSourceGoals=" + javaSourceGoals +
                   ", customDirectories=" + customDirectories +
                   '}';
        }

        public static class Watch {
            private String includes;
            private String excludes;

            public Watch() {
            }

            public List<String> getIncludes() {
                return includes == null ? emptyList() : Arrays.asList(includes.split(","));
            }

            public void setIncludes(String includes) {
                this.includes = includes;
            }

            public List<String> getExcludes() {
                return excludes == null ? emptyList() : Arrays.asList(excludes.split(","));
            }

            public void setExcludes(String excludes) {
                this.excludes = excludes;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Watch)) return false;
                final Watch watch = (Watch) o;
                return Objects.equals(getIncludes(), watch.getIncludes()) &&
                       Objects.equals(getExcludes(), watch.getExcludes());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getIncludes(), getExcludes());
            }

            @Override
            public String toString() {
                return "Watch{" +
                       "includes='" + includes + '\'' +
                       ", excludes='" + excludes + '\'' +
                       '}';
            }
        }

        public static class CustomDirectory {
            private String path;
            private Watch watch;
            private List<String> goals;


            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public Watch getWatch() {
                return watch;
            }

            public void setWatch(Watch watch) {
                this.watch = watch;
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
                if (!(o instanceof CustomDirectory)) return false;
                final CustomDirectory customDirectory = (CustomDirectory) o;
                return Objects.equals(getPath(), customDirectory.getPath()) &&
                       Objects.equals(getWatch(), customDirectory.getWatch()) &&
                       Objects.equals(getGoals(), customDirectory.getGoals());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getPath(), getWatch(), getGoals());
            }

            @Override
            public String toString() {
                return "CustomDirectory{" +
                       "path='" + path + '\'' +
                       ", watch=" + watch +
                       ", goals=" + goals +
                       '}';
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
