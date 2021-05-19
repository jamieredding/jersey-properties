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

package dev.coldhands.jersey.properties.java11;

import dev.coldhands.jersey.properties.deserialise.Property;
import dev.coldhands.jersey.properties.deserialise.PropertyDeserialiser;
import dev.coldhands.jersey.properties.resolver.PropertyResolver;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class PropertyInjectionFeature implements Feature {

    private final PropertyDeserialiser propertyDeserialiser;

    public PropertyInjectionFeature(PropertyResolver propertyResolver) {
        if (propertyResolver == null) {
            throw new IllegalArgumentException("PropertyResolver must not be null");
        }
        this.propertyDeserialiser = PropertyDeserialiser.builder()
                .withPropertyResolver(() -> propertyResolver)
                .build();
    }

    public PropertyInjectionFeature(PropertyDeserialiser propertyDeserialiser) {
        if (propertyDeserialiser == null) {
            throw new IllegalArgumentException("PropertyDeserialiser must not be null");
        }
        this.propertyDeserialiser = propertyDeserialiser;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(propertyDeserialiser).to(PropertyDeserialiser.class);

                bind(PropertyInjectionResolver.class)
                        .to(new TypeLiteral<InjectionResolver<Property>>() {
                        })
                        .in(Singleton.class);
            }
        });

        return true;
    }
}
