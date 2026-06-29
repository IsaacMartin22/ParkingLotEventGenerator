package com.eventspammer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestDefinitionTest {

    @Test
    void validateAllowsValidRequestDefinition() {
        RequestDefinition request = validRequestDefinition();

        assertDoesNotThrow(request::validate);
    }

    @Test
    void validateRejectsMissingName() {
        RequestDefinition request = validRequestDefinition();
        request.setName(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );

        assertEquals("Request name is required.", exception.getMessage());
    }

    @Test
    void validateRejectsMissingMethod() {
        RequestDefinition request = validRequestDefinition();
        request.setMethod(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );

        assertEquals("Request method is required for request: test-request", exception.getMessage());
    }

    @Test
    void validateRejectsMissingPath() {
        RequestDefinition request = validRequestDefinition();
        request.setPath(" ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );

        assertEquals("Request path is required for request: test-request", exception.getMessage());
    }

    @Test
    void validateRejectsPathWithoutLeadingSlash() {
        RequestDefinition request = validRequestDefinition();
        request.setPath("events");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );

        assertEquals("Request path must start with '/' for request: test-request", exception.getMessage());
    }

    @Test
    void validateAllowsZeroWeight() {
        RequestDefinition request = validRequestDefinition();
        request.setWeight(0);

        assertDoesNotThrow(request::validate);
    }

    @Test
    void validateRejectsNegativeWeight() {
        RequestDefinition request = validRequestDefinition();
        request.setWeight(-1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );

        assertEquals("Request weight must be >= 0 for request: test-request", exception.getMessage());
    }

    private RequestDefinition validRequestDefinition() {
        RequestDefinition request = new RequestDefinition();

        request.setName("test-request");
        request.setMethod(RequestMethod.POST);
        request.setPath("/events");
        request.setWeight(1);

        return request;
    }
}
