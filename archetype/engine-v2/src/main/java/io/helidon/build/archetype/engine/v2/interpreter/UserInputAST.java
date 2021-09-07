package io.helidon.build.archetype.engine.v2.interpreter;

public class UserInputAST extends ASTNode {

    private final String label;
    private final String help;

    UserInputAST(String label, String help, String currentDirectory) {
        super(null, currentDirectory);
        this.label = label;
        this.help = help;
    }

    /**
     * Get the label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Get the help.
     *
     * @return help
     */
    public String help() {
        return help;
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }

    static UserInputAST create(InputNodeAST input, StepAST step) {
        UserInputAST result = new UserInputAST(step.label(), step.help(), input.currentDirectory());
        return result;
    }
}