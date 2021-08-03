package io.helidon.build.archetype.engine.v2;

public interface ContextValue {
    // Flags this value as declared externally prior to any flow invocation.
    // E.g. passed-in with query parameter or CLI option
    boolean external();
    // Flags this value as set by a <context> directive
    boolean readOnly();
}
