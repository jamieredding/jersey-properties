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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import java.util.concurrent.CountDownLatch;

class TestResources {

    @Path("/stringProperty")
    public static class StringPropertyLookupResource {

        @Property("propertyName")
        private String propertyValue;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty() {
            return Response.ok()
                    .entity(propertyValue)
                    .build();
        }
    }

    public enum MyEnum {
        VALUE
    }

    @Path("/fieldInjection")
    public static class FieldInjectionPropertyLookupResource {

        @Property("stringField")
        private String stringField;

        @Property("integerField")
        private Integer integerField;

        @Property("intField")
        private int intField;

        @Property("enumField")
        private MyEnum enumField;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty(@QueryParam("type") String type) {
            return getResponse(type, stringField, integerField, intField, enumField);
        }
    }

    @Path("/constructorInjection")
    public static class ConstructorInjectionPropertyLookupResource {

        private final String stringField;
        private final Integer integerField;
        private final int intField;
        private final MyEnum enumField;

        public ConstructorInjectionPropertyLookupResource(@Property("stringField") String stringField,
                                                          @Property("integerField") Integer integerField,
                                                          @Property("intField") int intField,
                                                          @Property("enumField") MyEnum enumField) {
            this.stringField = stringField;
            this.integerField = integerField;
            this.intField = intField;
            this.enumField = enumField;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response lookupProperty(@QueryParam("type") String type) {
            return getResponse(type, stringField, integerField, intField, enumField);
        }
    }

    private static Response getResponse(String type, String stringField, Integer integerField, int intField, MyEnum enumField) {
        final Object entity;
        switch (type) {
            case "stringField":
                entity = stringField;
                break;
            case "integerField":
                entity = integerField;
                break;
            case "intField":
                entity = intField;
                break;
            case "enumField":
                entity = enumField;
                break;
            default:
                throw new IllegalArgumentException(type + " is not a supported type");
        }
        return Response.ok()
                .entity(entity.toString())
                .header("javaType", entity.getClass().getTypeName())
                .build();
    }

    static class ExceptionCapture {
        private Throwable exception;

        void capture(Throwable exception) {
            this.exception = exception;
        }

        Throwable getException() {
            return exception;
        }
    }

    static class AssertingRequestEventListener implements ApplicationEventListener, RequestEventListener {
        private final CountDownLatch countDownLatch;
        private final ExceptionCapture exceptionCapture;

        AssertingRequestEventListener(CountDownLatch countDownLatch, ExceptionCapture exceptionCapture) {
            this.countDownLatch = countDownLatch;
            this.exceptionCapture = exceptionCapture;
        }

        @Override
        public void onEvent(ApplicationEvent applicationEvent) {
        }

        @Override
        public RequestEventListener onRequest(RequestEvent requestEvent) {
            return this;
        }

        @Override
        public void onEvent(RequestEvent requestEvent) {
            if (requestEvent.getType() == RequestEvent.Type.ON_EXCEPTION) {
                exceptionCapture.capture(requestEvent.getException());
                countDownLatch.countDown();
            }
        }
    }
}
