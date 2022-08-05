package io.helidon.build.maven.archetype;

import java.util.Set;

@SuppressWarnings("unused")
public class Validation {

    private Set<String> patterns;
    private String match;
    private boolean fail;

    public Set<String> getPatterns() {
        return patterns;
    }

    public String getMatch() {
        return match;
    }

    public boolean getFail() {
        return fail;
    }

    public void setPatterns(Set<String> patterns) {
        this.patterns = patterns;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }
}
