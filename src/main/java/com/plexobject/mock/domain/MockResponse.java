package com.plexobject.mock.domain;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.plexobject.mock.util.JSONUtils;

/**
 * This class defines outgoing response
 * 
 * @author shahzad bhatti
 *
 */

public class MockResponse implements SerializationLifecycle {
    private transient int responseCode;
    private Map<String, String> headers;
    private String contentType;
    private String contents;

    public MockResponse() {
    }

    public MockResponse(int responseCode, String contentType,
            Map<String, String> headers, Object contents) throws IOException {
        Preconditions.checkNotNull(contentType, "contentType is not defined");
        Preconditions.checkNotNull(contents, "contents is not defined");
        Preconditions.checkNotNull(headers, "headers is not defined");
        this.responseCode = responseCode;
        this.contentType = contentType;
        this.headers = headers;
        setContents(contents);
    }

    @JsonIgnore
    public boolean isAPIContentType() {
        return contentType != null
                && contentType.startsWith(Constants.JSON_CONTENT);
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

    public String getContents() {
        return contents;
    }

    public void setContents(Object contents) throws IOException {
        if (contents instanceof String == false) {
            contents = JSONUtils.marshal(contents);
        }
        this.contents = (String) contents;
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

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public void beforeSerialize(Configuration config) throws IOException {
        if (isAPIContentType() && getContents() instanceof String
                && config.isUnserializeJsonContentBeforeSave()) {
            if (contents.startsWith("{")) {
                setContents(JSONUtils.unmarshal(contents, Map.class));
            } else if (contents.startsWith("[")) {
                setContents(JSONUtils.unmarshal(contents, List.class));
            }
        }
    }

    @Override
    public void afterDeserialize(Configuration config) throws IOException {
        if (getContents() != null
                && config.isUnserializeJsonContentBeforeSave()) {
            String json = JSONUtils.marshal(getContents());
            setContents(json);
        }
    }
}
