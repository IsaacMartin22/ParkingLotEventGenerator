package com.eventspammer.config;

import com.example.parkinglot.sdk.model.GenerationRequests;

import java.util.List;
import java.util.Map;

public record AppConfigSnapshot(
        String baseUrl,
        Map<String, String> defaultHeaders,
        List<String> carColors,
        Map<String, List<String>> carMakesAndModels,
        List<String> carMakes,
        int upperSpaceIdRand,
        int upperCarIdRand,
        int upperSpaceNumberRand,
        Map<GenerationRequests, Integer> requestWeightMap,
        Map<GenerationRequests, Boolean> requestEnabledMap,
        long delayMillis,
        int requestTimeoutSeconds,
        int totalWeight
) {
}

