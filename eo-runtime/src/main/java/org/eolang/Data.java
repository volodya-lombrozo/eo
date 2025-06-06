/*
 * SPDX-FileCopyrightText: Copyright (c) 2016-2025 Objectionary.com
 * SPDX-License-Identifier: MIT
 */

package org.eolang;

import java.nio.charset.StandardCharsets;

/**
 * A data container.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface Data {
    /**
     * Take the data.
     * @return The data
     */
    byte[] delta();

    /**
     * Makes a {@link Phi} out of a primitive Java object, like {@link String} or {@link Integer}.
     *
     * <p>This is more convenient than making EOstring, then making EObytes fill it up with data,
     * and then injecting bytes to string.
     * This class is used in Java tests mostly for the sake of brevity.
     * In auto-generated Java code we do:
     * {@code
     * Phi bytes = Phi.Φ.take("org.eolang.bytes").copy();
     * Phi attached = new PhWith(bytes, 0, new byte[] {...});
     * Phi str = Phi.Φ.take("org.eolang.string").copy();
     * Phi applied = new PhWith(str, 0, attached);
     * return applied;
     * }
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
        public boolean hasRho() {
            return this.object.hasRho();
        }

        @Override
        public Phi take(final String name) {
            return this.object.take(name);
        }

        @Override
        public Phi take(final int pos) {
            return this.object.take(pos);
        }

        @Override
        public void put(final int pos, final Phi obj) {
            this.object.put(pos, obj);
        }

        @Override
        public void put(final String name, final Phi obj) {
            this.object.put(name, obj);
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
        public Phi copy(final Phi self) {
            return this.copy();
        }

        @Override
        public byte[] delta() {
            return this.object.delta();
        }

        /**
         * Convert to Phi object.
         * @param obj Object to convert
         * @return Constructed Phi
         * @checkstyle CyclomaticComplexityCheck (100 lines)
         */
        @SuppressWarnings("PMD.CognitiveComplexity")
        private static Phi toPhi(final Object obj) {
            final Phi phi;
            final Phi eolang = Phi.Φ.take("org").take("eolang");
            if (obj instanceof Boolean) {
                if (obj.equals(true)) {
                    phi = eolang.take("true");
                } else {
                    phi = eolang.take("false");
                }
            } else if (obj instanceof Phi[]) {
                Phi tuple = eolang.take("tuple").take("empty");
                for (final Phi element : (Phi[]) obj) {
                    tuple = tuple.take("with");
                    tuple.put(0, element);
                }
                phi = tuple;
            } else if (obj instanceof byte[]) {
                phi = eolang.take("bytes").copy();
                phi.put(0, new PhDefault((byte[]) obj));
            } else if (obj instanceof Number) {
                final double value = ((Number) obj).doubleValue();
                if (Double.isNaN(value)) {
                    phi = eolang.take("nan");
                } else if (value == Double.POSITIVE_INFINITY) {
                    phi = eolang.take("positive-infinity");
                } else if (value == Double.NEGATIVE_INFINITY) {
                    phi = eolang.take("negative-infinity");
                } else {
                    phi = eolang.take("number").copy();
                    final Phi bts = eolang.take("bytes").copy();
                    bts.put(0, new PhDefault(new BytesOf(value).take()));
                    phi.put(0, bts);
                }
            } else if (obj instanceof String) {
                phi = eolang.take("string").copy();
                final Phi bts = eolang.take("bytes").copy();
                bts.put(0, new PhDefault(((String) obj).getBytes(StandardCharsets.UTF_8)));
                phi.put(0, bts);
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        "Unknown type of data: %s",
                        obj.getClass().getCanonicalName()
                    )
                );
            }
            return phi;
        }
    }
}
