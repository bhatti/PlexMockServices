package com.plexobject.mock.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YAMLUtils {
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public String marshal(Object value) throws IOException {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public Object unmarshal(String yaml, Class<?> klass) throws IOException {
        return mapper.readValue(yaml, klass);
    }

    public void write(File outputFile, Object value) throws IOException {
        try {
            mapper.writeValue(outputFile, value);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }
    }

    public Object read(File inputFile, Class<?> klass) throws IOException {
        return mapper.readValue(inputFile, klass);
    }

    public Object read(String input, Class<?> klass) throws IOException {
        return mapper.readValue(input, klass);
    }
}
