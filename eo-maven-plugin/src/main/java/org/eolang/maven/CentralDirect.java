/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.model.Dependency;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

/**
 * Downloads and unpacks a Maven artifact directly via the Aether resolver API.
 *
 * <p>Unlike {@link Central}, this implementation does not require
 * {@code MavenProject}, {@code MavenSession}, or {@code MojoExecutor}.</p>
 *
 * @since 0.55
 */
final class CentralDirect implements BiConsumer<Dependency, Path> {

    /**
     * Default local Maven repository path.
     */
    private static final Path LOCAL = Paths.get(
        System.getProperty("user.home"), ".m2", "repository"
    );

    /**
     * Maven Central URL.
     */
    private static final String CENTRAL = "https://repo1.maven.org/maven2";

    /**
     * Local repository path.
     */
    private final Path local;

    /**
     * Remote repository URL.
     */
    private final String url;

    /**
     * Ctor.
     */
    CentralDirect() {
        this(CentralDirect.LOCAL, CentralDirect.CENTRAL);
    }

    /**
     * Ctor.
     * @param repo Local repository path
     */
    CentralDirect(final Path repo) {
        this(repo, CentralDirect.CENTRAL);
    }

    /**
     * Ctor.
     * @param repo Local repository path
     * @param remote Remote repository URL
     */
    private CentralDirect(final Path repo, final String remote) {
        this.local = repo;
        this.url = remote;
    }

    @Override
    public void accept(final Dependency dep, final Path dest) {
        final RepositorySystem system = new RepositorySystemSupplier().get();
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(
            system.newLocalRepositoryManager(session, new LocalRepository(this.local.toFile()))
        );
        final String classifier = Objects.requireNonNullElse(dep.getClassifier(), "");
        try {
            final Artifact artifact = system.resolveArtifact(
                session,
                new ArtifactRequest(
                    new DefaultArtifact(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        classifier,
                        "jar",
                        dep.getVersion()
                    ),
                    Collections.singletonList(
                        new RemoteRepository.Builder("central", "default", this.url).build()
                    ),
                    null
                )
            ).getArtifact();
            Logger.info(
                this,
                "Resolved %s:%s:%s:%s to %s",
                dep.getGroupId(),
                dep.getArtifactId(),
                classifier,
                dep.getVersion(),
                artifact.getFile()
            );
            CentralDirect.unpack(artifact.getFile().toPath(), dest);
        } catch (final ArtifactResolutionException ex) {
            throw new IllegalStateException(
                String.format(
                    "Failed to resolve %s:%s:%s",
                    dep.getGroupId(), dep.getArtifactId(), dep.getVersion()
                ),
                ex
            );
        } catch (final IOException ex) {
            throw new IllegalStateException(
                String.format(
                    "Failed to unpack %s:%s:%s to %s",
                    dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dest
                ),
                ex
            );
        }
        if (classifier.isEmpty()) {
            Logger.info(
                this, "%s:%s:%s unpacked to %[file]s",
                dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dest
            );
        } else {
            Logger.info(
                this, "%s:%s:%s:%s unpacked to %[file]s",
                dep.getGroupId(), dep.getArtifactId(), classifier, dep.getVersion(), dest
            );
        }
    }

    @Override
    public BiConsumer<Dependency, Path> andThen(
        final BiConsumer<? super Dependency, ? super Path> after
    ) {
        throw new UnsupportedOperationException("not implemented #andThen()");
    }

    /**
     * Unpacks a JAR (ZIP) file into the given directory.
     * @param jar Path to the JAR file
     * @param dest Destination directory
     * @throws IOException If unpacking fails
     */
    private static void unpack(final Path jar, final Path dest) throws IOException {
        Files.createDirectories(dest);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                final Path target = dest.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
    }
}
