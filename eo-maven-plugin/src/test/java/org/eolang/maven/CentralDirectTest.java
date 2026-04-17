/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2026 Objectionary.com
 * SPDX-License-Identifier: MIT
 */
package org.eolang.maven;

import com.yegor256.Mktmp;
import com.yegor256.MktmpResolver;
import com.yegor256.WeAreOnline;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.io.FileMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test case for {@link CentralDirect}.
 *
 * @since 0.55
 */
@ExtendWith({MktmpResolver.class, WeAreOnline.class})
final class CentralDirectTest {

    @Test
    void downloadsFromRemoteWhenNotInLocalRepository(@Mktmp final Path temp) {
        final Path local = temp.resolve("empty-local-repo");
        final Path dest = temp.resolve("unpacked");
        new CentralDirect(local).accept(
            new Dep()
                .withGroupId("org.eolang")
                .withArtifactId("eo-runtime")
                .withVersion("0.7.0")
                .get(),
            dest
        );
        MatcherAssert.assertThat(
            "Artifact must be cached in the local repository after download",
            local.resolve("org/eolang/eo-runtime/0.7.0").toFile(),
            FileMatchers.anExistingDirectory()
        );
        MatcherAssert.assertThat(
            "Unpacked destination must contain files fetched from Maven Central",
            dest.toFile().list(),
            Matchers.not(Matchers.emptyArray())
        );
    }

    @Test
    void resolvesWithDefaultConstructor(@Mktmp final Path temp) {
        final Path dest = temp.resolve("unpacked");
        new CentralDirect().accept(
            new Dep()
                .withGroupId("org.eolang")
                .withArtifactId("eo-runtime")
                .withVersion("0.7.0")
                .get(),
            dest
        );
        MatcherAssert.assertThat(
            "Unpacked destination must contain files resolved using default constructor",
            dest.toFile().list(),
            Matchers.not(Matchers.emptyArray())
        );
    }

    @Test
    void throwsOnUnresolvableArtifact(@Mktmp final Path temp) {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new CentralDirect(temp.resolve("local-repo")).accept(
                new Dep()
                    .withGroupId("org.eolang")
                    .withArtifactId("does-not-exist-xyz")
                    .withVersion("0.0.1")
                    .get(),
                temp.resolve("dest")
            ),
            "Must throw when artifact cannot be resolved"
        );
    }
}
