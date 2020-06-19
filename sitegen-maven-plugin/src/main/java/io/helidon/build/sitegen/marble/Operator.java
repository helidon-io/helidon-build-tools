package io.helidon.build.sitegen.marble;

public class Operator implements MarbleDiagram.Row {

    final static String BOX_STYLE = "fill:none;stroke:black;stroke-width:3;";
    final static String TEXT_STYLE = "font-family: 'Segoe UI',Arial,sans-serif; font-weight: 300; font-size: 18px;";
    private final String text;

    private int y;

    public Operator(final String text) {
        this.text = text;
    }

    public static Operator create(String name) {
        return new Operator(name);
    }

    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<polygon points='20,%d 500,%d 500,%d 20,%d' style=\"%s\" />",
                y - 20, y - 20, y + 20, y + 20, BOX_STYLE));
        sb.append(String.format("<text x='240' y='%d' style=\"%s\" dominant-baseline='middle' text-anchor='middle'>%s</text>",
                y, TEXT_STYLE, text));
        return sb.toString();
    }

    @Override
    public void baseLine(final int y) {
        this.y = y;
    }
}
