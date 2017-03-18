package com.plexobject.mock.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
    private static ObjectMapper mapper = new ObjectMapper();

    public static String marshal(Object value) throws IOException {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public static Object unmarshal(String json, Class<?> klass) throws IOException {
        return mapper.readValue(json, klass);
    }
}
