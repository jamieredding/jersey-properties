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
import dev.coldhands.jersey.properties.TestHttpServerFactory;
import dev.coldhands.jersey.properties.resolver.PropertyResolverFeature;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.hk2.api.MultiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static dev.coldhands.jersey.properties.TestHttpServerFactory.anyOpenPort;
import static dev.coldhands.jersey.properties.injection.TestResources.*;
import static java.net.http.HttpClient.newHttpClient;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PropertyInjectionResolverTest {

    private static final Map<String, String> PROPERTIES = Map.ofEntries(
            entry("stringField", "abc"),
            entry("integerField", "123"),
            entry("intField", "456"),
            entry("enumField", MyEnum.VALUE.name())
    );

    private final URI baseUri = UriBuilder.fromUri("http://localhost/").port(anyOpenPort()).build();
    private HttpServer httpServer;

    @AfterEach
    void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @FunctionalInterface
    interface TypeResolver {
        Object resolve(String s);
    }

    static Stream<Arguments> fieldNameToValueAndType() {
        return Stream.of(
                arguments("stringField", (TypeResolver) s -> s, String.class),
                arguments("integerField", (TypeResolver) Integer::valueOf, Integer.class),
                arguments("intField", (TypeResolver) Integer::parseInt, Integer.class),
                arguments("enumField", (TypeResolver) MyEnum::valueOf, MyEnum.class));
    }

    @ParameterizedTest
    @MethodSource("fieldNameToValueAndType")
    void whenFieldAnnotatedProperty_thenInjectWithTheTypeResolvedValueFoundInThePropertyResolver(String fieldName, TypeResolver typeResolver, Class<?> expectedJavaType) throws IOException, InterruptedException {
        httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                .register(FieldInjectionPropertyLookupResource.class)
                .register(new PropertyResolverFeature(PROPERTIES::get))
                .register(PropertyInjectionFeature.class));

        final HttpResponse<String> response = newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(UriBuilder.fromUri(baseUri)
                                .path("/fieldInjection")
                                .queryParam("type", fieldName)
                                .build())
                        .build(),
                BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(typeResolver.resolve(response.body())).isEqualTo(typeResolver.resolve(PROPERTIES.get(fieldName)));
        assertThat(response.headers().firstValue("javaType")).hasValue(expectedJavaType.getTypeName());
    }

    @ParameterizedTest
    @MethodSource("fieldNameToValueAndType")
    void whenConstructorAnnotatedWithProperty_thenInjectWithTheTypeResolvedValueFoundInThePropertyResolver(String fieldName, TypeResolver typeResolver, Class<?> expectedJavaType) throws IOException, InterruptedException {
        httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                .register(ConstructorInjectionPropertyLookupResource.class)
                .register(new PropertyResolverFeature(PROPERTIES::get))
                .register(PropertyInjectionFeature.class));

        final HttpResponse<String> response = newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(UriBuilder.fromUri(baseUri)
                                .path("/constructorInjection")
                                .queryParam("type", fieldName)
                                .build())
                        .build(),
                BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(typeResolver.resolve(response.body())).isEqualTo(typeResolver.resolve(PROPERTIES.get(fieldName)));
        assertThat(response.headers().firstValue("javaType")).hasValue(expectedJavaType.getTypeName());
    }

    @Test
    void whenACustomDeserialiserRegistryIsProvided_thenDeserialiseWithThatInsteadOfTheDefault() throws IOException, InterruptedException {
        httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                .register(FieldInjectionPropertyLookupResource.class)
                .register(new PropertyResolverFeature(PROPERTIES::get))
                .register(new PropertyInjectionFeature()
                        .withAdditionalDeserialiserRegistry(DeserialiserRegistry.builder().put(String.class, s -> "overriddenValue").build())));

        final HttpResponse<String> response = newHttpClient().send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(UriBuilder.fromUri(baseUri)
                                .path("/fieldInjection")
                                .queryParam("type", "stringField")
                                .build())
                        .build(),
                BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("overriddenValue");
        assertThat(response.headers().firstValue("javaType")).hasValue(String.class.getTypeName());
    }

    @Nested
    class PropertyMissing {

        @Test
        void defaultBehaviour_whenPropertyIsMissing_thenInjectPropertyName() throws IOException, InterruptedException {
            httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                    .register(StringPropertyLookupResource.class)
                    .register(new PropertyResolverFeature(propertyName -> null))
                    .register(PropertyInjectionFeature.class));

            final HttpResponse<String> response = newHttpClient().send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(UriBuilder.fromUri(baseUri)
                                    .path("/stringProperty")
                                    .build())
                            .build(),
                    BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("propertyName");
        }

        @Test
        void configuredBehaviour_whenPropertyIsMissing_thenInjectPropertyName() throws IOException, InterruptedException {
            httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                    .register(StringPropertyLookupResource.class)
                    .register(new PropertyResolverFeature(propertyName -> null))
                    .register(new PropertyInjectionFeature()
                            .withResolutionFailureBehaviour(propertyName -> propertyName + "-value")));

            final HttpResponse<String> response = newHttpClient().send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(UriBuilder.fromUri(baseUri)
                                    .path("/stringProperty")
                                    .build())
                            .build(),
                    BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("propertyName-value");
        }

    }

    @Nested
    class ExceptionHandling {

        @Test
        void whenExceptionThrownFromPropertyDeserialiser_thenThrowExceptionToCauseResolutionToFail() throws IOException, InterruptedException {
            final var countDownLatch = new CountDownLatch(1);
            final var exceptionCapture = new ExceptionCapture();
            httpServer = TestHttpServerFactory.createHttpServer(baseUri, config -> config
                    .register(StringPropertyLookupResource.class)
                    .register(new PropertyResolverFeature(PROPERTIES::get))
                    .register(new PropertyInjectionFeature()
                            .withDefaultDeserialiserRegistry(DeserialiserRegistry.builder().build()))
                    .register(new AssertingRequestEventListener(countDownLatch, exceptionCapture)));

            newHttpClient().send(
                    HttpRequest.newBuilder()
                            .GET()
                            .uri(UriBuilder.fromUri(baseUri)
                                    .path("/stringProperty")
                                    .build())
                            .build(),
                    BodyHandlers.ofString());

            countDownLatch.await();
            final Throwable actualException = exceptionCapture.getException();
            assertThat(actualException).isInstanceOf(MultiException.class);
            assertThat(((MultiException) actualException).getErrors())
                    .anySatisfy(throwable ->
                            assertThat(throwable).isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("Unable to perform operation: resolve"))
                    .anySatisfy(throwable ->
                            assertThat(throwable)
                                    .isInstanceOf(MissingDeserialiserException.class)
                                    .hasMessage("No deserialiser configured for type: " + String.class.getTypeName()));

        }
    }

}
