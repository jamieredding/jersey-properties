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
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class PropertyDeserialiser {

    private final Provider<PropertyResolver> propertyResolverProvider;
    private final Provider<ResolutionFailureBehaviour> resolutionFailureBehaviourProvider;
    private final Iterable<DeserialiserRegistry> deserialiserRegistries;

    @Inject
    public PropertyDeserialiser(Provider<PropertyResolver> propertyResolverProvider, Provider<ResolutionFailureBehaviour> resolutionFailureBehaviourProvider, Iterable<DeserialiserRegistry> deserialiserRegistries) {
        this.propertyResolverProvider = propertyResolverProvider;
        this.resolutionFailureBehaviourProvider = resolutionFailureBehaviourProvider;
        this.deserialiserRegistries = deserialiserRegistries;
    }

    public Object deserialise(String propertyName, Type requiredType) {
        final String propertyValue = lookupPropertyValue(propertyName);

        return deserialiseValueToCorrectType(propertyName, propertyValue, requiredType);
    }

    private String lookupPropertyValue(String propertyName) {
        return propertyResolverProvider.get()
                .getOptionalProperty(propertyName)
                .orElseGet(() -> resolutionFailureBehaviourProvider.get().onMissingProperty(propertyName));
    }

    private Object deserialiseValueToCorrectType(String propertyName, String propertyValue, Type requiredType) {
        final String typeName = requiredType.getTypeName();

        return getDeserialiser(requiredType, typeName)
                .map(deserialiser -> {
                    try {
                        return deserialiser.deserialise(propertyValue);
                    } catch (Exception e) {
                        throw new DeserialiserException(propertyName, propertyValue, typeName, e);
                    }
                })
                .orElseThrow(() -> new MissingDeserialiserException(typeName));
    }

    private Optional<Deserialiser<?>> getDeserialiser(Type requiredType, String typeName) {
        return findDeserialiserInRegistry(typeName)
                .or(() -> {
                    if (requiredType instanceof Class<?>) {
                        final var typeAsClass = (Class<?>) requiredType;
                        if (typeAsClass.isEnum()) {
                            return Optional.of(new EnumDeserialiser(typeAsClass));
                        }
                    }
                    return Optional.empty();
                });
    }

    private Optional<Deserialiser<?>> findDeserialiserInRegistry(String typeName) {
        return StreamSupport.stream(deserialiserRegistries.spliterator(), false)
                .map(dr -> dr.findForType(typeName))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static class EnumDeserialiser implements Deserialiser<Object> {
        private final Class<?> typeAsClass;

        public EnumDeserialiser(Class<?> typeAsClass) {
            this.typeAsClass = typeAsClass;
        }

        @Override
        public Object deserialise(String propertyValue) throws Exception {
            final Method method = typeAsClass.getDeclaredMethod("valueOf", String.class);
            return method.invoke(typeAsClass, propertyValue);
        }
    }
}