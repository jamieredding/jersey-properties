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

package dev.coldhands.jersey.properties.java11.injection;

import dev.coldhands.jersey.properties.injection.Property;
import dev.coldhands.jersey.properties.injection.PropertyDeserialiser;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import java.lang.reflect.*;

class PropertyInjectionResolver implements InjectionResolver<Property> {

    @Inject
    private PropertyDeserialiser propertyDeserialiser;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
        final Property propertyAnnotation = locateAnnotation(injectee);
        final String propertyName = propertyAnnotation.value();

        return propertyDeserialiser.deserialise(propertyName, getInjectionSiteClass(injectee, propertyName));
    }

    private Property locateAnnotation(Injectee injectee) {
        final AnnotatedElement parent = injectee.getParent();

        if (parent instanceof Constructor<?>) {
            final var constructor = (Constructor<?>) parent;
            final Parameter[] parameters = constructor.getParameters();
            final Parameter paramToInject = parameters[injectee.getPosition()];
            return paramToInject.getAnnotation(Property.class);
        } else {
            return parent.getAnnotation(Property.class);
        }
    }

    private Class<?> getInjectionSiteClass(Injectee injectee, String propertyName) {
        final Type requiredType = injectee.getRequiredType();

        if (requiredType instanceof Class<?>) {
            return (Class<?>) requiredType;
        } else if (requiredType instanceof ParameterizedType) {
            throw new UnsupportedInjectionTargetException(requiredType, propertyName, ParameterizedType.class);
        } else if (requiredType instanceof GenericArrayType) {
            throw new UnsupportedInjectionTargetException(requiredType,propertyName, GenericArrayType.class);
        } else if (requiredType instanceof TypeVariable) {
            throw new UnsupportedInjectionTargetException(requiredType,propertyName, TypeVariable.class);
        }
        throw new UnsupportedInjectionTargetException(requiredType, propertyName, requiredType.getClass());
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
