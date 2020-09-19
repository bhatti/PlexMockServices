package com.plexobject.mock.domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.thymeleaf.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import com.plexobject.mock.server.MockService;
import com.plexobject.mock.util.FileUtils;

/**
 * This class defines incoming requests
 * 
 * @author shahzad bhatti
 *
 */

public class MockRequest {
    private static final Set<String> SKIP_HEADERS = ImmutableSet.of(
            "Accept-Encoding", "Upgrade-Insecure-Requests",
            "Upgrade: websocket", "Sec-WebSocket-Version", "Sec-WebSocket-Key",
            "Sec-WebSocket-Extensions");

    private static final Set<String> SKIP_FIELDS = ImmutableSet.of(
            Constants.MOCK_REQUEST_ID, Constants.MOCK_METHOD,
            Constants.MOCK_WAIT_TIME_MILLIS, Constants.MOCK_RESPONSE_CODE,
            Constants.MOCK_EXPORT_FORMAT);

    private final String url;
    private final MethodType method;
    private final String requestId;
    private final String hash;
    private final String contentType;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String content;
    private final int mockWaitTimeMillis;
    private final int mockResponseCode;
    private final MockMode mockMode;
    private final Configuration config;
    private final ExportFormat exportFormat;

    public MockRequest(Configuration config, String url, MethodType method,
            Map<String, String> headers, Map<String, String> params,
            String content) {
        this.config = config;
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.params = params;
        this.contentType = Constants.JSON_CONTENT;
        this.content = content;
        requestId = "";
        mockMode = MockMode.PLAY;
        hash = "";
        mockWaitTimeMillis = 0;
        mockResponseCode = 200;
        exportFormat = ExportFormat.JSON;
    }

    public MockRequest(final HttpServletRequest req, Configuration config)
            throws IOException, ServletException {
        this.config = config;
        url = getMockTargetURL(req, config.getTargetURL()) + getPath(req);
        method = MockService.getMethod(req);
        contentType = req.getContentType();
        headers = toHeaders(req);
        params = toParams(req);
        exportFormat = getExportFormat(req, config.getDefaultExportFormat());
        mockMode = getMockMode(req, config);
        InputStream in = req.getInputStream();
        byte[] data = FileUtils.read(in);
        content = new String(data, "utf-8");

        requestId = getRequestId(req);
        hash = getHash(req.getParameterMap(), content, mockMode);
        mockWaitTimeMillis = getInteger(req, Constants.MOCK_WAIT_TIME_MILLIS,
                Constants.XMOCK_WAIT_TIME_MILLIS);
        mockResponseCode = getInteger(req, Constants.MOCK_RESPONSE_CODE,
                Constants.XMOCK_RESPONSE_CODE);
    }

    @JsonIgnore
    public boolean isAPIContentType() {
        return contentType == null
                || (contentType.startsWith(Constants.JSON_CONTENT)
                        || contentType.startsWith(Constants.FORM_CONTENT)
                        || contentType.startsWith(Constants.MULTIPART_CONTENT));
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
    public MockMode getMockMode() {
        return mockMode;
    }

    @JsonIgnore
    public String getURL() {
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

    public String getContent() {
        return content;
    }

    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    public String getHash() {
        return hash;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return "MockRequest [url=" + url + ", method=" + method + ", requestId="
                + requestId + ", hash=" + hash + ", contentType=" + contentType
                + ", headers=" + headers + ", params=" + params
                + ", mockResponseCode=" + mockResponseCode + ", mockMode="
                + mockMode + ", exportFormat=" + exportFormat + "]";
    }

    //////////////////// PRIVATE METHODS //////////////////

    private static String getValue(HttpServletRequest req, String paramKey,
            String headerKey, String defValue) {
        String value = req.getParameter(paramKey);
        if (value == null) {
            value = req.getHeader(headerKey);
        }
        if (value == null) {
            value = defValue;
        }
        return value;
    }

    private static String getPath(HttpServletRequest req) {
        return req.getQueryString() == null ? req.getRequestURI()
                : req.getRequestURI() + "?" + req.getQueryString();
    }

    private static MockMode getMockMode(HttpServletRequest req,
            Configuration config) {
        String reqMode = getValue(req, Constants.MOCK_MODE,
                Constants.XMOCK_MODE, config.getMockMode().name())
                        .toUpperCase();
        return MockMode.valueOf(reqMode);
    }

    private String getRequestId(HttpServletRequest req) {
        return getValue(req, Constants.MOCK_REQUEST_ID,
                Constants.XMOCK_REQUEST_ID, "");
    }

    private String getMockTargetURL(HttpServletRequest req, String def) {
        return getValue(req, Constants.MOCK_TARGET_URL,
                Constants.XMOCK_TARGET_URL, def);
    }

    private static String getHash(Map<String, String[]> params, String body,
            MockMode mockMode) {
        if (!StringUtils.isEmpty(body) && mockMode != MockMode.STORE) {
            return FileUtils.hash(body);
        } else {
            return toKey(params);
        }
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

    private static final String toKey(final Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            for (String v : e.getValue()) {
                if (!SKIP_FIELDS.contains(e.getKey()) && v != null) {
                    sb.append(e.getKey().trim() + ":" + v.trim());
                }
            }
        }

        return FileUtils.hash(sb.toString());
    }

    private static int getInteger(HttpServletRequest req, String paramKey,
            String headerKey) {
        String value = getValue(req, paramKey, headerKey, "0");
        return Integer.parseInt(value);
    }

    private static ExportFormat getExportFormat(HttpServletRequest req,
            ExportFormat defaultExportFormat) {
        String value = getValue(req, Constants.MOCK_EXPORT_FORMAT,
                Constants.XMOCK_EXPORT_FORMAT, defaultExportFormat.name())
                        .toUpperCase();
        return ExportFormat.valueOf(value);
    }

    private static Map<String, String> toParams(final HttpServletRequest req) {
        Map<String, String> params = new HashMap<>();
        Set<Map.Entry<String, String[]>> entries = (Set<Map.Entry<String, String[]>>) req
                .getParameterMap().entrySet();
        for (Map.Entry<String, String[]> e : entries) {
            if (!SKIP_FIELDS.contains(e.getKey()) && e.getValue().length > 0) {
                params.put(e.getKey(), e.getValue()[0]);
            }
        }
        return params;
    }

    public MethodType getMethod() {
        return method;
    }

}
