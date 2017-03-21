package com.plexobject.mock.util;

import java.io.File;
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

    public static void write(File outputFile, Object value) throws IOException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public static Object read(File inputFile, Class<?> klass) throws IOException {
        Object value = mapper.readValue(inputFile, klass);
        return value;
    }
}
