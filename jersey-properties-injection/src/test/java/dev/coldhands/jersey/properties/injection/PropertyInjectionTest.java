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

package dev.coldhands.jersey.properties.injection;

import com.sun.net.httpserver.HttpServer;
import dev.coldhands.jersey.properties.resolver.PropertyResolverFeature;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Random;

import static java.net.http.HttpClient.newHttpClient;
import static org.assertj.core.api.Assertions.assertThat;

class PropertyInjectionTest {

    private final URI baseUri = UriBuilder.fromUri("http://localhost/").port(anyOpenPort()).build();
    private HttpServer httpServer;

    @AfterEach
    void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"stringField", "stringConstructorArgument"})
    void whenAnnotatedWithProperty_thenInjectWithTheValueFoundInThePropertyResolver(String injectionLocation) throws IOException, InterruptedException {
        final String randomPortNumber = Integer.toString(new Random().nextInt());

        final var config = new ResourceConfig()
                .register(FieldInjectionPropertyLookupResource.class)
                .register(ConstructorInjectionPropertyLookupResource.class)
                .register(new PropertyResolverFeature(propertyName -> Map.of(injectionLocation, randomPortNumber).get(propertyName)))
                .register(PropertyInjectionFeature.class);

        httpServer = JdkHttpServerFactory.createHttpServer(baseUri, config);

        final HttpResponse<String> response = newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(UriBuilder.fromUri(baseUri)
                                .path("/" + injectionLocation)
                                .build())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(randomPortNumber);
    }

    @Path("/stringField")
    public static class FieldInjectionPropertyLookupResource {

        @Property("stringField")
        private String stringField;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty() {
            return Response.ok().entity(stringField).build();
        }
    }

    @Path("/stringConstructorArgument")
    public static class ConstructorInjectionPropertyLookupResource {

        private final String stringConstructorArgument;

        public ConstructorInjectionPropertyLookupResource(@Property("stringConstructorArgument") String stringConstructorArgument) {
            this.stringConstructorArgument = stringConstructorArgument;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty() {
            return Response.ok().entity(stringConstructorArgument).build();
        }
    }

    private int anyOpenPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't allocate a port", e);
        }
    }
}
