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

package dev.coldhands.jersey.properties.core.deserialise;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DeserialiserRegistry {

    private static final DeserialiserRegistry DEFAULT_REGISTRY = DeserialiserRegistry.builder()
            .put(String.class, s -> s)
            .put(Integer.class, Integer::parseInt)
            .put(int.class, Integer::parseInt)
            .put(Long.class, Long::parseLong)
            .put(long.class, Long::parseLong)
            .put(Short.class, Short::parseShort)
            .put(short.class, Short::parseShort)
            .put(Float.class, Float::parseFloat)
            .put(float.class, Float::parseFloat)
            .put(Double.class, Double::parseDouble)
            .put(double.class, Double::parseDouble)
            .put(Boolean.class, Boolean::parseBoolean)
            .put(boolean.class, Boolean::parseBoolean)
            .put(Character.class, s -> s.charAt(0))
            .put(char.class, s -> s.charAt(0))
            .put(Byte.class, Byte::parseByte)
            .put(byte.class, Byte::parseByte)

            .put(Duration.class, Duration::parse)
            .put(Instant.class, Instant::parse)
            .put(LocalDate.class, LocalDate::parse)
            .put(LocalDateTime.class, LocalDateTime::parse)
            .put(LocalTime.class, LocalTime::parse)
            .put(MonthDay.class, MonthDay::parse)
            .put(OffsetDateTime.class, OffsetDateTime::parse)
            .put(OffsetTime.class, OffsetTime::parse)
            .put(Period.class, Period::parse)
            .put(Year.class, Year::parse)
            .put(YearMonth.class, YearMonth::parse)
            .put(ZonedDateTime.class, ZonedDateTime::parse)
            .put(ZoneId.class, ZoneId::of)
            .put(ZoneOffset.class, ZoneOffset::of)
            .build();

    public static DeserialiserRegistry defaultRegistry() {
        return DEFAULT_REGISTRY;
    }

    private final Map<Class<?>, Deserialiser<?>> registry = new HashMap<>();

    private DeserialiserRegistry(Map<Class<?>, Deserialiser<?>> registryConfiguration) {
        registryConfiguration.forEach(registry::put);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<Deserialiser<T>> findForType(Class<T> typeName) {
        return (Optional) Optional.ofNullable(registry.get(typeName));
    }

    public static class Builder {

        private final Map<Class<?>, Deserialiser<?>> map = new HashMap<>();

        public <T> Builder put(Class<T> clazz, Deserialiser<T> deserialiser) {
            map.put(clazz, deserialiser);
            return this;
        }

        public DeserialiserRegistry build() {
            return new DeserialiserRegistry(map);
        }
    }
}
