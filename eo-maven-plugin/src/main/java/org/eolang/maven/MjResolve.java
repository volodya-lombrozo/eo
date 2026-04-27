/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.cactoos.scalar.Unchecked;

/**
 * Find all required runtime dependencies, download
 * them from Maven Central, unpack and place to the {@code target/eo}
 * directory.
 *
 * <p>
 *     The motivation for this mojo is simple: Maven doesn't have
 *     a mechanism for adding .JAR files to transpile/test classpath in
 *     runtime.
 * </p>
 *
 * <p>
 *     This goal goes through all dependencies found in the
 *     {@link MjPull} goal, finds their implementations
 *     (i.e. transitive dependencies), downloads them from Maven Central,
 *     unpacks them and places the resulting files to the
 *     {@link MjResolve#DIR} directory.
 * </p>
 * @since 0.1
 */
@Mojo(
    name = "resolve",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    threadSafe = true
)
public final class MjResolve extends MjSafe {

    /**
     * The directory where to resolve to.
     */
    static final String DIR = "4-resolve";

    /**
     * Resolve default JNA dependency or not.
     *
     * @checkstyle MemberNameCheck (7 lines)
     */
    @SuppressWarnings("PMD.ImmutableField")
    private boolean resolveJna = true;

    /**
     * Resolve dependencies in central or not.
     * @checkstyle MemberNameCheck (7 lines)
     */
    @SuppressWarnings("PMD.ImmutableField")
    private boolean resolveInCentral = true;

    @Override
    public void exec() {
        new Resolve(
            this.scopedTojos(),
            this.targetDir.toPath().resolve(MjResolve.DIR),
            this.central,
            this.discoverSelf,
            this.skipZeroVersions,
            this.resolveJna,
            this.ignoreRuntime,
            this.runtime(),
            this.resolveInCentral,
            this.ignoreVersionConflicts
        ).exec();
    }

    private Unchecked<Dep> runtime() {
        final RtPom runtime = new RtPom(this.project);
        if (runtime.isPresent()){
            return new Unchecked<>(runtime);
        } else if (this.resolveInCentral) {
            return new Unchecked<>(new RtCentral());
        } else {
            return new Unchecked<>(new RtOffline());
        }
    }
}
