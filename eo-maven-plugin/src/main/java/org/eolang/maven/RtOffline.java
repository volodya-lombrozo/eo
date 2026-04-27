package org.eolang.maven;

import com.jcabi.manifests.Manifests;
import org.cactoos.Scalar;

final class RtOffline implements Scalar<Dep> {

    /**
     * EO current offline version.
     */
    private static final Dep EO_OFFLINE = new Dep().withGroupId("org.eolang")
        .withArtifactId("eo-runtime")
        .withVersion(Manifests.read("EO-Version"));

    @Override
    public Dep value() throws Exception {
        return RtOffline.EO_OFFLINE;
    }
}
