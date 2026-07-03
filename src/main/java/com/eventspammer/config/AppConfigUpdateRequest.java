package com.eventspammer.config;

import java.util.List;
import java.util.Map;

public record AppConfigUpdateRequest(
        List<String> carColors,
        Map<String, List<String>> carMakesAndModels,
        Integer upperSpaceIdRand,
        Integer upperCarIdRand,
        Integer upperSpaceNumberRand,
        Map<String, Integer> requestWeightMap,
        Map<String, Boolean> requestEnabledMap,
        Long delayMillis,
        Integer requestTimeoutSeconds
) {
}

