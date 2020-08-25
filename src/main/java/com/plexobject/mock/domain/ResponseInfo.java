package com.plexobject.mock.domain;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.plexobject.mock.util.JSONUtils;

public class ResponseInfo implements SerializationLifecycle {
    private transient int responseCode;
    private Map<String, String> headers;
    private String contentType;
    private String contentClass;
    private Object contents;
    private Configuration config;

    public ResponseInfo() {
    }

    public ResponseInfo(int responseCode, String contentType,
            Map<String, String> headers, Object contents,
            Configuration config) {
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.headers = headers;
        this.contents = contents;
        this.config = config;
    }

    @JsonIgnore
    public boolean isAPIContentType() {
        return contentType != null
                && contentType.startsWith("application/json");
    }

    @JsonIgnore
    public boolean isValidResponseCode() {
        return responseCode >= 200 && responseCode <= 299;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public Object getContents() {
        return contents;
    }

    public void setContents(Object contents) {
        this.contents = contents;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getContentClass() {
        return contentClass;
    }

    public void setContentClass(String contentClass) {
        this.contentClass = contentClass;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public void beforeSerialize() throws IOException {
        if (isAPIContentType() && getContents() instanceof byte[]
                && config.isUnserializeJsonContentBeforeSave()) {
            String json = new String((byte[]) getContents());
            if (json.startsWith("{")) {
                setContentClass(Map.class.getName());
                setContents(JSONUtils.unmarshal(json, Map.class));
            } else if (json.startsWith("[")) {
                setContentClass(List.class.getName());
                setContents(JSONUtils.unmarshal(json, List.class));
            }
        }
    }

    @Override
    public void afterDeserialize() throws IOException {
        if (getContentClass() != null && getContents() != null) {
            String json = JSONUtils.marshal(getContents());
            setContents(json);
        }
    }
}
