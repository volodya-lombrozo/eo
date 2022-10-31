package org.eolang.maven;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChPatternTest {

    @ParameterizedTest
    @CsvSource({
        "'0.*.*:abc2sd3', '0.0.0', abc2sd3",
        "'0.*.*:abc2sd3', '1.0.0', ''",
        "'0.*.*:m23ss3h', '0.1.2', 'm23ss3h'",
        "'0.*.*:m23ss3h,1.*.*:abc2sd3', '0.1.2', 'm23ss3h'",
        "'0.*.*:m23ss3h,1.*.*:abc2sd3', '1.1.2', 'abc2sd3'",
        "'0.*.*:m23ss3h,3.*.*:abc2sd3', '2.1.2', ''",
        "'3.*.*:m23ss3h,3.1.*:abc2sd3', '3.1.2', 'abc2sd3'",
        "'3.1.2:m23ss3h,3.1.*:abc2sd3', '3.1.2', 'm23ss3h'",
        "'master:m23ss3h,3.1.*:abc2sd3', 'master', 'm23ss3h'",
        "'master:m23ss3h,composite-tag:abc2sd3', 'composite-tag', 'abc2sd3'"
    })
    void returnsCorrectHashByPattern(
        final String pattern,
        final String tag,
        final String expected
    ) {
        MatcherAssert.assertThat(new ChPattern(pattern, tag).value(), Matchers.equalTo(expected));
    }
}