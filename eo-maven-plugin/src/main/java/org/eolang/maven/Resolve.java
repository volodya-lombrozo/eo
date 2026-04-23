/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.cactoos.iterable.Mapped;
import org.cactoos.set.SetOf;
import org.cactoos.text.Joined;

/**
 * Resolves all required runtime dependencies: downloads from Maven Central,
 * unpacks and places them into the target directory.
 *
 * @since 0.63.0
 * @checkstyle ParameterNumberCheck (100 lines)
 */
final class Resolve {

    /**
     * Tojos.
     */
    private final TjsForeign tojos;

    /**
     * Target directory.
     */
    private final Path target;

    /**
     * Central dependency consumer.
     */
    private final BiConsumer<Dependency, Path> central;

    /**
     * Discover self too.
     */
    private final boolean discover;

    /**
     * Skip zero versions.
     */
    private final boolean skipzero;

    /**
     * Resolve default JNA dependency.
     */
    private final boolean jna;

    /**
     * Ignore runtime dependency.
     */
    private final boolean noruntime;

    /**
     * Maven project (may be null).
     * @todo #4989:30min Remove MavenProject dependency from Resolve class.
     *  Currently Resolve depends on MavenProject to find the eo-runtime version
     *  declared in pom.xml. Extract that lookup into a plain {@code Optional<Dependency>}
     *  parameter so Resolve has no Maven API dependency at all.
     *  Don't forget to update MjResolve accordingly.
     */
    private final MavenProject project;

    /**
     * Resolve dependencies in central.
     */
    private final boolean incentral;

    /**
     * Ignore version conflicts.
     */
    private final boolean noconflicts;

    /**
     * Ctor.
     * @param tjs Tojos
     * @param tgt Target directory
     * @param cntrl Central dependency consumer
     * @param self Discover self
     * @param zero Skip zero versions
     * @param jnadep Resolve default JNA
     * @param norun Ignore runtime
     * @param proj Maven project
     * @param central Resolve in central
     * @param noconf Ignore version conflicts
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    Resolve(
        final TjsForeign tjs,
        final Path tgt,
        final BiConsumer<Dependency, Path> cntrl,
        final boolean self,
        final boolean zero,
        final boolean jnadep,
        final boolean norun,
        final MavenProject proj,
        final boolean central,
        final boolean noconf
    ) {
        this.tojos = tjs;
        this.target = tgt;
        this.central = cntrl;
        this.discover = self;
        this.skipzero = zero;
        this.jna = jnadep;
        this.noruntime = norun;
        this.project = proj;
        this.incentral = central;
        this.noconflicts = noconf;
    }

    /**
     * Execute the resolve process.
     * @throws IOException If fails
     */
    void exec() throws IOException {
        final Collection<Dep> deps = this.deps();
        if (deps.isEmpty()) {
            Logger.info(this, "No new dependencies unpacked");
        } else {
            new Threaded<>(
                deps,
                dep -> this.resolved(dep, this.target)
            ).total();
            Logger.info(
                this,
                "New %d dependenc(ies) unpacked to %[file]s: %s",
                deps.size(), this.target,
                new Joined(", ", new Mapped<>(Dep::toString, deps))
            );
        }
    }

    /**
     * Resolve a single dependency.
     * @param dep Dependency
     * @param dest Destination directory
     * @return Count resolved
     * @throws IOException If fails
     */
    private int resolved(final Dep dep, final Path dest) throws IOException {
        final String classifier;
        final Dependency dependency = dep.get();
        if (dependency.getClassifier() == null || dependency.getClassifier().isEmpty()) {
            classifier = "-";
        } else {
            classifier = dependency.getClassifier();
        }
        final Path place = this.cleanPlace(
            dest
                .resolve(dependency.getGroupId())
                .resolve(dependency.getArtifactId())
                .resolve(classifier),
            dependency.getVersion()
        );
        final int total;
        if (Files.exists(place)) {
            Logger.debug(
                this,
                "Dependency %s already resolved and exists in %[file]s",
                dep, place
            );
            total = 0;
        } else {
            this.central.accept(dependency, place);
            final int files = new Walk(place).size();
            if (files == 0) {
                Logger.warn(this, "No new files after unpacking of %s!", dep);
            } else {
                Logger.info(
                    this, "Found %d new file(s) (%d MB) after unpacking of %s",
                    files, Resolve.folderSizeInMb(place), dep
                );
            }
            total = 1;
        }
        return total;
    }

    /**
     * Returns directory where files should be unpacked, removing outdated versions.
     * @param dir Directory
     * @param version Version
     * @return Full path
     * @throws IOException If fails
     */
    private Path cleanPlace(final Path dir, final String version) throws IOException {
        final File[] subs = dir.toFile().listFiles();
        if (subs != null) {
            for (final File sub : subs) {
                final String base = sub.getName();
                Logger.info(this, "Base if %s", base);
                if (base.equals(version)) {
                    continue;
                }
                final Path bad = dir.resolve(base);
                try (Stream<Path> walk = Files.walk(bad)) {
                    walk
                        .map(Path::toFile)
                        .sorted(Comparator.reverseOrder())
                        .forEach(File::delete);
                }
                Logger.info(
                    this,
                    "Directory %[file]s deleted because it contained wrong version files (not %s)",
                    bad, version
                );
            }
        }
        return dir.resolve(version);
    }

    /**
     * Find all deps for all tojos.
     * @return List of dependencies
     */
    private Collection<Dep> deps() {
        Dependencies result = new DpsDefault(
            this.tojos, this.discover, this.skipzero, this.jna
        );
        if (this.noruntime) {
            Logger.info(this, "Runtime dependency is ignored because eo:ignoreRuntime=TRUE");
            result = new DpsWithoutRuntime(result);
        } else {
            final Optional<Dependency> runtime = this.runtimeFromPom();
            if (runtime.isPresent()) {
                result = new DpsWithRuntime(result, new Dep(runtime.get()));
                Logger.info(
                    this,
                    "Runtime dependency added from pom with version: %s",
                    runtime.get().getVersion()
                );
            } else {
                if (this.incentral) {
                    result = new DpsWithRuntime(result);
                } else {
                    result = new DpsOfflineRuntime(result);
                }
            }
        }
        if (!this.noconflicts) {
            result = new DpsUniquelyVersioned(result);
        }
        return new SetOf<>(result)
            .stream()
            .sorted()
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Runtime dependency from pom.xml.
     * @return Dependency if found
     */
    private Optional<Dependency> runtimeFromPom() {
        final Optional<Dependency> res;
        if (this.project == null) {
            res = Optional.empty();
        } else {
            res = this.project
                .getDependencies()
                .stream()
                .filter(Resolve::isRuntime)
                .findFirst();
        }
        return res;
    }

    /**
     * Checks if dependency is the eo-runtime artifact.
     * @param dep Dependency
     * @return True if runtime
     */
    static boolean isRuntime(final Dependency dep) {
        return "org.eolang".equals(dep.getGroupId())
            && "eo-runtime".equals(dep.getArtifactId());
    }

    /**
     * Folder size in megabytes.
     * @param path Folder
     * @return Size in MB
     * @throws IOException If fails
     */
    @SuppressWarnings("PMD.UnnecessaryLocalRule")
    private static long folderSizeInMb(final Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.filter(Files::isRegularFile).mapToLong(
                p -> {
                    try {
                        return Files.size(p);
                    } catch (final IOException exception) {
                        throw new IllegalStateException(
                            String.format("Failed to calculate size in %s", p),
                            exception
                        );
                    }
                }
            ).sum() / 1024L / 1024L;
        }
    }
}
