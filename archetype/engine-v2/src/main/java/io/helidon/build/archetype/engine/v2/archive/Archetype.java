package io.helidon.build.archetype.engine.v2.archive;

import java.nio.file.Path;
import java.util.List;

public interface Archetype {
    Path getFile(String path);

    Descriptor getDescriptor(String path);

    List<String> getPaths();
}
