/*
 * ISC License Copyright (c) 2004-2010 by Internet Systems Consortium, Inc. ("ISC")
 *
 * Copyright (c) 2021 by Jamie Redding
 *
 * Permission to use, copy, modify, and /or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above copyright
 * notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH REGARD
 * TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
 * IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING
 * OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import com.sun.net.httpserver.HttpServer;
import dev.coldhands.jersey.properties.core.deserialise.Property;
import dev.coldhands.jersey.properties.core.deserialise.PropertyDeserialiser;
import dev.coldhands.jersey.properties.jakarta.PropertyInjectionFeature;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.model.internal.FeatureContextWrapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaExampleTest {

    private HttpServer httpServer;
    private final InjectionManagerCapture injectionManager = new InjectionManagerCapture();

    @AfterEach
    void stopHttpServer() {
        httpServer.stop(0);
    }

    @Test
    void basicUsage() {
        Map<String, String> properties = Map.of(
                "runCount", "10",
                "startDate", "2021-06-08");

        ResourceConfig resourceConfig = new ResourceConfig()
                // register the feature in your resource config to enable injection support
                // you can provide either a PropertyResolver and accept defaults
                // or a PropertyDeserialiser to allow customisation
                .register(new PropertyInjectionFeature(properties::get))
                // register all of your resources/binders etc.
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        // this class has fields/constructor params to inject properties into
                        bindAsContract(MyInjectionSites.class);
                    }
                })
                .register(injectionManager);
        httpServer = JdkHttpServerFactory.createHttpServer(URI.create("http://localhost:8080/"), resourceConfig);

        MyInjectionSites instance = injectionManager.get()
                .getInstance(MyInjectionSites.class);

        assertThat(instance.runCountString).isEqualTo("10");
        assertThat(instance.runCount).isEqualTo(10);
        assertThat(instance.startDate).isEqualTo(LocalDate.of(2021, 6, 8));
        assertThat(instance.startDateString).isEqualTo("2021-06-08");
        assertThat(instance.shouldRun).isEqualTo(true);
    }

    public static class MyInjectionSites {
        // use this annotation with the property name you want to inject
        @Property("runCount")
        // use a type that supports deserialisation
        private int runCount;
        @Property("runCount")
        private String runCountString;

        private final LocalDate startDate;
        private final String startDateString;
        private final boolean shouldRun;

        // injection into constructors is also supported
        public MyInjectionSites(@Property("startDate") LocalDate startDate,
                                @Property("startDate") String startDateString,
                                // you can inject the PropertyDeserialiser if you want to
                                // provide a default value for a property
                                PropertyDeserialiser propertyDeserialiser) {
            this.startDate = startDate;
            this.startDateString = startDateString;
            this.shouldRun = propertyDeserialiser.optionalDeserialise("nonExistingProperty", boolean.class)
                    .orElse(true);
        }
    }

    private static class InjectionManagerCapture implements Feature {

        private InjectionManager injectionManager;

        @Override
        public boolean configure(FeatureContext context) {
            injectionManager = ((FeatureContextWrapper) context).getInjectionManager();
            return true;
        }

        public InjectionManager get() {
            return injectionManager;
        }
    }
}
