package com.eventspammer.http;

public record ApiResponse(
        int statusCode,
        String body,
        long durationMillis
) {
}
