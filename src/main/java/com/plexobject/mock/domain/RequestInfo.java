package com.plexobject.mock.domain;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.plexobject.mock.util.FileUtils;

public class RequestInfo {
    private static final String MOCK_USE_HASH = "mockUseHash";
    private static final String RECORD = "record";
    private static final String MOCK_MODE = "mockMode";
    private static final String REQUEST_ID = "requestId";
    private static final String MOCK_WAIT_TIME_MILLIS = "mockWaitTimeMillis";
    private static final String MOCK_RESPONSE_CODE = "mockResponseCode";
    //
    private static final String XMOCK_MODE = "XMockMode";
    private static final String XREQUEST_ID = "XRequestId";
    private static final String XMOCK_WAIT_TIME_MILLIS = "XMockWaitTimeMillis";
    private static final String XMOCK_RESPONSE_CODE = "XMockResponseCode";
    private static final Set<String> SKIP_HEADERS = new HashSet<>(Arrays.asList("Accept-Encoding"));
    private final boolean useHash;
    private final String requestId;
    private final String url;
    private final String contentType;
    private final Map<java.lang.String, java.lang.String> headers;
    private final Map<java.lang.String, java.lang.String[]> params;
    private final String content;
    private final int mockWaitTimeMillis;
    private final int mockResponseCode;
    private final boolean recordMode;

    public RequestInfo(final HttpServletRequest req, Configuration config) throws IOException {
        String path = getPath(req);
        url = config.getUrlPrefix() + path;
        contentType = req.getContentType();
        headers = toHeaders(req);
        params = req.getParameterMap();
        useHash = "true".equals(params.get(MOCK_USE_HASH));
        byte[] data = FileUtils.read(req.getInputStream());
        content = data.length > 0 ? new String(data) : null;
        requestId = getRequestId(req, path, req.getParameterMap(), null);
        mockWaitTimeMillis = getInteger(req, MOCK_WAIT_TIME_MILLIS, XMOCK_WAIT_TIME_MILLIS);
        mockResponseCode = getInteger(req, MOCK_RESPONSE_CODE, XMOCK_RESPONSE_CODE);
        recordMode = isRecordMode(req, config);
    }

    private static int getInteger(HttpServletRequest req, String paramKey, String headerKey) {
        String value = getValue(req, paramKey, headerKey, "0");
        return Integer.parseInt(value);
    }

    @JsonIgnore
    public String getRequestId() {
        return requestId;
    }

    @JsonIgnore
    public int getMockWaitTimeMillis() {
        return mockWaitTimeMillis;
    }

    @JsonIgnore
    public int getMockResponseCode() {
        return mockResponseCode;
    }

    @JsonIgnore
    public boolean isRecordMode() {
        return recordMode;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<java.lang.String, java.lang.String> getHeaders() {
        return headers;
    }

    public Map<java.lang.String, java.lang.String[]> getParams() {
        return params;
    }

    public String getContent() {
        return content;
    }

    @Override
    @JsonIgnore
    public String toString() {
        StringBuilder sb = new StringBuilder(url + ", ID=" + requestId + "\n");
        if (params != null) {
            sb.append("\tParams: " + params + "\n");
        }
        if (content != null) {
            sb.append("\tContent: " + content.length() + ">" + content + "\n");
        }
        return sb.toString();
    }

    private static String getPath(HttpServletRequest req) {
        return req.getQueryString() == null ? req.getRequestURI() : req.getRequestURI() + "?" + req.getQueryString();
    }

    private static String getValue(HttpServletRequest req, String paramKey, String headerKey, String defValue) {
        String value = req.getParameter(paramKey);
        if (value == null) {
            value = req.getHeader(headerKey);
        }
        if (value == null) {
            value = defValue;
        }
        return value;
    }

    private static boolean isRecordMode(HttpServletRequest req, Configuration config) {
        String reqMode = getValue(req, MOCK_MODE, XMOCK_MODE, null);
        return reqMode != null ? RECORD.equalsIgnoreCase(reqMode) : config.isRecordMode();
    }

    private String getRequestId(HttpServletRequest req, final String path,
            Map<java.lang.String, java.lang.String[]> params, String body) {
        String id = getValue(req, REQUEST_ID, XREQUEST_ID, "");
        if (useHash) {
            id = getIdByHash(params, body);
        }
        return path + id;
    }

    private static String getIdByHash(Map<java.lang.String, java.lang.String[]> params, String body) {
        String id;
        if (body != null) {
            id = FileUtils.hash(body);
        } else {
            id = toKey(params);
        }
        return id;
    }

    private static Map<String, String> toHeaders(final HttpServletRequest req) {
        Map<String, String> headers = new HashMap<String, String>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!SKIP_HEADERS.contains(name)) {
                String value = req.getHeader(name);
                headers.put(name, value);
            }
        }
        return headers;
    }

    private static final String toKey(final Map<java.lang.String, java.lang.String[]> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            for (String v : e.getValue()) {
                if (e.getKey() != null && v != null) {
                    sb.append(e.getKey().trim() + ":" + v.trim());
                }
            }
        }
        return FileUtils.hash(sb.toString());
    }
}
