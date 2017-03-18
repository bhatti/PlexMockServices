package com.plexobject.mock.server;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.plexobject.mock.util.FileUtils;

public class RequestInfo {
	private static final String REQUEST_ID = "requestId";

	final String requestId;
	final String url;
	final String contentType;
	final Map<java.lang.String, java.lang.String> headers;
	final Map<java.lang.String, java.lang.String[]> params;
	final String content;

	RequestInfo(final String urlPrefix, final HttpServletRequest req) throws IOException {
		url = urlPrefix + getPath(req);
		contentType = req.getContentType();
		headers = toHeaders(req);
		params = req.getParameterMap();
		byte[] data = FileUtils.read(req.getInputStream());
		content = data.length > 0 ? new String(data) : null;
		requestId = getRequestId(req, url, req.getParameterMap(), null);
	}

	@Override
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

	private static String getRequestId(HttpServletRequest req, final String url,
			Map<java.lang.String, java.lang.String[]> params, String body) {
		String id;
		if (req.getParameter(REQUEST_ID) != null) {
			id = req.getParameter(REQUEST_ID);
		} else {
			if (body != null) {
				id = FileUtils.hash(body);
			} else {
				id = toKey(params);
			}
		}
		return url + id;
	}

	private static Map<String, String> toHeaders(final HttpServletRequest req) {
		Map<String, String> headers = new HashMap<String, String>();
		Enumeration<String> names = req.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
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
}