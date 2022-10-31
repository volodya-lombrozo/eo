package org.eolang.maven;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Mapped;

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
        final SortedMap<Integer, String> matches = new TreeMap<>(Comparator.reverseOrder());
        Iterable<Pattern> all = new Mapped<>(
            Pattern::new,
            new IterableOf<>(pattern.split(","))
        );
        for (final Pattern pattern : all) {
            final int weight = pattern.weight(tag);
            if (weight > 0) {
                matches.put(weight, pattern.hash);
            }
        }
        final String hash;
        if (matches.isEmpty()) {
            hash = "";
        } else {
            hash = matches.get(matches.firstKey());
        }
        return hash;
    }

    private static class Pattern {

        private final String pattern;
        private final String hash;

        private Pattern(String raw) {
            this(raw.split(":"));
        }

        private Pattern(String[] raw) {
            this(raw[0], raw[1]);
        }

        private Pattern(final String pattern, final String hash) {
            this.pattern = pattern;
            this.hash = hash;
        }

        private int weight(final String tag) {
            final int weight;
            if (tag.matches(this.regex())) {
                weight = 1 + numberOfConstants();
            } else {
                weight = 0;
            }
            return weight;
        }

        private int numberOfConstants() {
            return (int) Stream.of(pattern.split("\\."))
                .filter(s -> !s.equals("*")).count();
        }

        private String regex() {
            List<String> keys = new LinkedList<>();
            for (final String key : pattern.split("\\.")) {
                if (key.equals("*")) {
                    keys.add("\\w+");
                } else {
                    keys.add(key);
                }
            }
            return String.join("\\.", keys);
        }
    }
}
