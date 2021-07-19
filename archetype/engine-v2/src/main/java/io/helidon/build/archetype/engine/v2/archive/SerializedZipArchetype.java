package io.helidon.build.archetype.engine.v2.archive;

import java.nio.file.Path;
import java.util.List;

public class SerializedZipArchetype implements Archetype {
    @Override
    public Path getFile(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Descriptor getDescriptor(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getPaths() {
        throw new UnsupportedOperationException();
    }
}
