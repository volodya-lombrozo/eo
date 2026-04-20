/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.model.Dependency;

/**
 * Downloads and unpacks a Maven artifact via plain HTTP, without Aether.
 *
 * <p>Unlike {@link Central}, this implementation does not require
 * {@code MavenProject}, {@code MavenSession}, {@code MojoExecutor},
 * or any Aether runtime classes, making it safe to use both inside
 * a Maven plugin and in standalone execution.</p>
 *
 * @since 0.55
 */
final class CentralHttp implements BiConsumer<Dependency, Path> {

    /**
     * Default local Maven repository path.
     */
    private static final Path LOCAL = Paths.get(
        System.getProperty("user.home"), ".m2", "repository"
    );

    /**
     * Maven Central base URL.
     */
    private static final String CENTRAL = "https://repo1.maven.org/maven2";

    /**
     * Local repository root.
     */
    private final Path local;

    /**
     * Remote repository base URL.
     */
    private final String remote;

    /**
     * Ctor.
     */
    CentralHttp() {
        this(CentralHttp.LOCAL, CentralHttp.CENTRAL);
    }

    /**
     * Ctor.
     * @param repo Local repository path
     */
    CentralHttp(final Path repo) {
        this(repo, CentralHttp.CENTRAL);
    }

    /**
     * Ctor.
     * @param repo Local repository path
     * @param base Remote repository base URL
     */
    CentralHttp(final Path repo, final String base) {
        this.local = repo;
        this.remote = base;
    }

    @Override
    public void accept(final Dependency dep, final Path dest) {
        final String classifier = Objects.requireNonNullElse(dep.getClassifier(), "");
        final String filename = CentralHttp.jarName(
            dep.getArtifactId(), dep.getVersion(), classifier
        );
        final Path cached = this.local
            .resolve(dep.getGroupId().replace('.', '/'))
            .resolve(dep.getArtifactId())
            .resolve(dep.getVersion())
            .resolve(filename);
        if (!Files.exists(cached)) {
            final String url = String.format(
                "%s/%s/%s/%s/%s",
                this.remote,
                dep.getGroupId().replace('.', '/'),
                dep.getArtifactId(),
                dep.getVersion(),
                filename
            );
            try {
                CentralHttp.fetch(url, cached);
            } catch (final IOException ex) {
                throw new IllegalStateException(
                    String.format(
                        "Failed to download %s:%s:%s from %s",
                        dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), url
                    ),
                    ex
                );
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    String.format(
                        "Interrupted while downloading %s:%s:%s",
                        dep.getGroupId(), dep.getArtifactId(), dep.getVersion()
                    ),
                    ex
                );
            }
        }
        try {
            CentralHttp.unpack(cached, dest);
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
     * Builds the JAR filename from artifact coordinates.
     * @param artifact Artifact ID
     * @param version Version
     * @param classifier Classifier (may be empty)
     * @return JAR filename
     */
    private static String jarName(
        final String artifact, final String version, final String classifier
    ) {
        final String result;
        if (classifier.isEmpty()) {
            result = String.format("%s-%s.jar", artifact, version);
        } else {
            result = String.format("%s-%s-%s.jar", artifact, version, classifier);
        }
        return result;
    }

    /**
     * Downloads a remote URL to a local file.
     * @param url Source URL
     * @param dest Destination file (parent directories are created automatically)
     * @throws IOException If the server returns a non-200 status or I/O fails
     * @throws InterruptedException If the thread is interrupted during download
     */
    private static void fetch(final String url, final Path dest)
        throws IOException, InterruptedException {
        Files.createDirectories(dest.getParent());
        final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        final HttpResponse<Path> response = client.send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofFile(dest)
        );
        if (response.statusCode() != 200) {
            Files.deleteIfExists(dest);
            throw new IOException(
                String.format("HTTP %d fetching %s", response.statusCode(), url)
            );
        }
        Logger.info(CentralHttp.class, "Downloaded %s", url);
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
