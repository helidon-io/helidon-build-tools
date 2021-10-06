package io.helidon.build.archetype.engine.v2;

import java.io.File;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;

public class ArchetypeBaseTest {

    protected Archetype getArchetype(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(name).getFile());
        return ArchetypeFactory.create(file);
    }
}
