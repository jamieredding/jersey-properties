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

package dev.coldhands.jersey.properties.resolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class FileBasedPropertyResolver implements PropertyResolver {

    private final Properties properties;

    public FileBasedPropertyResolver(Path propertyFilePath) throws IOException {
        this(propertyFilePath, Files::newInputStream);
    }

    FileBasedPropertyResolver(Path propertyFilePath, InputStreamProvider inputStreamProvider) throws IOException {
        if (!Files.exists(propertyFilePath)) {
            throw new FileNotFoundException("Property file does not exist: " + propertyFilePath.toAbsolutePath());
        }

        properties = new Properties();

        try (final var inputStream = inputStreamProvider.getInputStream(propertyFilePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IOException("Problem reading property file: " + propertyFilePath.toAbsolutePath(), e);
        }
    }

    @Override
    public String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    @FunctionalInterface
    interface InputStreamProvider {
        InputStream getInputStream(Path path) throws IOException;
    }
}
