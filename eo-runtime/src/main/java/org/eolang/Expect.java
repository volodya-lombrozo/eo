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

/**
 * This wrapper helps us explain our expectations in an error
 * message that we throw.
 *
 * @param <T> Type of returned value
 * @since 0.41.0
 */
public final class Expect<T> {

    /**
     * The action.
     */
    private final Action<T> action;

    /**
     * The message.
     */
    private final String message;

    /**
     * Ctor.
     * @param act The action
     * @param msg Additional explanation
     */
    public Expect(final Action<T> act, final String msg) {
        this.action = act;
        this.message = msg;
    }

    /**
     * Take the value from the lambda.
     * @return The value
     * @checkstyle MethodNameCheck (3 lines)
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public T it() {
        try {
            return this.action.act();
        } catch (final ExFailure ex) {
            throw new ExFailure(this.message, ex);
        }
    }
}
