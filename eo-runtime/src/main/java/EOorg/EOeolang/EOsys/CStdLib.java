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
/*
 * @checkstyle PackageNameCheck (4 lines)
 */
package EOorg.EOeolang.EOsys;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * C standard library with unix syscalls.
 * @since 0.40
 */
public interface CStdLib extends Library {

    /**
     * C STDLIB instance.
     */
    CStdLib CSTDLIB = Native.load("c", CStdLib.class);

    /**
     * The "getpid" syscall.
     *
     * @return Process ID.
     */
    int getpid();

    /**
     * The "write" syscall.
     *
     * @param descriptor File descriptor.
     * @param buf Buffer.
     * @param size Number of bytes to be written.
     * @return Number of bytes was written.
     */
    int write(Long descriptor, String buf, Long size);

    /**
     * The "read" syscall.
     *
     * @param descriptor File descriptor.
     * @param buf Buffer.
     * @param size Number of bytes to be read.
     * @return Number of bytes was read.
     */
    int read(Long descriptor, byte[] buf, Long size);
}
