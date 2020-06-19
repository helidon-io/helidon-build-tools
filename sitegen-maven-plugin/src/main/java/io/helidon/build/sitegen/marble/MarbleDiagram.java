package io.helidon.build.sitegen.marble;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MarbleDiagram {

    private final LinkedList<Row> rows = new LinkedList<>();
    private final AtomicInteger level = new AtomicInteger(100);

    public void stream(Row row) {
        row.baseLine(level.getAndAdd(80));
        rows.add(row);
    }

    public String render() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "<svg width='1000' height='500'>\n" +
                rows.stream()
                        .map(Renderable::render)
                        .collect(Collectors.joining()) +
                "   Sorry, your browser does not support inline SVG.\n" +
                "</svg> \n" +
                " \n" +
                "</body>\n" +
                "</html>";
    }

    interface Renderable {
        String render();
    }

    interface Row extends Renderable{
        void baseLine(int y);
    }

    interface Signal extends Renderable{
        void stream(Stream stream);
    }
}
