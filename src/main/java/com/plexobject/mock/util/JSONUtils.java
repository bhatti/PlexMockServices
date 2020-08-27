package com.plexobject.mock.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
    private static final Logger logger = Logger.getLogger(JSONUtils.class);

    private static ObjectMapper mapper = new ObjectMapper();

    public static String marshal(Object value) throws IOException {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(String json, Class<?> klass)
            throws IOException {
        return (T) mapper.readValue(json, klass);
    }

    public static void write(File outputFile, Object value) throws IOException {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile,
                    value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T read(String json, Class<?> klass) throws IOException {
        try {
            return (T) mapper.readValue(json, klass);
        } catch (JsonMappingException e) {
            logger.warn("Failed to parse " + json);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T read(File inputFile, Class<?> klass)
            throws IOException {
        return (T) mapper.readValue(inputFile, klass);
    }
}
