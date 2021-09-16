package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;

import io.helidon.build.archetype.engine.v2.descriptor.Templates;

/**
 * Archetype templates AST node in {@link OutputAST}.
 */
public class TemplatesAST extends ASTNode implements ConditionalNode {

    private String directory;
    private final LinkedList<String> includes;
    private final LinkedList<String> excludes;
    private final String engine;
    private final String transformation;

    TemplatesAST(String engine, String transformation, String directory,
                 LinkedList<String> includes, LinkedList<String> excludes,
                 ASTNode parent, Location location) {
        super(parent, location);
        this.directory = directory;
        this.includes = includes;
        this.excludes = excludes;
        this.engine = engine;
        this.transformation = transformation;
    }

    /**
     * Get the engine.
     *
     * @return engine
     */
    public String engine() {
        return engine;
    }

    /**
     * Get the directory.
     *
     * @return directory
     */
    public String directory() {
        return directory;
    }

    /**
     * Set the directory.
     *
     * @param directory new directory
     */
    public void directory(String directory) {
        this.directory = directory;
    }

    /**
     * Get the transformation.
     *
     * @return transformation
     */
    public String transformation() {
        return transformation;
    }

    /**
     * Get the include filters.
     *
     * @return list of include filter, never {@code null}
     */
    public LinkedList<String> includes() {
        return includes;
    }

    /**
     * Get the exclude filters.
     *
     * @return list of exclude filter, never {@code null}
     */
    public LinkedList<String> excludes() {
        return excludes;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    @Override
    public <T, A> T accept(GenericVisitor<T, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    static TemplatesAST create(Templates templatesFrom, ASTNode parent, Location location) {
        TemplatesAST result = new TemplatesAST(
                templatesFrom.engine(),
                templatesFrom.transformation(),
                templatesFrom.directory(),
                templatesFrom.includes(),
                templatesFrom.excludes(),
                parent,
                location
        );
        if (templatesFrom.model() != null) {
            ModelAST model = ModelAST.create(templatesFrom.model(), result, location);
            result.children().add(ConditionalNode.mapConditional(templatesFrom.model(), model, result, location));
        }

        return result;
    }
}
