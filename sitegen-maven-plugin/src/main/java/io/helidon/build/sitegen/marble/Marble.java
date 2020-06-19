package io.helidon.build.sitegen.marble;

import java.util.Objects;

public class Marble implements MarbleDiagram.Renderable, MarbleDiagram.Signal {

    final String value;
    Stream stream;
    final int x;
    String color = "white";

    public Marble(final String value, String color, final int x) {
        this.value = value;
        this.x = x;
        this.color = color;
    }

    public static Marble create(String value, final int x) {
        return new Marble(value, "white", x);
    }

    public static Marble create(String value, final int x, String color) {
        return new Marble(value, color, x);
    }

    public void stream(Stream stream){
        this.stream = stream;
    }

    @Override
    public String render() {
        Objects.requireNonNull(stream);
        return String.format("<circle cx='%d' cy='%d' r='20' stroke='black' stroke-width='3' fill='%s' />\n" +
                        "<text x='%d' y='%d' " +
                        "style=\"font-family: 'Segoe UI',Arial,sans-serif; font-weight: 300; font-size: 22px;\" " +
                        "dominant-baseline='middle' " +
                        "text-anchor='middle'>%s</text>",
                x, stream.y, color, x, stream.y, value);
    }
}