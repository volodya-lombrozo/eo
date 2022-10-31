package org.eolang.maven;

import java.util.LinkedList;
import java.util.List;

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
        final String[] allPatterns = pattern.split(",");
        for (final String pattern : allPatterns) {
            final String[] keyValue = pattern.split(":");
            String key = keyValue[0];
            String value = keyValue[1];
            if (new PatternEntry(key, value).matches(tag)) {
                return value;
            }
        }
        return "";
    }

    private static class PatternEntry {

        private final String tag;
        private final String hash;

        private PatternEntry(final String tag, final String hash) {
            this.tag = tag;
            this.hash = hash;
        }

        private boolean matches(final String incoming) {
            if (incoming.equals(tag)) {
                return true;
            }
            final String[] splitPattern = tag.split("\\.");

            List<String> keys = new LinkedList<>();
            for (final String s : splitPattern) {
                if (s.equals("*")) {
                    keys.add("\\w+");
                } else {
                    keys.add(s);
                }
            }

            final String compiled = String.join("\\.", keys);

            System.out.println(splitPattern);
            System.out.println(compiled);
            final boolean res = incoming.matches(compiled);
            return res;
        }

        private String hash() {
            return hash;
        }
    }
}
