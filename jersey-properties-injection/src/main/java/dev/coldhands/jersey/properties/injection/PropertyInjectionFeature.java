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

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

class PropertyInjectionFeature implements Feature {

    private ResolutionFailureBehaviour resolutionFailureBehaviour= ResolutionFailureBehaviour.defaultBehaviour();
    private DeserialiserRegistry deserialiserRegistry = DeserialiserRegistry.defaultRegistry();
    private DeserialiserRegistry additionalDeserialiserRegistry;

    public PropertyInjectionFeature withResolutionFailureBehaviour(ResolutionFailureBehaviour resolutionFailureBehaviour) {
        this.resolutionFailureBehaviour = resolutionFailureBehaviour;
        return this;
    }

    public PropertyInjectionFeature withDefaultDeserialiserRegistry(DeserialiserRegistry deserialiserRegistry) {
        this.deserialiserRegistry = deserialiserRegistry;
        return this;
    }

    public PropertyInjectionFeature withAdditionalDeserialiserRegistry(DeserialiserRegistry deserialiserRegistry) {
        additionalDeserialiserRegistry = deserialiserRegistry;
        return this;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(resolutionFailureBehaviour).to(ResolutionFailureBehaviour.class);
                bind(PropertyInjectionResolver.class)
                        .to(new TypeLiteral<InjectionResolver<Property>>() {
                        })
                        .in(Singleton.class);
                bind(deserialiserRegistry).to(DeserialiserRegistry.class).ranked(0);
                if (additionalDeserialiserRegistry != null) {
                    bind(additionalDeserialiserRegistry).to(DeserialiserRegistry.class).ranked(1);
                }
            }
        });

        return true;
    }
}
