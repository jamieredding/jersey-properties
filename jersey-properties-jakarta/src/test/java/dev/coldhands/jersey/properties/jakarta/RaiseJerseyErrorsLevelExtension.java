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

package dev.coldhands.jersey.properties.jakarta;

import org.glassfish.jersey.internal.Errors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

class RaiseJerseyErrorsLevelExtension implements BeforeEachCallback, AfterEachCallback {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(RaiseJerseyErrorsLevelExtension.class.getName());

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        if (jerseyErrorLoggingShouldBeDisabled()) {
            final Logger logger = getJerseyErrorLogger();
            extensionContext.getStore(NAMESPACE).put("oldLogLevel", logger.getLevel());
            logger.setLevel(Level.SEVERE);
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        if (jerseyErrorLoggingShouldBeDisabled()) {
            getJerseyErrorLogger().setLevel((Level) extensionContext.getStore(NAMESPACE).get("oldLogLevel"));
        }
    }

    private Logger getJerseyErrorLogger() {
        return Logger.getLogger(Errors.class.getName());
    }

    private Boolean jerseyErrorLoggingShouldBeDisabled() {
        return ofNullable(System.getProperty("disableJerseyLogging"))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }
}
