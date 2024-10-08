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
 * @checkstyle TrailingCommentCheck (3 lines)
 */
package EOorg.EOeolang.EOsys; // NOPMD

import EOorg.EOeolang.EOsys.Posix.GetenvSyscall;
import EOorg.EOeolang.EOsys.Posix.GetpidSyscall;
import EOorg.EOeolang.EOsys.Posix.ReadSyscall;
import EOorg.EOeolang.EOsys.Posix.WriteSyscall;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.eolang.Atom;
import org.eolang.Attr;
import org.eolang.Dataized;
import org.eolang.ExFailure;
import org.eolang.PhDefault;
import org.eolang.Phi;
import org.eolang.XmirObject;

/**
 * Posix syscall.
 *
 * @since 0.40
 * @checkstyle TypeNameCheck (100 lines)
 */
@XmirObject(oname = "posix.@")
@SuppressWarnings("PMD.AvoidDollarSigns")
public final class EOposix$EOφ extends PhDefault implements Atom {
    /**
     * System calls map.
     */
    static final Map<String, Function<Phi, Syscall>> SYS_CALLS = new HashMap<>();

    static {
        EOposix$EOφ.SYS_CALLS.put("getpid", GetpidSyscall::new);
        EOposix$EOφ.SYS_CALLS.put("read", ReadSyscall::new);
        EOposix$EOφ.SYS_CALLS.put("write", WriteSyscall::new);
        EOposix$EOφ.SYS_CALLS.put("getenv", GetenvSyscall::new);
    }

    @Override
    public Phi lambda() throws Exception {
        final Phi rho = this.take(Attr.RHO);
        final String call = new Dataized(rho.take("name")).asString();
        if (!EOposix$EOφ.SYS_CALLS.containsKey(call)) {
            throw new ExFailure(
                "Can't make posix syscall '%s' because it's either not supported yet or does not exist",
                call
            );
        }
        return EOposix$EOφ.SYS_CALLS.get(call).apply(rho).make(
            new TupleToArray(rho.take("args")).get()
        );
    }
}
