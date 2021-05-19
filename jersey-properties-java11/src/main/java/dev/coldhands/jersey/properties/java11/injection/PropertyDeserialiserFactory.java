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

import dev.coldhands.jersey.properties.injection.DeserialiserRegistry;
import dev.coldhands.jersey.properties.injection.PropertyDeserialiser;
import dev.coldhands.jersey.properties.injection.ResolutionFailureBehaviour;
import dev.coldhands.jersey.properties.resolver.PropertyResolver;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;

class PropertyDeserialiserFactory implements Factory<PropertyDeserialiser> {

    @Inject
    private Provider<PropertyResolver> propertyResolverProvider;
    @Inject
    private Provider<ResolutionFailureBehaviour> resolutionFailureBehaviourProvider;
    @Inject
    private IterableProvider<DeserialiserRegistry> deserialiserRegistries;

    @Override
    public PropertyDeserialiser provide() {
        return PropertyDeserialiser.builder()
                .withPropertyResolver(propertyResolverProvider::get)
                .withResolutionFailureBehaviour(resolutionFailureBehaviourProvider::get)
                .withDeserialiserRegistries(deserialiserRegistries)
                .build();
    }

    @Override
    public void dispose(PropertyDeserialiser instance) {

    }
}
