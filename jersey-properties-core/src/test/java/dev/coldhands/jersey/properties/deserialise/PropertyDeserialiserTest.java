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

package dev.coldhands.jersey.properties.deserialise;

import dev.coldhands.jersey.properties.resolver.PropertyResolver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PropertyDeserialiserTest {

    private static final Map<String, String> PROPERTIES = Map.ofEntries(
            entry("stringField", "abc"),
            entry("integerField", "123"),
            entry("intField", "456"),
            entry("enumField", MyEnum.VALUE.name())
    );

    private enum MyEnum {VALUE}

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
    void whenPropertyExists_thenDeserialiseForExpectedType(String propertyName, TypeResolver typeResolver, Class<?> expectedJavaType) throws PropertyException {
        final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                .build();

        assertThat(underTest.deserialise(propertyName, expectedJavaType))
                .isEqualTo(typeResolver.resolve(PROPERTIES.get(propertyName)));
    }

    @Test
    void whenMultipleDeserialiserRegistriesAreConfiguredToSupportAType_thenDeserialiseWithFirstThatSupportsThatType() throws PropertyException {
        final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                .withDeserialiserRegistries(List.of(
                        DeserialiserRegistry.builder().put(String.class, s -> "overriddenValue").build(),
                        DeserialiserRegistry.defaultRegistry()))
                .build();

        assertThat(underTest.deserialise("stringField", String.class))
                .isEqualTo("overriddenValue");
    }

    @Test
    void whenMultipleDeserialiserRegistriesAreConfiguredButOnlySecondSupportsThatType_thenUseDeserialiserInSecondRegistry() throws PropertyException {
        final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                .withDeserialiserRegistries(List.of(
                        DeserialiserRegistry.builder().put(String.class, s -> "overriddenValue").build(),
                        DeserialiserRegistry.defaultRegistry()))
                .build();

        assertThat(underTest.deserialise("integerField", Integer.class))
                .isEqualTo(123);
    }

    @Nested
    class PropertyMissing {

        @Test
        void whenPropertyIsMissing_thenThrowExceptionToCauseResolutionToFail() {
            final var underTest = PropertyDeserialiser.builder(propertyName -> null).build();

            assertThatThrownBy(() -> underTest.deserialise("anyProperty", String.class))
                    .isInstanceOf(PropertyException.class)
                    .isInstanceOf(MissingPropertyException.class)
                    .hasMessage("Could not find property with name: anyProperty");
        }

    }

    @Nested
    class MissingDeserialiser {

        @Test
        void whenNoDeserialiserConfiguredForThatType_thenThrowExceptionToCauseResolutionToFail() {
            final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                    .withDeserialiserRegistries(List.of(DeserialiserRegistry.builder().build()))
                    .build();

            assertThatThrownBy(() -> underTest.deserialise("stringField", String.class))
                    .isInstanceOf(PropertyException.class)
                    .isInstanceOf(MissingDeserialiserException.class)
                    .hasMessage("No deserialiser configured for type: " + String.class.getTypeName());
        }
    }

    @Nested
    class UnableToDeserialise {

        @Test
        void whenExceptionThrownWhileDeserialising_thenThrowExceptionToCauseResolutionToFail() {
            final var underTest = PropertyDeserialiser.builder(s -> "propertyValue")
                    .withDeserialiserRegistries(List.of(DeserialiserRegistry.builder()
                            .put(String.class, s -> {
                                throw new RuntimeException("Cannot deserialise a string.");
                            })
                            .build()))
                    .build();

            assertThatThrownBy(() -> underTest.deserialise("propertyName", String.class))
                    .isInstanceOf(PropertyException.class)
                    .isInstanceOf(DeserialiserException.class)
                    .hasMessage("Exception thrown while deserialising property: propertyName=propertyValue as type: " + String.class.getTypeName())
                    .getCause()
                    .hasMessage("Cannot deserialise a string.");
        }

        @Test
        void whenExceptionThrownWhileAutomaticallyDeserialisingEnum_thenThrowExceptionToCauseResolutionToFail() {
            final var expectedPropertyValue = "Value not in " + MyEnum.class;
            final var underTest = PropertyDeserialiser.builder(s -> expectedPropertyValue).build();

            assertThatThrownBy(() -> underTest.deserialise("invalidEnum", MyEnum.class))
                    .isInstanceOf(PropertyException.class)
                    .isInstanceOf(DeserialiserException.class)
                    .hasMessage("Exception thrown while deserialising property: invalidEnum=" + expectedPropertyValue + " as type: " + MyEnum.class.getTypeName())
                    .getCause()
                    .isInstanceOf(InvocationTargetException.class)
                    .getCause()
                    .hasMessageContainingAll("No enum constant", MyEnum.class.getSimpleName(), expectedPropertyValue);
        }
    }

    @Nested
    class OptionalDeserialise {

        @Test
        void whenPropertyCanBeDeserialised_thenReturnOptionalOfValue() {
            final var underTest = PropertyDeserialiser.builder(PROPERTIES::get).build();

            assertThat(underTest.optionalDeserialise("intField", int.class))
                    .contains(456);
        }

        @Test
        void whenPropertyIsMissing_thenReturnOptionalEmpty() {
            final var underTest = PropertyDeserialiser.builder(propertyName -> null).build();

            assertThat(underTest.optionalDeserialise("intField", int.class))
                    .isEmpty();
        }

        @Test
        void whenMissingDeserialiser_thenReturnOptionalEmpty() {
            final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                    .withDeserialiserRegistries(List.of(DeserialiserRegistry.builder().build()))
                    .build();

            assertThat(underTest.optionalDeserialise("intField", int.class))
                    .isEmpty();
        }

        @Test
        void whenUnableToDeserialise_thenReturnOptionalEmpty() {
            final var underTest = PropertyDeserialiser.builder(PROPERTIES::get)
                    .withDeserialiserRegistries(List.of(DeserialiserRegistry.builder()
                            .put(int.class, i -> {
                                throw new RuntimeException("Won't deserialise");
                            })
                            .build()))
                    .build();

            assertThat(underTest.optionalDeserialise("intField", int.class))
                    .isEmpty();
        }
    }

    @Nested
    class BuilderFieldsAreNull {

        @Test
        void whenPropertyResolverIsNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> PropertyDeserialiser.builder((PropertyResolver) null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("PropertyResolver must not be null");
        }

        @Test
        void whenPropertyResolverSupplierIsNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> PropertyDeserialiser.builder((Supplier<PropertyResolver>) null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("PropertyResolver must not be null");
        }

        @Test
        void whenDeserialiserRegistriesAreNull_thenThrowIllegalArgumentException() {
            assertThatThrownBy(() -> PropertyDeserialiser.builder(a -> a)
                    .withDeserialiserRegistries(null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("DeserialiserRegistries must not be null");
        }
    }
}
