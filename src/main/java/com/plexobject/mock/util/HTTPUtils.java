package com.plexobject.mock.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.MockRequest;
import com.plexobject.mock.domain.MockResponse;

public class HTTPUtils {
    private static final String CONTENT_TYPE = "Content-Type";
    private final HttpClient httpClient;

    public HTTPUtils(Configuration config) {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
        httpClient.getParams()
                .setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.getParams()
                .setSoTimeout(config.getConnectionTimeoutMillis());
        httpClient.getParams().setParameter("http.socket.timeout",
                config.getConnectionTimeoutMillis());
        httpClient.getParams().setParameter("http.connection.timeout",
                config.getConnectionTimeoutMillis());
    }

    public MockResponse invokeRemoteAPI(final MethodType methodType,
            final MockRequest requestInfo)
            throws IOException, UnsupportedEncodingException {
        HttpMethodBase method = null;
        switch (methodType) {
        case GET:
            method = new GetMethod(requestInfo.getURL());
            break;
        case POST:
            method = new PostMethod(requestInfo.getURL());
            setEntityContent(requestInfo, method);
            break;
        case PUT:
            method = new PutMethod(requestInfo.getURL());
            setEntityContent(requestInfo, method);
            break;
        case DELETE:
            method = new DeleteMethod(requestInfo.getURL());
            break;
        case HEAD:
            method = new HeadMethod(requestInfo.getURL());
            break;
        default:
            return null;
        }
        return execute(method, requestInfo);
    }

    private static void setEntityContent(final MockRequest requestInfo,
            HttpMethodBase method) throws UnsupportedEncodingException {
        ((EntityEnclosingMethod) method).setRequestEntity(
                new StringRequestEntity((String) requestInfo.getContent(),
                        requestInfo.getContentType(), "UTF-8"));
    }

    private MockResponse execute(final HttpMethodBase method,
            final MockRequest requestInfo) throws IOException {
        try {
            for (Map.Entry<String, String> h : requestInfo.getHeaders()
                    .entrySet()) {
                method.setRequestHeader(h.getKey(), h.getValue());
            }
            if (requestInfo.getParams() != null
                    && method instanceof PostMethod) {
                for (Map.Entry<String, String> e : requestInfo.getParams()
                        .entrySet()) {
                    if (e.getValue() != null) {
                        ((PostMethod) method).addParameter(e.getKey().trim(),
                                e.getValue().trim());
                    }
                }
            }
            final int sc = httpClient.executeMethod(method);
            InputStream in = method.getResponseBodyAsStream();
            byte[] data = null;
            if (in != null) {
                data = FileUtils.read(in);
            } else {
                data = new byte[0];
            }
            Header typeHeader = method.getResponseHeader(CONTENT_TYPE);
            String contentType = typeHeader != null ? typeHeader.getValue()
                    : null;

            return new MockResponse(sc, contentType, toResponseHeaders(method),
                    new String(data, "utf-8"));
        } finally {
            try {
                method.releaseConnection();
            } catch (Exception e) {
            }
        }
    }

    private Map<String, String> toResponseHeaders(final HttpMethodBase post)
            throws HttpException {
        Map<String, String> headers = new HashMap<>();
        for (Header h : post.getResponseHeaders()) {
            String value = h.getValue();
            if (value != null) {
                headers.put(h.getName(), value);
            }
        }
        return headers;
    }
}
