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
    private static final Set<String> SKIP_HEADERS = new HashSet<>(
            Arrays.asList("Accept-Encoding", "Upgrade-Insecure-Requests", "Upgrade: websocket", "Sec-WebSocket-Version",
                    "Sec-WebSocket-Key", "Sec-WebSocket-Extensions"));
    private final boolean useHash;
    private final String requestId;
    private final String url;
    private final String contentType;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final Object content;
    private final int mockWaitTimeMillis;
    private final int mockResponseCode;
    private final boolean recordMode;
    private final Configuration config;

    public RequestInfo(final HttpServletRequest req, Configuration config) throws IOException {
        this.config = config;
        String path = getPath(req);
        url = config.getUrlPrefix() + path;
        contentType = req.getContentType();
        headers = toHeaders(req);
        params = toParams(req);
        useHash = "true".equals(params.get(MOCK_USE_HASH));
        byte[] data = FileUtils.read(req.getInputStream());
        content = data.length > 0 && isAPIContentType() ? new String(data) : data;
        requestId = getRequestId(req, path, req.getParameterMap(), null);
        mockWaitTimeMillis = getInteger(req, MOCK_WAIT_TIME_MILLIS, XMOCK_WAIT_TIME_MILLIS);
        mockResponseCode = getInteger(req, MOCK_RESPONSE_CODE, XMOCK_RESPONSE_CODE);
        recordMode = isRecordMode(req, config);
    }

    private static Map<String, String> toParams(final HttpServletRequest req) {
        Map<String, String> params = new HashMap<>();
        Set<Map.Entry<String, String[]>> entries = (Set<Map.Entry<String, String[]>>) req.getParameterMap().entrySet();
        for (Map.Entry<String, String[]> e : entries) {
            if (e.getValue().length > 0) {
                params.put(e.getKey(), e.getValue()[0]);
            }
        }
        return params;
    }

    @JsonIgnore
    public boolean isAPIContentType() {
        return contentType == null || (contentType.startsWith("application/json")
                || contentType.startsWith("application/x-www-form-urlencoded")
                || contentType.startsWith("multipart/form-data"));
    }

    @JsonIgnore
    public Configuration getConfig() {
        return config;
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

    public Map<java.lang.String, java.lang.String> getParams() {
        return params;
    }

    public Object getContent() {
        return content;
    }

    @Override
    @JsonIgnore
    public String toString() {
        StringBuilder sb = new StringBuilder(url + ", Content-Type=" + contentType + ", ID=" + requestId + "\n");
        if (params != null) {
            sb.append("\tParams: " + params + "\n");
        }
        if (content != null) {
            int len = 0;
            if (content instanceof byte[]) {
                len = ((byte[]) content).length;
                sb.append("\tBinary Content Len: " + len + "\n");
            } else if (content instanceof CharSequence) {
                len = ((CharSequence) content).length();
                sb.append("\tContent: " + len + ">" + content + "\n");
            }
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

    private Map<String, String> toHeaders(final HttpServletRequest req) {
        Map<String, String> headers = new HashMap<String, String>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (SKIP_HEADERS.contains(name)) {
                continue;
            }
            String value = req.getHeader(name);
            headers.put(name, value);
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

    private static int getInteger(HttpServletRequest req, String paramKey, String headerKey) {
        String value = getValue(req, paramKey, headerKey, "0");
        return Integer.parseInt(value);
    }

}
