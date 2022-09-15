package io.helidon.build.maven.graal;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.util.SourcePath;

/**
 * A configuration filtering component using excludes and includes list.
 */
public class FilteringComponent {

    private List<String> excludes;
    private List<String> includes;

    private void excludes(List<String> excludes) {
        this.excludes = excludes;
    }

    private void includes(List<String> includes) {
        this.includes = includes;
    }

    private List<String> excludes() {
        return this.excludes;
    }

    private List<String> includes() {
        return this.includes;
    }

    /**
     * Merge includes and excludes list.
     *
     * @return the merged list
     */
    public List<String> filter() {
        Objects.requireNonNull(includes);
        List<SourcePath> paths = includes.stream()
                .map(SourcePath::new)
                .collect(Collectors.toList());
        return SourcePath.filter(paths, includes, excludes)
                .stream()
                .map(p -> p.asString(false))
                .collect(Collectors.toList());
    }
}
