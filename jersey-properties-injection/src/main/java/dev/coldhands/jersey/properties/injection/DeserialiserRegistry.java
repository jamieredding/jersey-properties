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

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

class DeserialiserRegistry {

    private static final DeserialiserRegistry DEFAULT_REGISTRY = new DeserialiserRegistry(Map.ofEntries(
            entry(String.class, s -> s),
            entry(Integer.class, Integer::parseInt),
            entry(int.class, Integer::parseInt),
            entry(Long.class, Long::parseLong),
            entry(long.class, Long::parseLong),
            entry(Short.class, Short::parseShort),
            entry(short.class, Short::parseShort),
            entry(Float.class, Float::parseFloat),
            entry(float.class, Float::parseFloat),
            entry(Double.class, Double::parseDouble),
            entry(double.class, Double::parseDouble),
            entry(Boolean.class, Boolean::parseBoolean),
            entry(boolean.class, Boolean::parseBoolean),
            entry(Character.class, s -> s.charAt(0)),
            entry(char.class, s -> s.charAt(0)),
            entry(Byte.class, Byte::parseByte),
            entry(byte.class, Byte::parseByte),

            entry(Duration.class, Duration::parse),
            entry(Instant.class, Instant::parse),
            entry(LocalDate.class, LocalDate::parse),
            entry(LocalDateTime.class, LocalDateTime::parse),
            entry(LocalTime.class, LocalTime::parse),
            entry(MonthDay.class, MonthDay::parse),
            entry(OffsetDateTime.class, OffsetDateTime::parse),
            entry(OffsetTime.class, OffsetTime::parse),
            entry(Period.class, Period::parse),
            entry(Year.class, Year::parse),
            entry(YearMonth.class, YearMonth::parse),
            entry(ZonedDateTime.class, ZonedDateTime::parse),
            entry(ZoneId.class, ZoneId::of),
            entry(ZoneOffset.class, ZoneOffset::of)
    ));

    public static DeserialiserRegistry defaultRegistry() {
        return DEFAULT_REGISTRY;
    }

    private final Map<String, Deserialiser<?>> registry = new HashMap<>();

    public DeserialiserRegistry(Map<Class<?>, Deserialiser<?>> registryConfiguration) {
        registryConfiguration.forEach((clazz, deserialiser) ->
                registry.put(clazz.getTypeName(), deserialiser));
    }

    public Optional<Deserialiser<?>> findForType(String typeName) {
        return Optional.ofNullable(registry.get(typeName));
    }
}
