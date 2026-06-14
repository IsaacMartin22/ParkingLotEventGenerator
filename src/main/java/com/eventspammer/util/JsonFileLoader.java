package com.eventspammer.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonFileLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonFileLoader() {
    }

    public static <T> T load(Path path, Class<T> type) throws Exception {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Config file does not exist: " + path.toAbsolutePath());
        }

        return OBJECT_MAPPER.readValue(path.toFile(), type);
    }
}
