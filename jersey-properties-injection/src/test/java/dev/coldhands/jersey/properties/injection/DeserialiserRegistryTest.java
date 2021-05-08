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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DeserialiserRegistryTest {

    @Test
    void findConfiguredDeserialiserForType() {
        final Deserialiser<String> stringDeserialiser = s -> s;
        final Deserialiser<Integer> integerDeserialiser = Integer::parseInt;

        final var underTest = new DeserialiserRegistry(Map.of(
                String.class, stringDeserialiser,
                Integer.class, integerDeserialiser));

        assertThat(underTest.findForType(String.class.getTypeName()))
                .hasValue(stringDeserialiser);
        assertThat(underTest.findForType(Integer.class.getTypeName()))
                .hasValue(integerDeserialiser);
    }

    @Test
    void whenThereIsNoDeserialiserForThatType_thenReturnOptionalEmpty() {
        final var underTest = new DeserialiserRegistry(Map.of(String.class, s -> s));

        assertThat(underTest.findForType(Object.class.getTypeName()))
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("expectedTypeAndDeserialisedValue")
    void defaultDeserialiserRegistrySupports(Class<?> expectedClass, String serialisedValue, Object expectedDeserialisedValue) {
        assertThat(DeserialiserRegistry.defaultRegistry().findForType(expectedClass.getTypeName()))
                .isPresent()
                .get()
                .satisfies(deserialiser ->
                        assertThat(deserialiser.deserialise(serialisedValue)).isEqualTo(expectedDeserialisedValue)
                );
    }

    private static Stream<Arguments> expectedTypeAndDeserialisedValue() {
        return Stream.of(
                arguments(String.class, "abc", "abc"),
                arguments(Integer.class, "1", 1),
                arguments(int.class, "1", 1),
                arguments(Long.class, String.valueOf(Integer.MAX_VALUE+1L), Integer.MAX_VALUE+1L),
                arguments(long.class, String.valueOf(Integer.MAX_VALUE+1L), Integer.MAX_VALUE+1L),
                arguments(Short.class, "1", (short) 1),
                arguments(short.class, "1", (short) 1),
                arguments(Float.class, "1.0", 1.0F),
                arguments(float.class, "1.0", 1.0F),
                arguments(Double.class, String.valueOf(Float.MAX_VALUE + 1.0D), Float.MAX_VALUE + 1.0D),
                arguments(double.class, String.valueOf(Float.MAX_VALUE + 1.0D), Float.MAX_VALUE + 1.0D),
                arguments(Boolean.class, "true", true),
                arguments(boolean.class, "true", true),
                arguments(Character.class, "a", 'a'),
                arguments(char.class, "a", 'a'),
                arguments(Byte.class, "1", (byte) 1),
                arguments(byte.class, "1", (byte) 1)
        );
    }
}