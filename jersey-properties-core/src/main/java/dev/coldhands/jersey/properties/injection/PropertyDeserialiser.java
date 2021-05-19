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

import dev.coldhands.jersey.properties.resolver.PropertyResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class PropertyDeserialiser {

    private final Supplier<PropertyResolver> propertyResolverSupplier;
    private final Supplier<ResolutionFailureBehaviour> resolutionFailureBehaviourSupplier;
    private final Iterable<DeserialiserRegistry> deserialiserRegistries;

    private PropertyDeserialiser(Supplier<PropertyResolver> propertyResolverSupplier, Supplier<ResolutionFailureBehaviour> resolutionFailureBehaviourSupplier, Iterable<DeserialiserRegistry> deserialiserRegistries) {
        this.propertyResolverSupplier = propertyResolverSupplier;
        this.resolutionFailureBehaviourSupplier = resolutionFailureBehaviourSupplier;
        this.deserialiserRegistries = deserialiserRegistries;
    }

    // todo add overloaded method that takes Property annotation
    public <T> T deserialise(String propertyName, Class<T> requiredType) {
        final String propertyValue = lookupPropertyValue(propertyName);

        return deserialiseValueToCorrectType(propertyName, propertyValue, requiredType);
    }

    private String lookupPropertyValue(String propertyName) {
        return propertyResolverSupplier.get()
                .getOptionalProperty(propertyName)
                .orElseGet(() -> resolutionFailureBehaviourSupplier.get().onMissingProperty(propertyName));
    }

    private <T> T deserialiseValueToCorrectType(String propertyName, String propertyValue, Class<T> requiredType) {
        return getDeserialiser(requiredType)
                .map(deserialiser -> {
                    try {
                        return deserialiser.deserialise(propertyValue);
                    } catch (Exception e) {
                        throw new DeserialiserException(propertyName, propertyValue, requiredType, e);
                    }
                })
                .orElseThrow(() -> new MissingDeserialiserException(requiredType));
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Supplier<PropertyResolver> propertyResolverSupplier;
        private Supplier<ResolutionFailureBehaviour> resolutionFailureBehaviourSupplier = ResolutionFailureBehaviour::defaultBehaviour;
        private Iterable<DeserialiserRegistry> deserialiserRegistries = List.of(DeserialiserRegistry.defaultRegistry());

        public Builder withPropertyResolver(PropertyResolver propertyResolver) {
            return withPropertyResolver(() -> propertyResolver);
        }

        public Builder withPropertyResolver(Supplier<PropertyResolver> propertyResolverSupplier) {
            this.propertyResolverSupplier = propertyResolverSupplier;
            return this;
        }

        public Builder withResolutionFailureBehaviour(ResolutionFailureBehaviour resolutionFailureBehaviour) {
            return withResolutionFailureBehaviour(() -> resolutionFailureBehaviour);
        }

        public Builder withResolutionFailureBehaviour(Supplier<ResolutionFailureBehaviour> resolutionFailureBehaviourSupplier) {
            this.resolutionFailureBehaviourSupplier = resolutionFailureBehaviourSupplier;
            return this;
        }

        public Builder withDeserialiserRegistries(Iterable<DeserialiserRegistry> deserialiserRegistries) {
            this.deserialiserRegistries = deserialiserRegistries;
            return this;
        }

        public PropertyDeserialiser build() {
            return new PropertyDeserialiser(propertyResolverSupplier, resolutionFailureBehaviourSupplier, deserialiserRegistries);
        }
    }

}