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
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import java.lang.reflect.*;
import java.util.Optional;

class PropertyInjectionResolver implements InjectionResolver<Property> {

    @Inject
    private Provider<PropertyResolver> propertyResolverProvider;

    @Inject
    private Provider<ResolutionFailureBehaviour> resolutionFailureBehaviourProvider;

    @Inject
    private DeserialiserRegistry deserialiserRegistry;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
        final Property propertyAnnotation = locateAnnotation(injectee);
        final String propertyName = propertyAnnotation.value();

        final String propertyValue = lookupPropertyValue(propertyName);

        return deserialiseValueToCorrectType(propertyName, propertyValue, injectee.getRequiredType());
    }

    private Property locateAnnotation(Injectee injectee) {
        final AnnotatedElement parent = injectee.getParent();

        if (parent instanceof Constructor<?> constructor) {
            final Parameter[] parameters = constructor.getParameters();
            final Parameter paramToInject = parameters[injectee.getPosition()];
            return paramToInject.getAnnotation(Property.class);
        } else {
            return parent.getAnnotation(Property.class);
        }
    }

    private String lookupPropertyValue(String propertyName) {
        return propertyResolverProvider.get()
                .getOptionalProperty(propertyName)
                .orElseGet(() -> resolutionFailureBehaviourProvider.get().onMissingProperty(propertyName));
    }

    private Object deserialiseValueToCorrectType(String propertyName, String propertyValue, Type requiredType) {
        final String typeName = requiredType.getTypeName();

        final Optional<Deserialiser<?>> deserialiser = deserialiserRegistry.findForType(typeName);
        if (deserialiser.isPresent()) {
            try {
                return deserialiser.get().deserialise(propertyValue);
            } catch (Exception e) {
                throw new DeserialiserException(propertyName, propertyValue, typeName, e);
            }
        } else {
            final Class<?> typeAsClass = (Class<?>) requiredType;
            if (typeAsClass.isEnum()) {
                try {
                    final Method method = typeAsClass.getDeclaredMethod("valueOf", String.class);
                    return method.invoke(typeAsClass, propertyValue);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new DeserialiserException(propertyName, propertyValue, typeName, e);
                }
            }
        }
        throw new MissingDeserialiserException(typeName);
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
