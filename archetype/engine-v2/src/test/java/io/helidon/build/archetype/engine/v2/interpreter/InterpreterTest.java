package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.File;

import io.helidon.build.archetype.engine.v2.archive.Archetype;
import io.helidon.build.archetype.engine.v2.archive.ArchetypeFactory;
import io.helidon.build.archetype.engine.v2.descriptor.ArchetypeDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest {

    @Test
    void testStepVisit() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("archetype").getFile());
        Archetype archetype = ArchetypeFactory.create(file);
        ArchetypeDescriptor descriptor = archetype.getDescriptor("flavor.xml");
        descriptor.archetypeAttributes();

    }

    @Test
    void testEnumVisit() {
    }

    @Test
    void testSourceVisit() {
    }
}