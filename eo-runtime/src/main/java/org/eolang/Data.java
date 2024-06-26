/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2024 Objectionary.com
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

package org.eolang;

import EOorg.EOeolang.EOint;
import EOorg.EOeolang.EOstring;
import java.nio.charset.StandardCharsets;

/**
 * A data container.
 *
 * @since 0.1
 */
@Versionized
public interface Data {
    /**
     * Attach data to the object.
     * @param data Data.
     * @todo #2931:60min Change the data storage architecture. Current implementation allows the
     *  presence of two methods for data manipulations: {@link Data#attach(byte[])} to set data and
     *  {@link Data#delta()} to get data; which does not seem to be object oriented. It also
     *  requires every object to have reserved place for possible injected data. In our case, every
     *  {@link PhDefault} has {@link PhDefault#data} variable. It would be much better to have this
     *  data only inside some decorator. The main difficulty here is - child attributes of
     *  decorated object should know that their \rho is decorated and contains data.
     */
    void attach(byte[] data);

    /**
     * Take the data.
     * @return The data
     */
    byte[] delta();

    /**
     * Makes a {@link Phi} out of a primitive Java object, like {@link String} or {@link Integer}.
     *
     * <p>This is more convenient than making {@link EOstring}, then making
     * {@link EOorg.EOeolang.EObytes}, fill it up with data and then injecting bytes to string.
     * This class is used in Java tests mostly for the sake of brevity.
     * In auto-generated Java code we use
     * {@link EOint}/{@link EOstring} and then wrap it with {@link PhData}.
     *
     * @since 0.1
     */
    final class ToPhi implements Phi {
        /**
         * Phi object.
         */
        private final Phi object;

        /**
         * Ctor.
         * @param obj Data
         */
        public ToPhi(final Object obj) {
            this.object = Data.ToPhi.toPhi(obj);
        }

        @Override
        public boolean equals(final Object obj) {
            return this.object.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.object.hashCode();
        }

        @Override
        public Phi copy() {
            return this.object.copy();
        }

        @Override
        public Phi take(final String name) {
            return this.object.take(name);
        }

        @Override
        public boolean put(final int pos, final Phi obj) {
            return this.object.put(pos, obj);
        }

        @Override
        public boolean put(final String name, final Phi obj) {
            return this.object.put(name, obj);
        }

        @Override
        public String locator() {
            return this.object.locator();
        }

        @Override
        public String forma() {
            return this.object.forma();
        }

        @Override
        public String φTerm() {
            return this.object.φTerm();
        }

        @Override
        public String toString() {
            return this.object.toString();
        }

        @Override
        public void attach(final byte[] data) {
            this.object.attach(data);
        }

        @Override
        public byte[] delta() {
            return this.object.delta();
        }

        /**
         * Convert to Phi object.
         * @param obj Object to convert
         * @return Constructed Phi
         */
        private static Phi toPhi(final Object obj) {
            final Phi phi;
            final byte[] bytes;
            final boolean delta;
            final Phi eolang = Phi.Φ.take("org").take("eolang");
            if (obj instanceof Boolean) {
                phi = eolang.take("bool").copy();
                delta = false;
                if (obj.equals(true)) {
                    bytes = new byte[] {0x01};
                } else {
                    bytes = new byte[] {0x00};
                }
            } else if (obj instanceof byte[]) {
                phi = eolang.take("bytes").copy();
                delta = true;
                bytes = (byte[]) obj;
            } else if (obj instanceof Long) {
                phi = eolang.take("int").copy();
                delta = false;
                bytes = new BytesOf((Long) obj).take();
            } else if (obj instanceof String) {
                phi = eolang.take("string").copy();
                delta = false;
                bytes = Data.ToPhi.unescapeJavaString(
                    (String) obj
                ).getBytes(StandardCharsets.UTF_8);
            } else if (obj instanceof Double) {
                phi = eolang.take("float").copy();
                delta = false;
                bytes = new BytesOf((Double) obj).take();
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        "Unknown type of data: %s",
                        obj.getClass().getCanonicalName()
                    )
                );
            }
            if (delta) {
                phi.attach(bytes);
            } else {
                final Phi bts = eolang.take("bytes").copy();
                bts.attach(bytes);
                phi.put(0, bts);
            }
            return phi;
        }

        /**
         * Unescapes a string that contains standard Java escape sequences.
         * <ul>
         * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> :
         * BS, FF, NL, CR, TAB, double and single quote.</li>
         * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character
         * specification (0 - 377, 0x00 - 0xFF).</li>
         * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
         * </ul>
         * @param str A string optionally containing standard java escape sequences.
         * @return The translated string
         * @checkstyle CyclomaticComplexityCheck (100 lines)
         * @checkstyle JavaNCSSCheck (100 lines)
         * @checkstyle NestedIfDepthCheck (100 lines)
         * @checkstyle ModifiedControlVariableCheck (100 lines)
         */
        private static String unescapeJavaString(final String str) {
            final StringBuilder unescaped = new StringBuilder(str.length());
            for (int idx = 0; idx < str.length(); ++idx) {
                char chr = str.charAt(idx);
                if (chr == '\\') {
                    final char next;
                    if (idx == str.length() - 1) {
                        next = '\\';
                    } else {
                        next = str.charAt(idx + 1);
                    }
                    if (next >= '0' && next <= '7') {
                        String code = String.valueOf(next);
                        ++idx;
                        if (idx < str.length() - 1 && str.charAt(idx + 1) >= '0'
                            && str.charAt(idx + 1) <= '7') {
                            code += str.charAt(idx + 1);
                            ++idx;
                            if (idx < str.length() - 1 && str.charAt(idx + 1) >= '0'
                                && str.charAt(idx + 1) <= '7') {
                                code += str.charAt(idx + 1);
                                ++idx;
                            }
                        }
                        unescaped.append((char) Integer.parseInt(code, 8));
                        continue;
                    }
                    switch (next) {
                        case '\\':
                            break;
                        case 'b':
                            chr = '\b';
                            break;
                        case 'f':
                            chr = '\f';
                            break;
                        case 'n':
                            chr = '\n';
                            break;
                        case 'r':
                            chr = '\r';
                            break;
                        case 't':
                            chr = '\t';
                            break;
                        case '\"':
                            chr = '\"';
                            break;
                        case '\'':
                            chr = '\'';
                            break;
                        case 'u':
                            if (idx >= str.length() - 5) {
                                chr = 'u';
                                break;
                            }
                            unescaped.append(
                                Character.toChars(
                                    Integer.parseInt(
                                        String.join(
                                            "",
                                            String.valueOf(str.charAt(idx + 2)),
                                            String.valueOf(str.charAt(idx + 3)),
                                            String.valueOf(str.charAt(idx + 4)),
                                            String.valueOf(str.charAt(idx + 5))
                                        ),
                                        16
                                    )
                                )
                            );
                            idx += 5;
                            continue;
                        default:
                            break;
                    }
                    ++idx;
                }
                unescaped.append(chr);
            }
            return unescaped.toString();
        }
    }
}
