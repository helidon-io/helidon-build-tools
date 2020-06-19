package io.helidon.build.sitegen.marble;

import java.util.LinkedList;
import java.util.List;

public class Stream implements MarbleDiagram.Row {

    final static String NAME_STYLE = "\"font-family: 'Segoe UI',Arial,sans-serif; font-weight: 300; font-size: 18px;\"";

    final String name;
    int y;
    private List<MarbleDiagram.Signal> signals = new LinkedList<>();

    public Stream(final String name) {
        this.name = name;
    }

    public void baseLine(int baseLine){
        this.y = baseLine;
    }

    public void signal(MarbleDiagram.Signal signal) {
        signal.stream(this);
        this.signals.add(signal);
    }

    public static Stream create(String name) {
        return new Stream(name);
    }

    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<path d='M500 %d L515 %d L500 %d Z' stroke-width='3' stroke='black'></path>", y+10, y, y-10));
        sb.append(String.format("<path d='M20 %d L500 %d Z' stroke-width='3' stroke='black'/>", y, y));
        sb.append(String.format("<text x='20' y='%d' style=%s dominant-baseline='middle'>%s</text>", y+20, NAME_STYLE, name));
        signals.forEach(signal -> sb.append(signal.render()));
        return sb.toString();
    }
}