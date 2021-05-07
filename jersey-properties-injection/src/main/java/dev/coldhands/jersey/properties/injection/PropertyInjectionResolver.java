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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

class PropertyInjectionResolver implements InjectionResolver<Property> {

    @Inject
    private Provider<PropertyResolver> propertyResolverProvider;

    @Inject
    private Provider<ResolutionFailureBehaviour> resolutionFailureBehaviourProvider;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
        final Property propertyAnnotation = locateAnnotation(injectee);
        final String propertyName = propertyAnnotation.value();

        final String propertyValue = propertyResolverProvider.get()
                .getOptionalProperty(propertyName)
                .orElseGet(() -> resolutionFailureBehaviourProvider.get().onMissingProperty(propertyName));

        final Type requiredType = injectee.getRequiredType();
        if (requiredType.getTypeName().equals(Integer.class.getTypeName())) {
            return Integer.valueOf(propertyValue);
        }
        if (requiredType.getTypeName().equals(int.class.getTypeName())) {
            return Integer.parseInt(propertyValue);
        }
        return propertyValue;
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

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
