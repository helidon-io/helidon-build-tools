/*
* Copyright (c) 2021 Oracle and/or its affiliates.
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

@SuppressWarnings(['GrPackage', 'unused'])
def postGenerate(Map<String, String> classes, request, String engineGAV, List<String> propNames) {

    // The parent class-loader is the class-loader used to run the maven-archetype-plugin, it does not have the Helidon
    // archetype engine classes loaded because it does not depend on the Helidon archetype engine (duh).

    // The maven-archetype-plugin does resolve the archetype artifact in order to generate the project, however the
    // class-loader used to invoke archetype-post-generate.groovy does not expose the archetype dependencies.
    // I.e Archetype artifacts are expected to be standalone...

    // The archetype-post-generate.groovy included in the Helidon archetypes is used to invoke the Helidon
    // archetype engine. In order for this to work, we create a class-loader from the Maven installation that can be
    // used to invoke the Maven built-in aether and resolve the Helidon archetype engine.

    def libDir = new File(System.getProperty("maven.home"), "lib")
    def mavenLibs = libDir.listFiles().collect { it.toURI().toURL() }.toArray(new URL[0])
    def mavenCl = new URLClassLoader((URL[]) mavenLibs, this.getClass().getClassLoader())

    def ccl = Thread.currentThread().getContextClassLoader()
    try {
        Thread.currentThread().setContextClassLoader(mavenCl)

        // load the bundled groovy classes
        def gcl = new GroovyClassLoader()
        classes.each {gcl.defineClass(it.key, it.value.replace('\n', '').decodeBase64())}

        // invoke the engine facade
        def aClass = gcl.loadClass("io.helidon.build.archetype.maven.postgenerate.EngineFacade")
        def method = aClass.getMethods().find { ("generate" == it.getName()) }
        try {
            method.invoke(null, request, engineGAV, propNames)
        } catch (java.lang.reflect.InvocationTargetException ex) {
            throw ex.getCause();
        }
    } finally {
        Thread.currentThread().setContextClassLoader(ccl)
    }
}
