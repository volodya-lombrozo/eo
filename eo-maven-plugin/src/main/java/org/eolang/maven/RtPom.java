package org.eolang.maven;

import java.util.Optional;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.cactoos.Scalar;

final class RtPom implements Scalar<Dep> {
    private final MavenProject project;

    RtPom(final MavenProject mvn) {
        this.project = mvn;
    }

    @Override
    public Dep value() {
        return this.runtimeFromPom()
            .map(Dep::new)
            .orElseThrow(
                () -> new IllegalStateException("Runtime dependency not found in pom.xml")
            );
    }

    boolean isPresent() {
        return this.runtimeFromPom().isPresent();
    }

    /**
     * Runtime dependency from pom.xml.
     * @return Dependency if found
     */
    private Optional<Dependency> runtimeFromPom() {
        final Optional<Dependency> res;
        if (this.project == null) {
            res = Optional.empty();
        } else {
            res = this.project
                .getDependencies()
                .stream()
                .filter(RtPom::isRuntime)
                .findFirst();
        }
        return res;
    }

    /**
     * Checks if dependency is the eo-runtime artifact.
     * @param dep Dependency
     * @return True if runtime
     */
    private static boolean isRuntime(final Dependency dep) {
        return "org.eolang".equals(dep.getGroupId())
            && "eo-runtime".equals(dep.getArtifactId());
    }

}
