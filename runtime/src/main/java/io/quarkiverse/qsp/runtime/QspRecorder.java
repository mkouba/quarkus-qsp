package io.quarkiverse.qsp.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.LazyValue;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.qute.runtime.QuteRecorder.QuteContext;
import io.quarkus.qute.runtime.TemplateProducer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class QspRecorder {

    private static final Logger LOG = Logger.getLogger(QspRecorder.class);

    private final HttpBuildTimeConfig httpBuildTimeConfig;

    public QspRecorder(HttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    public Consumer<Route> initializeRoute() {
        return new Consumer<Route>() {

            @Override
            public void accept(Route r) {
                r.method(HttpMethod.GET);
            }
        };
    }

    public Handler<RoutingContext> handler(String rootPath, Set<String> templatePaths) {
        // TemplateProducer is singleton and we want to initialize lazily
        LazyValue<TemplateProducer> templateProducer = new LazyValue<>(
                () -> Arc.container().instance(TemplateProducer.class).get());
        LazyValue<QuteContext> quteContext = new LazyValue<>(() -> Arc.container().instance(QuteContext.class).get());
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext rc) {
                // Extract the template path, e.g. /qp/item.html -> item
                String path = extractTemplatePath(rc, rootPath);

                if (path != null && templatePaths.contains(path)) {
                    Template template = templateProducer.get().getInjectableTemplate(path);
                    TemplateInstance instance = template.instance();

                    List<MIMEHeader> acceptableTypes = rc.parsedHeaders().accept();
                    Variant selected = trySelectVariant(rc, instance, acceptableTypes);

                    if (selected != null) {
                        instance.setAttribute(TemplateInstance.SELECTED_VARIANT, selected);
                        rc.response().putHeader(HttpHeaders.CONTENT_TYPE, selected.getContentType());
                        // Only compress the response if the content type matches the config value
                        if (httpBuildTimeConfig.enableCompression
                                && httpBuildTimeConfig.compressMediaTypes.orElse(List.of())
                                        .contains(selected.getContentType())) {
                            String contentEncoding = rc.response().headers().get(HttpHeaders.CONTENT_ENCODING);
                            if (contentEncoding != null && HttpHeaders.IDENTITY.toString().equals(contentEncoding)) {
                                rc.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                            }
                        }
                    }

                    if (selected == null && !acceptableTypes.isEmpty()) {
                        // The Accept header is set but we are not able to select the appropriate variant
                        LOG.errorf("Appropriate template variant not found %s: %s",
                                acceptableTypes.stream().map(MIMEHeader::rawValue).collect(Collectors.toList()),
                                rc.request().path());
                        rc.response().setStatusCode(406).end();
                    } else {
                        instance.renderAsync().whenComplete((r, t) -> {
                            if (t != null) {
                                LOG.errorf(t, "Error occured during rendering template: %s", path);
                                rc.response().setStatusCode(500).end();
                            } else {
                                rc.response().setStatusCode(200).end(r);
                            }
                        });
                    }
                } else {
                    LOG.errorf("Template page not found: %s", rc.request().path());
                    rc.response().setStatusCode(404).end();
                }
            }

            private Variant trySelectVariant(RoutingContext rc, TemplateInstance instance, List<MIMEHeader> acceptableTypes) {
                Object variantsAttr = instance.getAttribute(TemplateInstance.VARIANTS);
                if (variantsAttr != null) {
                    @SuppressWarnings("unchecked")
                    List<Variant> variants = (List<Variant>) variantsAttr;
                    if (!acceptableTypes.isEmpty()) {
                        for (MIMEHeader accept : acceptableTypes) {
                            // https://github.com/vert-x3/vertx-web/issues/2388
                            accept.value();
                            for (Variant variant : variants) {
                                if (new ContentType(variant.getContentType()).matches(accept.component(),
                                        accept.subComponent())) {
                                    return variant;
                                }
                            }
                        }
                    }
                }
                return null;
            }

            private String extractTemplatePath(RoutingContext rc, String rootPath) {
                String path = rc.request().path();
                if (path.length() > rootPath.length()) {
                    path = path.substring(rootPath.length());
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    if (path.contains(".")) {
                        Map<String, List<String>> allVariants = quteContext.get().getVariants();
                        for (Entry<String, List<String>> e : allVariants.entrySet()) {
                            if (e.getValue().contains(path)) {
                                path = e.getKey();
                                break;
                            }
                        }
                    }
                    return path;
                }
                return null;
            }
        };
    }
}
