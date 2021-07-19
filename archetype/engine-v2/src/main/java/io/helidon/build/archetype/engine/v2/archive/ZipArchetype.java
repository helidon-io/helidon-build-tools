package io.helidon.build.archetype.engine.v2.archive;

import java.nio.file.Path;
import java.util.List;

public class ZipArchetype implements Archetype {
    @Override
    public Path getFile(String path) {
        return null;
    }

    @Override
    public Descriptor getDescriptor(String path) {
        return null;
    }

    @Override
    public List<String> getPaths() {
        return null;
    }
}
