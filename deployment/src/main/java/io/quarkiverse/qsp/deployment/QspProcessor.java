package io.quarkiverse.qsp.deployment;

import io.quarkiverse.qsp.runtime.QspRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.qute.deployment.TemplateFilePathsBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

class QspProcessor {

    private static final String FEATURE = "qutepages";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RouteBuildItem produceTemplatesRoute(QspRecorder recorder, TemplateFilePathsBuildItem templateFilePaths,
            HttpRootPathBuildItem httpRootPath, QspBuildTimeConfig config) {
        return httpRootPath.routeBuilder()
                .routeFunction(httpRootPath.relativePath(config.path + "/*"), recorder.initializeRoute())
                .handler(recorder.handler(httpRootPath.relativePath(config.path), templateFilePaths.getFilePaths()))
                .build();
    }
}
