package com.eventspammer.core;

import com.eventspammer.config.RequestDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequestSelector {

    private final List<RequestDefinition> weightedRequests = new ArrayList<>();
    private final Random random = new Random();

    public RequestSelector(List<RequestDefinition> requests) {
        for (RequestDefinition request : requests) {
            for (int index = 0; index < request.getWeight(); index++) {
                weightedRequests.add(request);
            }
        }

        if (weightedRequests.isEmpty()) {
            throw new IllegalArgumentException("No weighted requests are available.");
        }
    }

    public RequestDefinition next() {
        int index = random.nextInt(weightedRequests.size());
        return weightedRequests.get(index);
    }
}
