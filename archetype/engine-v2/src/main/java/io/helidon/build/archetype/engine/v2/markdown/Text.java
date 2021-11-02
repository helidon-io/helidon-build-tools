package io.helidon.build.archetype.engine.v2.markdown;


class Text extends Node {

    private String literal;

    public Text() {
    }

    public Text(String literal) {
        this.literal = literal;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String getLiteral() {
        return literal;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }

    @Override
    protected String toStringAttributes() {
        return "literal=" + literal;
    }
}
