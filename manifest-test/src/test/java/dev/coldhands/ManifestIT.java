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

package dev.coldhands;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestIT {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "jersey-properties-core, dev.coldhands.jersey.properties.core",
            "jersey-properties-jakarta, dev.coldhands.jersey.properties.jakarta"
    })
    void dependenciesManifestHasAutomaticModuleName(String dependencyName, String expectedAutomaticModuleName) throws IOException {
        final Optional<Manifest> potentialManifest = getManifestUrlForDependency(dependencyName)
                .map(throwing(URL::openStream))
                .map(throwing(Manifest::new));

        assertThat(potentialManifest)
                .isPresent();

        final Manifest manifest = potentialManifest.get();

        assertThat(manifest.getMainAttributes().getValue("Automatic-Module-Name"))
                .isEqualTo(expectedAutomaticModuleName);
    }

    private Optional<URL> getManifestUrlForDependency(String dependencyName) throws IOException {
        final var manifestUrls = getClass().getClassLoader()
                .getResources(JarFile.MANIFEST_NAME)
                .asIterator();

        Optional<URL> manifestUrl = Optional.empty();
        while (manifestUrls.hasNext()) {
            final URL url = manifestUrls.next();
            if (url.toString().contains(dependencyName)) {
                manifestUrl = Optional.of(url);
            }
        }
        return manifestUrl;
    }

    <T, V> Function<T, V> throwing(ThrowingFunction<T, V> throwingFunction) {
        return t -> {
            try {
                return throwingFunction.throwing(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, V> {

        V throwing(T t) throws Exception;
    }
}
