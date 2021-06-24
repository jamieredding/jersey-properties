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

import java.io.File;
import java.nio.file.Path;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * DeserialiserRegistry provides a mapping from a Class type to
 * a particular {@link Deserialiser} to convert from a String to
 * an object of that Class type.
 *
 * <p>
 *     There is a {@link DeserialiserRegistry#defaultRegistry()} containing
 *     deserialiser mappings for a variety of java types.
 * </p>
 */
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
            .put(Path.class, Path::of)
            .put(File.class, File::new)
            .build();

    /**
     * A preconfigured registry containing {@link Deserialiser} instances
     * for a variety of java types.
     * @return the default registry
     */
    public static DeserialiserRegistry defaultRegistry() {
        return DEFAULT_REGISTRY;
    }

    private final Map<Class<?>, Deserialiser<?>> registry = new HashMap<>();

    private DeserialiserRegistry(Map<Class<?>, Deserialiser<?>> registryConfiguration) {
        registryConfiguration.forEach(registry::put);
    }

    /**
     * Factory method for creating a {@link DeserialiserRegistry}.
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Lookup a registered {@link Deserialiser} for a particular
     * Class type.
     * @param clazz a class whose type should have a deserialiser in
     *              this registry
     * @param <T> the type which should be deserialised to
     * @return an {@link Optional} containing a deserialiser for
     *         the specified type or empty if that class has not been
     *         registered
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<Deserialiser<T>> findForType(Class<T> clazz) {
        return (Optional) Optional.ofNullable(registry.get(clazz));
    }

    public static class Builder {

        private final Map<Class<?>, Deserialiser<?>> map = new HashMap<>();

        /**
         * Add a {@link Deserialiser} to class mapping to the registry
         * @param clazz a class whose type should have a deserialiser in
         *              this registry
         * @param deserialiser the deserialiser to used for that class
         * @param <T> the type which should be deserialised to
         * @return this builder instance
         */
        public <T> Builder put(Class<T> clazz, Deserialiser<T> deserialiser) {
            map.put(clazz, deserialiser);
            return this;
        }

        public DeserialiserRegistry build() {
            return new DeserialiserRegistry(map);
        }
    }
}
