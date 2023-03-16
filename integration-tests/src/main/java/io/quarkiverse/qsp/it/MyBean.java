package io.quarkiverse.qsp.it;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import io.quarkus.qute.TemplateGlobal;

@Named("foo")
@ApplicationScoped
public class MyBean {

    @TemplateGlobal
    public static final List<Integer> integers = List.of(1, 11, 42);

    public List<String> names() {
        return List.of("Joe", "Violet", "Omaha");
    }

}
