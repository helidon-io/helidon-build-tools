/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.io.File;
import java.io.IOException;

/**
 * Helidon archetype engine main class.
 */
public final class Main {

    private Main() {
    }

    /**
     * Helidon archetype engine main method.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: template-jar directory");
            System.exit(1);
        }
        File templateJar = new File(args[0]);
        if (!templateJar.exists()) {
            System.err.println(templateJar + " does not exist");
            System.exit(1);
        }

        ArchetypeLoader loader;
        try {
            loader = new ArchetypeLoader(templateJar);
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        File outputDir = new File(args[1]);
        if (outputDir.exists()) {
            System.err.println(outputDir + " exists");
            System.exit(1);
        }
        new ArchetypeEngine(loader, Maps.fromProperties(System.getProperties())).generate(outputDir);
    }
}
