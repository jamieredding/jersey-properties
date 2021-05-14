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

import java.time.*;
import java.util.Map;
import java.util.Optional;
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
    void defaultDeserialiserRegistrySupports(Class<?> expectedClass, String serialisedValue, Object expectedDeserialisedValue) throws Exception {
        final Optional<Deserialiser<?>> optionalDeserialiser = DeserialiserRegistry.defaultRegistry().findForType(expectedClass.getTypeName());

        assertThat(optionalDeserialiser).isPresent();
        assertThat(optionalDeserialiser.get().deserialise(serialisedValue))
                .isEqualTo(expectedDeserialisedValue);
    }

    private static Stream<Arguments> expectedTypeAndDeserialisedValue() {
        return Stream.of(
                arguments(String.class, "abc", "abc"),
                arguments(Integer.class, "1", 1),
                arguments(int.class, "1", 1),
                arguments(Long.class, String.valueOf(Integer.MAX_VALUE + 1L), Integer.MAX_VALUE + 1L),
                arguments(long.class, String.valueOf(Integer.MAX_VALUE + 1L), Integer.MAX_VALUE + 1L),
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
                arguments(byte.class, "1", (byte) 1),

                arguments(Duration.class, "PT48H", Duration.ofDays(2)),
                arguments(Instant.class, "2021-05-10T18:50:05.684484732Z", Instant.ofEpochSecond(1620672605, 684484732)),
                arguments(LocalDate.class, "2021-05-10", LocalDate.of(2021, 5, 10)),
                arguments(LocalDateTime.class, "2021-05-10T19:50:00", LocalDateTime.of(2021, 5, 10, 19, 50)),
                arguments(LocalTime.class, "19:50:01", LocalTime.of(19, 50, 1)),
                arguments(MonthDay.class, "--05-10", MonthDay.of(5, 10)),
                arguments(OffsetDateTime.class, "2021-05-10T19:50:07.054303449+01:00", OffsetDateTime.of(2021, 5, 10, 19, 50, 7, 54303449, ZoneOffset.ofHours(1))),
                arguments(OffsetTime.class, "19:50:07.311069314+01:00", OffsetTime.of(19, 50, 7, 311069314, ZoneOffset.ofHours(1))),
                arguments(Period.class, "P10Y", Period.ofYears(10)),
                arguments(Year.class, "2021", Year.of(2021)),
                arguments(YearMonth.class, "2021-05", YearMonth.of(2021, 5)),
                arguments(ZonedDateTime.class, "2021-05-10T19:50:08.419688569+01:00[Europe/London]", ZonedDateTime.of(2021, 5, 10, 19, 50, 8, 419688569, ZoneId.of("Europe/London"))),
                arguments(ZoneId.class, "Europe/London", ZoneId.of("Europe/London")),
                arguments(ZoneOffset.class, "+10:00", ZoneOffset.ofHours(10))
        );
    }
}