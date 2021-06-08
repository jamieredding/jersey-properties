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

import dev.coldhands.jersey.properties.core.deserialise.DeserialiserRegistry;
import dev.coldhands.jersey.properties.core.deserialise.PropertyDeserialiser;
import dev.coldhands.jersey.properties.core.deserialise.PropertyException;
import dev.coldhands.jersey.properties.core.resolver.FileBasedPropertyResolver;
import dev.coldhands.jersey.properties.core.resolver.PropertyResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class CoreExampleTest {

    @Nested
    class PropertyResolverExample {

        @TempDir
        Path tempDir;

        @Test
        void basicUsage() throws IOException {
            Path propertyFile = tempDir.resolve("app.properties");
            Files.writeString(propertyFile, "httpPort=8080");

            //  file-based PropertyResolver
            PropertyResolver propertyResolver = new FileBasedPropertyResolver(propertyFile);

            // existing property
            assertThat(propertyResolver.getProperty("httpPort")).isEqualTo("8080");
            // missing property
            assertThat(propertyResolver.getProperty("missingProperty")).isNull();

            // optional support
            assertThat(propertyResolver.getOptionalProperty("httpPort")).contains("8080");
            assertThat(propertyResolver.getOptionalProperty("missingProperty")).isEmpty();
        }

        @Test
        void customPropertyResolver() {
            // using a map to implement a PropertyResolver
            var properties = Map.of("httpPort", "8080");
            PropertyResolver propertyResolver = properties::get;

            assertThat(propertyResolver.getProperty("httpPort")).isEqualTo("8080");
            assertThat(propertyResolver.getProperty("missingProperty")).isNull();
        }
    }

    @Nested
    class PropertyDeserialiserExample {

        private final Map<String, String> properties = Map.of(
                "httpPort", "8080",
                "startDate", "2021-06-05",
                "enumProperty", "VALUE");
        private final PropertyResolver propertyResolver = properties::get;

        @Test
        void basicUsage() {
            // there is a builder to use for creating property deserialisers
            PropertyDeserialiser deserialiser = PropertyDeserialiser.builder(propertyResolver).build();

            try {
                // retrieve as String
                assertThat(deserialiser.deserialise("httpPort", String.class)).isEqualTo("8080");
                // retrieve as int
                assertThat(deserialiser.deserialise("httpPort", int.class)).isEqualTo(8080);

                // out-of-the-box support for some java types
                // see DeserialiserRegistry.defaultRegistry() for all types
                assertThat(deserialiser.deserialise("httpPort", long.class)).isEqualTo(8080L);
                assertThat(deserialiser.deserialise("startDate", LocalDate.class))
                        .isEqualTo(LocalDate.of(2021, 6, 5));
                // also supports enum deserialisation
                assertThat(deserialiser.deserialise("enumProperty", MyEnum.class))
                        .isEqualTo(MyEnum.VALUE);
            } catch (PropertyException e) {
                fail(e);
            }

            // supports optionals
            // this can allow specifying default values
            final LocalDate endDate = deserialiser.optionalDeserialise("endDate", LocalDate.class)
                    .orElse(LocalDate.of(2021, 6, 5));
            assertThat(endDate).isEqualTo(LocalDate.of(2021, 6, 5));
        }

        @Test
        void customDeserialisers() {
            class CustomData {
                final String a;
                final String b;

                CustomData(String a, String b) {
                    this.a = a;
                    this.b = b;
                }
            }

            var properties = Map.of(
                    "customData", "csv,data",
                    "someInt", "200");
            PropertyDeserialiser deserialiser = PropertyDeserialiser.builder(properties::get)
                    // you can pass a list of deserialiser registries to specify how to
                    // deserialise to a particular type
                    .withDeserialiserRegistries(List.of(
                            DeserialiserRegistry.builder()
                                    // register a deserialiser for your custom type
                                    .put(CustomData.class, string -> {
                                        String[] value = string.split(",");
                                        return new CustomData(value[0], value[1]);
                                    })
                                    // you can override the default deserialiser by registering
                                    // your own deserialiser in a registry that is ordered before
                                    // the default registry
                                    .put(int.class, string -> -1)
                                    .build(),
                            // if you want to use default deserialisers, this default registry
                            // must be included
                            DeserialiserRegistry.defaultRegistry()))
                    .build();
            try {
                CustomData customData = deserialiser.deserialise("customData", CustomData.class);
                assertThat(customData.a).isEqualTo("csv");
                assertThat(customData.b).isEqualTo("data");

                assertThat(deserialiser.deserialise("someInt", int.class)).isEqualTo(-1);
            } catch (PropertyException e) {
                fail(e);
            }
        }

        @Test
        void exceptionalBehaviour() {
            PropertyDeserialiser deserialiser = PropertyDeserialiser.builder(propertyResolver).build();

            try {
                // exception thrown when property doesn't exist
                deserialiser.deserialise("missingProperty", String.class);
                fail();
            } catch (PropertyException e) {
                assertThat(e).hasMessageContaining("missingProperty");
            }

            try {
                // exception thrown when type to deserialise to isn't supported
                deserialiser.deserialise("httpPort", HashMap.class);
                fail();
            } catch (PropertyException e) {
                assertThat(e).hasMessageContaining("HashMap");
            }

            try {
                // exception thrown when unable to deserialise value to type
                deserialiser.deserialise("startDate", int.class);
                fail();
            } catch (PropertyException e) {
                assertThat(e).hasMessageContaining("int");
            }
        }
    }

    public enum MyEnum {VALUE}
}
