/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang.maven;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.nio.file.Path;
import org.eolang.maven.hash.ChsAsMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link VersionsMojo}.
 *
 * @since 0.29.6
 */
final class VersionsMojoTest {
    @Test
    void replacesVersionsOk(@TempDir final Path tmp) throws Exception {
        final String[] tags = new String[] {"0.23.17", "0.25.0"};
        final String[] hashes = new String[] {"15c85d7", "0aa6875"};
        new FakeMaven(tmp)
            .with("withVersions", true)
            .with("commitHashes", new ChsAsMap.Fake())
            .withProgram(
                "+alias org.eolang.io.stdout\n",
                "[] > main",
                "  seq > @",
                "    stdout|0.23.17",
                "      QQ.txt.sprintf|0.25.0",
                "        \"Hello world\"",
                "    nop"
            ).execute(new FakeMaven.Versions());
        final XML xml = new XMLDocument(
            tmp.resolve(
                String.format("target/%s/foo/x/main.xmir", OptimizeMojo.DIR)
            )
        );
        final String format = "";
        MatcherAssert.assertThat(
            xml.xpath(String.format(format, hashes[0], hashes[1])),
            Matchers.hasSize(2)
        );
        MatcherAssert.assertThat(
            xml.xpath(String.format(format, tags[0], tags[1])),
            Matchers.empty()
        );
    }
}