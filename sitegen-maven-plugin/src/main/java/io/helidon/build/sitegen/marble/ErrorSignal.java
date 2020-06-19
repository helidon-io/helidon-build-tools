package io.helidon.build.sitegen.marble;

public class ErrorSignal implements MarbleDiagram.Renderable, MarbleDiagram.Signal {

    static final String STYLE = "fill:none;stroke:black;stroke-width:3";

    Stream stream;
    final int x;

    public ErrorSignal(final int x) {
        this.x = x;
    }

    public static ErrorSignal create(final int x) {
        return new ErrorSignal(x);
    }

    @Override
    public void stream(final Stream stream) {
        this.stream = stream;
    }

    @Override
    public String render() {
        return String.format("<polyline points='%d,%d %d,%d %d,%d %d,%d %d,%d' style='%s'/>",
                x + 10,
                stream.y + 10,
                x - 10,
                stream.y - 10,
                x, stream.y,
                x + 10,
                stream.y - 10,
                x - 10,
                stream.y + 10,
                STYLE);
    }

}
