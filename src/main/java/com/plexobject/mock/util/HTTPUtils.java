package com.plexobject.mock.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.RequestInfo;
import com.plexobject.mock.domain.ResponseInfo;

public class HTTPUtils {
    private static final String CONTENT_TYPE = "Content-Type";
    private final HttpClient httpClient;

    public HTTPUtils(Configuration config) {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
        httpClient.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        httpClient.getParams().setSoTimeout(config.getConnectionTimeoutMillis());
        httpClient.getParams().setParameter("http.socket.timeout", config.getConnectionTimeoutMillis());
        httpClient.getParams().setParameter("http.connection.timeout", config.getConnectionTimeoutMillis());
    }

    public ResponseInfo invokeRemoteAPI(final MethodType methodType, final RequestInfo requestInfo)
            throws IOException, UnsupportedEncodingException {
        HttpMethodBase method = null;
        switch (methodType) {
        case GET:
            method = new GetMethod(requestInfo.getUrl());
            break;
        case POST:
            method = new PostMethod(requestInfo.getUrl());
            setEntityContent(requestInfo, method);
            break;
        case PUT:
            method = new PutMethod(requestInfo.getUrl());
            setEntityContent(requestInfo, method);
            break;
        case DELETE:
            method = new DeleteMethod(requestInfo.getUrl());
            break;
        case HEAD:
            method = new HeadMethod(requestInfo.getUrl());
            break;
        default:
            return null;
        }
        return execute(method, requestInfo);
    }

    private static void setEntityContent(final RequestInfo requestInfo, HttpMethodBase method)
            throws UnsupportedEncodingException {
        if (requestInfo.getContent() instanceof byte[]) {
            ((EntityEnclosingMethod) method).setRequestEntity(
                    new ByteArrayRequestEntity((byte[]) requestInfo.getContent(), requestInfo.getContentType()));
        } else if (requestInfo.getContent() instanceof String) {
            ((EntityEnclosingMethod) method).setRequestEntity(
                    new StringRequestEntity((String) requestInfo.getContent(), requestInfo.getContentType(), "UTF-8"));
        }
    }

    private ResponseInfo execute(final HttpMethodBase method, final RequestInfo requestInfo) throws IOException {
        try {
            for (Map.Entry<String, String> h : requestInfo.getHeaders().entrySet()) {
                method.setRequestHeader(h.getKey(), h.getValue());
            }
            if (requestInfo.getParams() != null && method instanceof PostMethod) {
                for (Map.Entry<String, String[]> e : requestInfo.getParams().entrySet()) {
                    for (String v : e.getValue()) {
                        if (e.getKey() != null && v != null) {
                            ((PostMethod) method).addParameter(e.getKey().trim(), v.trim());
                        }
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
            String type = method.getResponseHeader(CONTENT_TYPE).getValue();

            return new ResponseInfo(sc, type, toResponseHeaders(method), data, requestInfo.getConfig());
        } finally {
            try {
                method.releaseConnection();
            } catch (Exception e) {
            }
        }
    }

    private Map<String, String> toResponseHeaders(final HttpMethodBase post) throws HttpException {
        Map<String, String> headers = new HashMap<>();
        for (Header h : post.getResponseHeaders()) {
            @SuppressWarnings("deprecation")
            HeaderElement[] e = h.getValues();
            String value = null;
            for (int i = 0; i < e.length; i++) {
                value = e[i].getValue();
            }
            if (value != null) {
                headers.put(h.getName(), value);
            }
        }
        return headers;
    }
}
