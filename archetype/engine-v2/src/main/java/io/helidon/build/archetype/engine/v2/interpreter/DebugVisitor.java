package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class DebugVisitor extends VisitorEmptyImpl<ASTNode> {

    private static final Logger LOGGER = Logger.getLogger(DebugVisitor.class.getName());
    private boolean showVisits;
    private final Set<ASTNode> visitedNodes = new HashSet<>();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");
    }

    DebugVisitor(boolean showVisits) {
        this.showVisits = showVisits;
    }

    @Override
    public void visit(InputTextAST input, ASTNode parent) {
        String message = String.format("%s InputTextAST {path=\"%s\"; label=\"%s\"}", indent(input), input.path(), input.label());
        processMessage(input, message);
    }

    @Override
    public void visit(StepAST input, ASTNode parent) {
        String message = String.format("%s StepAST {label=\"%s\"}", indent(input), input.label());
        processMessage(input, message);
    }

    @Override
    public void visit(InputAST input, ASTNode parent) {
        String message = String.format("%s InputAST", indent(input));
        processMessage(input, message);
    }

    @Override
    public void visit(InputBooleanAST input, ASTNode parent) {
        String message = String.format("%s InputBooleanAST {path=\"%s\"; label=\"%s\"}", indent(input), input.path(),
                input.label());
        processMessage(input, message);
    }

    @Override
    public void visit(InputEnumAST input, ASTNode parent) {
        String message = String.format("%s InputEnumAST {path=\"%s\"; label=\"%s\"}", indent(input), input.path(), input.label());
        processMessage(input, message);
    }

    @Override
    public void visit(InputListAST input, ASTNode parent) {
        String message = String.format("%s InputListAST {path=\"%s\"; label=\"%s\"}", indent(input), input.path(), input.label());
        processMessage(input, message);
    }

    @Override
    public void visit(ExecAST input, ASTNode parent) {
        String message = String.format("%s ExecAST {src=\"%s\"}", indent(input), input.src());
        processMessage(input, message);
    }

    @Override
    public void visit(SourceAST input, ASTNode parent) {
        String message = String.format("%s SourceAST {source=\"%s\"}", indent(input), input.source());
        processMessage(input, message);
    }

    @Override
    public void visit(ContextAST input, ASTNode parent) {
        if (input.children().isEmpty()) {
            return;
        }
        String message = String.format("%s ContextAST", indent(input));
        processMessage(input, message);
    }

    @Override
    public void visit(ContextBooleanAST input, ASTNode parent) {
        String message = String.format("%s ContextBooleanAST {path=\"%s\"; bool=%s}", indent(input), input.path(), input.bool());
        processMessage(input, message);
    }

    @Override
    public void visit(ContextEnumAST input, ASTNode parent) {
        String message = String.format("%s ContextEnumAST {path=\"%s\"; value=\"%s\"}", indent(input), input.path(),
                input.value());
        processMessage(input, message);
    }

    @Override
    public void visit(ContextListAST input, ASTNode parent) {
        String message = String.format("%s ContextListAST {path=\"%s\"; values=\"%s\"}", indent(input), input.path(),
                input.values());
        processMessage(input, message);
    }

    @Override
    public void visit(ContextTextAST input, ASTNode parent) {
        String message = String.format("%s ContextTextAST {path=\"%s\"; text=\"%s\"}", indent(input), input.path(), input.text());
        processMessage(input, message);
    }

    @Override
    public void visit(OptionAST input, ASTNode parent) {
        String message = String.format("%s OptionAST {label=\"%s\"; value=\"%s\"}", indent(input), input.label(), input.value());
        processMessage(input, message);
    }

    @Override
    public void visit(OutputAST input, ASTNode parent) {
        String message = String.format("%s OutputAST", indent(input));
        processMessage(input, message);
    }

    private void processMessage(ASTNode input, String message) {
        if (showVisits) {
            LOGGER.info(message);
            return;
        }
        if (!visitedNodes.contains(input)) {
            LOGGER.info(message);
            visitedNodes.add(input);
        }
    }

    /**
     * Show all nodes' visits that interpreter does during the interpreting the scripts.
     *
     * @return true if show, false otherwise.
     */
    public boolean showVisits() {
        return showVisits;
    }

    private String indent(ASTNode input) {
        return " ".repeat(getLevel(input, 0));
    }

    private int getLevel(ASTNode input, int startLevel) {
        if (input.parent() == null) {
            return startLevel;
        } else {
            return getLevel(input.parent(), startLevel + 1);
        }
    }

    /**
     * Create a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new DebugVisitor.Builder();
    }

    /**
     * {@code DebuggerVisitor} builder static inner class.
     */
    public static final class Builder {

        private boolean showVisits = false;

        private Builder() {
        }

        /**
         * Sets the {@code archetype} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param showVisits the {@code showVisits} to set
         * @return a reference to this Builder
         */
        public Builder archetype(boolean showVisits) {
            this.showVisits = showVisits;
            return this;
        }

        /**
         * Returns a {@code DebuggerVisitor} built from the parameters previously set.
         *
         * @return a {@code DebuggerVisitor} built with parameters of this {@code Builder}
         */
        public DebugVisitor build() {
            return new DebugVisitor(showVisits);
        }
    }

}
