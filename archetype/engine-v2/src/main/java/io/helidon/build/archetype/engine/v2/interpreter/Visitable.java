package io.helidon.build.archetype.engine.v2.interpreter;

interface Visitable {

    <A> void accept(Visitor<A> visitor, A arg);
}
