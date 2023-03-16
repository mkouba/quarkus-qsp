package io.quarkiverse.qsp.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class QspBuildTimeConfig {

    /**
     * All templates will be served relative to this path which is relative to the root path.
     * <p>
     * For example, a template located in `src/main/resource/templates/foo.html` will be served from the paths `/qsp/foo` and
     * `/qsp/foo.html` by default.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "/qsp")
    public String path;

}
