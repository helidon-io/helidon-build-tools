package io.helidon.build.sitegen.marble;

public class CompleteSignal implements MarbleDiagram.Signal {

    static final String STYLE = "fill:none;stroke:black;stroke-width:3";

    Stream stream;
    final int x;

    public CompleteSignal(final int x) {
        this.x = x;
    }

    public static CompleteSignal create(final int x) {
        return new CompleteSignal(x);
    }

    @Override
    public void stream(final Stream stream) {
        this.stream = stream;
    }

    @Override
    public String render() {
        return String.format("<polyline points='%d,%d %d,%d' style='%s'/>",
                x,
                stream.y + 10,
                x,
                stream.y - 10,
                STYLE);
    }
}
