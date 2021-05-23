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

import dev.coldhands.jersey.properties.core.resolver.PropertyResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class PropertyDeserialiser {

    private final Supplier<PropertyResolver> propertyResolverSupplier;
    private final Iterable<DeserialiserRegistry> deserialiserRegistries;

    private PropertyDeserialiser(Supplier<PropertyResolver> propertyResolverSupplier, Iterable<DeserialiserRegistry> deserialiserRegistries) {
        this.propertyResolverSupplier = propertyResolverSupplier;
        this.deserialiserRegistries = deserialiserRegistries;
    }

    // todo add overloaded method that takes Property annotation
    public <T> T deserialise(String propertyName, Class<T> requiredType) throws PropertyException {
        final String propertyValue = lookupPropertyValue(propertyName);

        return deserialiseValueToCorrectType(propertyName, propertyValue, requiredType);
    }

    public <T> Optional<T> optionalDeserialise(String propertyName, Class<T> requiredType) {
        try {
            return Optional.of(deserialise(propertyName, requiredType));
        } catch (PropertyException e) {
            return Optional.empty();
        }
    }

    private String lookupPropertyValue(String propertyName) throws PropertyException {
        final Optional<String> optionalProperty = propertyResolverSupplier.get()
                .getOptionalProperty(propertyName);
        if (optionalProperty.isPresent()) {
            return optionalProperty.get();
        } else {
            throw new MissingPropertyException(propertyName);
        }
    }

    private <T> T deserialiseValueToCorrectType(String propertyName, String propertyValue, Class<T> requiredType) throws PropertyException {
        final Optional<Deserialiser<T>> deserialiser = getDeserialiser(requiredType);
        if (deserialiser.isPresent()) {
            try {
                return deserialiser.get().deserialise(propertyValue);
            } catch (Exception e) {
                throw new DeserialiserException(propertyName, propertyValue, requiredType, e);
            }
        } else {
            throw new MissingDeserialiserException(requiredType);
        }
    }

    private <T> Optional<Deserialiser<T>> getDeserialiser(Class<T> requiredType) {
        return findDeserialiserInRegistry(requiredType)
                .or(() -> {
                    if (requiredType.isEnum()) {
                        return Optional.of(new EnumDeserialiser<>(requiredType));
                    }
                    return Optional.empty();
                });
    }

    private <T> Optional<Deserialiser<T>> findDeserialiserInRegistry(Class<T> typeName) {
        return StreamSupport.stream(deserialiserRegistries.spliterator(), false)
                .map(dr -> dr.findForType(typeName))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static class EnumDeserialiser<T> implements Deserialiser<T> {
        private final Class<T> typeAsClass;

        public EnumDeserialiser(Class<T> typeAsClass) {
            this.typeAsClass = typeAsClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialise(String propertyValue) throws Exception {
            final Method method = typeAsClass.getDeclaredMethod("valueOf", String.class);
            return (T) method.invoke(typeAsClass, propertyValue);
        }
    }

    public static Builder builder(PropertyResolver propertyResolver) {
        checkNotNull(propertyResolver, "PropertyResolver");
        return new Builder(propertyResolver);
    }

    public static Builder builder(Supplier<PropertyResolver> propertyResolverSupplier) {
        checkNotNull(propertyResolverSupplier, "PropertyResolver");
        return new Builder(propertyResolverSupplier);
    }

    public static class Builder {

        private final Supplier<PropertyResolver> propertyResolverSupplier;

        private Iterable<DeserialiserRegistry> deserialiserRegistries = List.of(DeserialiserRegistry.defaultRegistry());

        private Builder(PropertyResolver propertyResolver) {
            this(() -> propertyResolver);
        }

        private Builder(Supplier<PropertyResolver> propertyResolverSupplier) {
            this.propertyResolverSupplier = propertyResolverSupplier;
        }

        public Builder withDeserialiserRegistries(Iterable<DeserialiserRegistry> deserialiserRegistries) {
            checkNotNull(deserialiserRegistries, "DeserialiserRegistries");
            this.deserialiserRegistries = deserialiserRegistries;
            return this;
        }

        public PropertyDeserialiser build() {
            return new PropertyDeserialiser(propertyResolverSupplier, deserialiserRegistries);
        }
    }

    private static <T> void checkNotNull(T object, String type) {
        if (object == null) {
            throw new IllegalArgumentException(type + " must not be null");
        }
    }

}