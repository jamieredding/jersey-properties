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

package dev.coldhands.jersey.properties.java11;

import com.sun.net.httpserver.HttpServer;
import dev.coldhands.jersey.properties.resolver.PropertyResolver;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import static dev.coldhands.jersey.properties.java11.TestHttpServerFactory.anyOpenPort;
import static java.net.http.HttpClient.newHttpClient;
import static org.assertj.core.api.Assertions.assertThat;

class PropertyResolverFeatureTest {

    private final URI baseUri = UriBuilder.fromUri("http://localhost/").port(anyOpenPort()).build();
    private HttpServer httpServer;

    @AfterEach
    void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void whenPassedAPropertyResolver_thenRegisterThisResolverForInjectionIntoOtherClasses() throws IOException, InterruptedException {
        httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                .register(PropertyLookupResource.class)
                .register(new PropertyResolverFeature(propertyName -> Map.of("port", "8080").get(propertyName))));

        final HttpResponse<String> response = newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(UriBuilder.fromUri(baseUri)
                                .path("/property")
                                .queryParam("name", "port")
                                .build())
                        .build(),
                BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("8080");
    }

    @Path("/property")
    public static class PropertyLookupResource {

        @Inject
        PropertyResolver propertyResolver;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty(@QueryParam("name") String propertyName) {
            return Response.ok()
                    .entity(propertyResolver.getProperty(propertyName))
                    .build();
        }
    }
}
