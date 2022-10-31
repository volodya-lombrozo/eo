package org.eolang.maven;

public class ChPattern implements CommitHash {

    private final String pattern;
    private final String tag;

    public ChPattern(
        final String pattern,
        final String tag
    ) {
        this.pattern = pattern;
        this.tag = tag;
    }

    @Override
    public String value() {
        return null;
    }
}
