/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2025 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import com.github.lombrozo.xnav.Xnav;
import com.jcabi.log.Logger;
import com.jcabi.manifests.Manifests;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.cactoos.list.ListOf;
import org.eolang.lints.Defect;
import org.eolang.lints.Program;
import org.eolang.lints.Programs;
import org.eolang.lints.Severity;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * Mojo that runs all lints and checks errors and warnings,
 * preferably after the {@code assemble} goal.
 *
 * @since 0.31.0
 */
@Mojo(
    name = "lint",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true
)
@SuppressWarnings("PMD.TooManyMethods")
public final class MjLint extends MjSafe {
    /**
     * The directory where to transpile to.
     */
    static final String DIR = "3-lint";

    /**
     * Subdirectory for optimized cache.
     */
    static final String CACHE = "linted";

    @Override
    void exec() throws IOException {
        if (this.skipLinting) {
            Logger.info(this, "Linting is skipped because eo:skipLinting is TRUE");
        } else {
            this.lint();
        }
    }

    /**
     * Lint.
     * @throws IOException If fails
     */
    private void lint() throws IOException {
        final long start = System.currentTimeMillis();
        final Collection<TjForeign> tojos = this.scopedTojos().withXmir();
        final ConcurrentHashMap<Severity, Integer> counts = new ConcurrentHashMap<>();
        counts.putIfAbsent(Severity.CRITICAL, 0);
        counts.putIfAbsent(Severity.ERROR, 0);
        counts.putIfAbsent(Severity.WARNING, 0);
        final int passed = new Threaded<>(
            tojos,
            tojo -> this.lintOne(tojo, counts)
        ).total();
        if (tojos.isEmpty()) {
            Logger.info(this, "There are no XMIR programs, nothing to lint individually");
        }
        if (this.lintAsPackage) {
            Logger.info(
                this,
                "XMIR programs linted as a package: %d",
                this.lintAll(counts)
            );
        } else {
            Logger.info(
                this,
                "Skipping linting as package (use -Deo.lintAsPackage=true to enable)"
            );
        }
        final String sum = MjLint.summary(counts);
        Logger.info(
            this,
            "Linted %d out of %d XMIR program(s) that needed this (out of %d total programs) in %[ms]s: %s",
            passed, tojos.size(), tojos.size(), System.currentTimeMillis() - start, sum
        );
        Logger.info(
            this,
            "Read more about lints: https://www.objectionary.com/lints/%s",
            Manifests.read("Lints-Version")
        );
        if (counts.get(Severity.ERROR) > 0 || counts.get(Severity.CRITICAL) > 0) {
            throw new IllegalStateException(
                String.format(
                    "In %d XMIR files, we found %s (must stop here)",
                    tojos.size(), sum
                )
            );
        } else if (counts.get(Severity.WARNING) > 0 && this.failOnWarning) {
            throw new IllegalStateException(
                String.format(
                    "In %d XMIR files, we found %s (use -Deo.failOnWarning=false to ignore)",
                    tojos.size(), sum
                )
            );
        }
    }

    /**
     * XMIR verified to another XMIR.
     * @param tojo Foreign tojo
     * @param counts Counts of errors, warnings, and critical
     * @return Amount of passed tojos (1 if passed, 0 if errors)
     * @throws Exception If failed to lint
     */
    private int lintOne(
        final TjForeign tojo,
        final ConcurrentHashMap<Severity, Integer> counts
    ) throws Exception {
        final Path source = tojo.xmir();
        final XML xmir = new XMLDocument(source);
        final Path base = this.targetDir.toPath().resolve(MjLint.DIR);
        final Path target = new Place(new ProgramName(xmir).get()).make(base, MjAssemble.XMIR);
        tojo.withLinted(
            new FpDefault(
                src -> MjLint.linted(xmir, counts).toString(),
                this.cache.toPath().resolve(MjLint.CACHE),
                this.plugin.getVersion(),
                new TojoHash(tojo),
                base.relativize(target),
                this.cacheEnabled
            ).apply(source, target)
        );
        return 1;
    }

    /**
     * Lint all XMIR files together.
     * @param counts Counts of errors, warnings, and critical
     * @return Amount of seen XMIR files
     * @throws IOException If failed to lint
     */
    private int lintAll(final ConcurrentHashMap<Severity, Integer> counts) throws IOException {
        final Map<String, Path> paths = new HashMap<>();
        for (final TjForeign tojo : this.scopedTojos().withXmir()) {
            paths.put(tojo.identifier(), tojo.xmir());
        }
        for (final TjForeign tojo : this.compileTojos().withXmir()) {
            paths.put(tojo.identifier(), tojo.xmir());
        }
        final Map<String, XML> pkg = new HashMap<>();
        for (final Map.Entry<String, Path> ent : paths.entrySet()) {
            pkg.put(ent.getKey(), new XMLDocument(ent.getValue()));
        }
        final Collection<Defect> defects = new Programs(pkg)
            .without("unlint-non-existing-defect")
            .without("inconsistent-args")
            .defects();
        for (final Defect defect : defects) {
            counts.compute(defect.severity(), (sev, before) -> before + 1);
            MjLint.embed(
                pkg.get(defect.program()),
                new ListOf<>(defect)
            );
            MjLint.logOne(defect);
        }
        return pkg.size();
    }

    /**
     * Log one defect.
     * @param defect The defect to log
     */
    private static void logOne(final Defect defect) {
        final StringBuilder message = new StringBuilder()
            .append(defect.program())
            .append(':').append(defect.line())
            .append(' ')
            .append(defect.text())
            .append(" (")
            .append(defect.rule())
            .append(')');
        switch (defect.severity()) {
            case WARNING:
                Logger.warn(MjLint.class, message.toString());
                break;
            case ERROR:
            case CRITICAL:
                Logger.error(MjLint.class, message.toString());
                break;
            default:
                throw new IllegalArgumentException(
                    String.format(
                        "Not yet supported severity: %s",
                        defect.severity()
                    )
                );
        }
    }

    /**
     * Text in plural or singular form.
     * @param count Counts of errors, warnings, and critical
     * @param name Name of them
     * @return Summary text
     */
    private static String plural(final int count, final String name) {
        final StringBuilder txt = new StringBuilder();
        txt.append(count).append(' ').append(name);
        if (count > 1) {
            txt.append('s');
        }
        return txt.toString();
    }

    /**
     * Summarize the counts.
     * @param counts Counts of errors, warnings, and critical
     * @return Summary text
     */
    private static String summary(final ConcurrentHashMap<Severity, Integer> counts) {
        final List<String> parts = new ArrayList<>(0);
        final int criticals = counts.get(Severity.CRITICAL);
        if (criticals > 0) {
            parts.add(MjLint.plural(criticals, "critical error"));
        }
        final int errors = counts.get(Severity.ERROR);
        if (errors > 0) {
            parts.add(MjLint.plural(errors, "error"));
        }
        final int warnings = counts.get(Severity.WARNING);
        if (warnings > 0) {
            parts.add(MjLint.plural(warnings, "warning"));
        }
        if (parts.isEmpty()) {
            parts.add("no complaints");
        }
        final String sum;
        if (parts.size() < 3) {
            sum = String.join(" and ", parts);
        } else {
            sum = String.format(
                "%s, and %s",
                String.join(", ", parts.subList(0, parts.size() - 2)),
                parts.get(parts.size() - 1)
            );
        }
        return sum;
    }

    /**
     * Find all possible linting defects and add them to the XMIR.
     * @param xmir The XML before linting
     * @param counts Counts of errors, warnings, and critical
     * @return XML after linting
     * @todo #3977:25min Enable `unlint-non-existing-defect` lint.
     *  Currently its disabled because of <a href="https://github.com/objectionary/lints/issues/385">this</a>
     *  bug. Once issue will be resolved, we should enable this lint. Don't forget to enable
     *  this lint in WPA scope too.
     * @todo #4039:30min Enable `inconsistent-args` lint.
     *  This lint generates many errors during the compilation of eo-runtime.
     *  We need to fix the errors in eo-runtime and enable this lint.
     *  Don't forget to enable this lint in {@link #lintAll(ConcurrentHashMap)} method
     *  as well.
     */
    private static XML linted(final XML xmir, final ConcurrentHashMap<Severity, Integer> counts) {
        final Directives dirs = new Directives();
        final Collection<Defect> defects = new Program(xmir)
            .without(
                "unlint-non-existing-defect",
                "empty-object",
                "sprintf-without-formatters",
                "inconsistent-args"
            )
            .defects()
            .stream()
            .filter(MjLint.distinctByKey(Defect::text))
            .collect(Collectors.toList());
        if (!defects.isEmpty()) {
            dirs.xpath("/program").addIf("errors").strict(1);
            MjLint.embed(xmir, defects);
        }
        for (final Defect defect : defects) {
            counts.compute(defect.severity(), (sev, before) -> before + 1);
            MjLint.logOne(defect);
        }
        final Node node = xmir.inner();
        new Xembler(dirs).applyQuietly(node);
        return new XMLDocument(node);
    }

    /**
     * Check if the defect is unique.
     * @param field Field extractor.
     * @param <T> Type of the object which is checked.
     * @return Predicate that checks if the field is distinct.
     * @todo #4039:30min Remove 'distinctByKey' method.
     *  We specifically added to the method to remove duplicates from the
     *  `defects` list generated by the {@link Program} class.
     *  There is the issue with duplicated error reports:
     *  https://github.com/objectionary/lints/issues/451
     *  When this issue will be resolved, we should remove this method.
     */
    private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> field) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(field.apply(t));
    }

    /**
     * Inject defect into XMIR.
     * @param xmir The XML before linting
     * @param defects The defects to inject
     */
    private static void embed(final XML xmir, final Collection<Defect> defects) {
        final Directives dirs = new Directives();
        dirs.xpath("/program").addIf("errors").strict(1);
        final Node node = xmir.inner();
        final Xnav xnav = new Xnav(node);
        for (final Defect defect : defects) {
            if (MjLint.suppressed(xnav, defect)) {
                continue;
            }
            dirs.add("error")
                .attr("check", defect.rule())
                .attr("severity", defect.severity().mnemo())
                .set(defect.text());
            if (defect.line() > 0) {
                dirs.attr("line", defect.line());
            }
            dirs.up();
        }
        new Xembler(dirs).applyQuietly(node);
    }

    /**
     * This defect is suppressed?
     * @param xnav The XMIR as {@link Xnav}
     * @param defect The defect
     * @return TRUE if suppressed
     */
    private static boolean suppressed(final Xnav xnav, final Defect defect) {
        return xnav.path(
            String.format(
                "/program/metas/meta[head='unlint' and tail='%s']",
                defect.rule()
            )
        ).findAny().isPresent();
    }
}
