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

import org.apache.maven.artifact.versioning.ComparableVersion

class HelidonEngineImpl {

    private File mavenLibDir = new File(System.getProperty("maven.home"), "lib")

    /**
     * Check that the Maven version is greater or equal to 3.2.5.
     */
    void checkMavenVersion() {
        def mavenCoreJar = mavenLibDir.list().find { it.startsWith("maven-core-") }
        if (mavenCoreJar == null) {
            throw new IllegalStateException("Unable to determine Maven version")
        }
		String mavenVersion = new java.util.jar.JarFile(mavenCoreJar).manifest.mainAttributes.getValue("Implementation-Version");
        ComparableVersion minMavenVersion = new ComparableVersion("3.2.5")
        if (new ComparableVersion(mavenVersion) < minMavenVersion) {
            throw new IllegalStateException("Requires Maven >= 3.2.5")
        }
    }

    /**
     * Check that the Java version is greater or equal to 11.
     */
    void checkJavaVersion() {
        try {
            if (Runtime.class.getMethod("version") != null
                    && Runtime.Version.getMethod("feature") != null
                    && Runtime.getRuntime().version().feature() < 11) {
                throw new IllegalStateException()
            }
        } catch (NoSuchMethodException | IllegalStateException ex) {
            throw new IllegalStateException("Requires Java >= 11")
        } catch (Throwable ex) {
            throw new IllegalStateException("Unable to verify Java version", ex)
        }
    }

    /**
     * Generate a project.
     *
     * @param aetherScript script for {@code Aether.groovy}
     * @param localRepo local repository directory
     * @param remoteRepos remote artifacts repositories
     * @param archetypeGroupId archetype groupId
     * @param archetypeArtifactId archetype artifactId
     * @param archetypeVersion archetype version
     * @param engineGav helidon engine GAV string
     * @param props properties
     * @param projectDir project directory
     */
    void generate(String aetherScript,
                  File localRepo,
                  remoteRepos,
                  String archetypeGroupId,
                  String archetypeArtifactId,
                  String archetypeVersion,
                  String engineGav,
                  Map<String, String> props,
                  File projectDir) {

        // the current class loader is restricted and there is no way to bootstrap aether as-is
        // create a class-loader with the maven core libraries found under ${maven.home}/lib
        List<URL> mavenLibs = mavenLibDir
                .listFiles()
                .collect{ it.toURI().toURL() }
        def cl = new URLClassLoader(mavenLibs.toArray(new URL[mavenLibs.size()]), this.getClass().getClassLoader())

        // set the context class loader for plexus to work properly.
        Thread.currentThread().setContextClassLoader(cl)

        def aether = new GroovyShell(cl)
                .parse(aetherScript)
                .create(localRepo, remoteRepos)

        // resolve the archetype JAR from the local repository
        def archetypeFile = aether
                .resolveArtifact(archetypeGroupId, archetypeArtifactId, "jar", archetypeVersion)

        // resolve the helidon engine libs from remote repository
        List<URL> engineLibs = aether
                .resolveDependencies(engineGav)
                .collect { it.toURI().toURL() }

        // create a class-loader with the engine dependencies
        def ecl = new URLClassLoader(engineLibs.toArray(new URL[engineLibs.size()]), this.getClass().getClassLoader())

        // instantiate the engine
        def engine = ecl.loadClass("io.helidon.build.archetype.engine.ArchetypeEngine")
                .getConstructor(File.class, Map.class)
                .newInstance(archetypeFile, props)

        // delete place place-holder pom
        new File(projectDir, "pom.xml")
                .delete()

        engine.generate(projectDir)
    }
}

/**
 * Create a new engine instance.
 * @return HelidonEngineImpl
 */
def create() {
    return new HelidonEngineImpl()
}

return this
