package com.plexobject.mock.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import com.plexobject.mock.util.FileUtils;

public class Server extends HttpServlet {
	private static final Logger LOGGER = Logger.getLogger(Server.class);
	private static final long serialVersionUID = 1L;
	private final HttpClient httpClient;

	static class Response implements Serializable {
		private static final long serialVersionUID = 1L;
		transient int responseCode;
		byte[] payload;
		Map<String, String[]> headers;
		String contentType;

		public Response(int responseCode, byte[] payload, String contentType, Map<String, String[]> headers) {
			this.responseCode = responseCode;
			this.payload = payload;
			this.contentType = contentType;
			this.headers = headers;
		}

		public int getResponseCode() {
			return responseCode;
		}

		public void setResponseCode(int responseCode) {
			this.responseCode = responseCode;
		}

		public byte[] getPayload() {
			return payload;
		}

		public void setPayload(byte[] payload) {
			this.payload = payload;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		public Map<String, String[]> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, String[]> headers) {
			this.headers = headers;
		}

	}

	private int connectionTimeoutMillis;
	private boolean recordMode;
	private String urlPrefix;
	public Server() {
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		httpClient = new HttpClient(connectionManager);
		httpClient.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
	}
	
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		connectionTimeoutMillis = Integer.parseInt(servletConfig.getInitParameter("connectionTimeoutMillis"));
		recordMode = "true".equals(servletConfig.getInitParameter("recordMode"));
		urlPrefix = servletConfig.getInitParameter("urlPrefix");

		httpClient.getParams().setSoTimeout(connectionTimeoutMillis);
		httpClient.getParams().setParameter("http.socket.timeout", connectionTimeoutMillis);
		httpClient.getParams().setParameter("http.connection.timeout", connectionTimeoutMillis);

		if (recordMode && (urlPrefix == null || urlPrefix.length() == 0)) {
			throw new RuntimeException("Running in record mode but mock.target.url property is not set");
		}
		if (!urlPrefix.startsWith("http://") && !urlPrefix.startsWith("https://")) {
			throw new RuntimeException("Invalid mock.target.url property '" + urlPrefix + "'");
		}
		if (urlPrefix.endsWith("/")) {
			urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
		}
		LOGGER.info("TARGET URL PREFIX " + urlPrefix + " record mode ? " + recordMode);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		final String path = req.getRequestURI() + "?" + (req.getQueryString() == null ? "" : req.getQueryString());
		final String url = urlPrefix + path;
		Response response = null;

		boolean record = req.getParameter("mock_mode") != null
				? "record".equalsIgnoreCase(req.getParameter("mock_mode")) : recordMode;

		if (record) {
			LOGGER.info("GET RECORDING " + url + "\n");
			response = get(url, toHeaders(req));
			if (response.responseCode == 200) {
				FileUtils.writeObject(response, url);
			}
		} else {
			response = (Response) FileUtils.readObject(url);
			LOGGER.info("GET PLAYING " + url + " found " + (response != null) + "\n");

		}
		if (response == null) {
			res.sendError(500, "Failed to find response for " + path);
			return;
		}
		res.setContentType(response.contentType);
		OutputStream out = res.getOutputStream();
		out.write(response.payload);
		out.flush();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		final String path = req.getRequestURI() + "?" + (req.getQueryString() == null ? "" : req.getQueryString());
		final String url = urlPrefix + path;
		Response response = null;
		@SuppressWarnings("unchecked")
		Map<java.lang.String, java.lang.String[]> params = req.getParameterMap();
		String body = new String(FileUtils.read(req.getInputStream()));

		String key = null;
		if (req.getParameter("mock_key") != null) {
			key = req.getParameter("mock_key");
		} else {
			if (params != null && params.size() > 0) {
				key = url + toKey(params);
			} else {
				key = url + FileUtils.hash(body);
			}
		}
		boolean record = req.getParameter("mock_mode") != null
				? "record".equalsIgnoreCase(req.getParameter("mock_mode")) : recordMode;

		if (record) {
			LOGGER.info("POST RECORDING " + key + ", body " + body + "\n");

			if (params != null && params.size() > 0) {
				response = post(url, params, toHeaders(req));
			} else {
				response = post(url, new String(req.getHeader("Content-Type").getBytes()), body, toHeaders(req));
			}
			if (response.responseCode == 200) {
				FileUtils.writeObject(response, key);
			}
		} else {
			response = (Response) FileUtils.readObject(key);
			LOGGER.info("POST PLAYING " + key + ", body " + body + ", found " + (response != null) + "\n");
		}

		//
		if (response == null) {
			res.sendError(500, "Failed to find response for " + path);
			return;
		}
		res.setContentType(response.contentType);
		OutputStream out = res.getOutputStream();
		out.write(response.payload);
		LOGGER.debug("POST SENDING " + key + ", response " + new String(response.payload));

		out.flush();
	}

	private Response get(final String u, final Map<java.lang.String, java.lang.String> headers) throws IOException {
		Response response = execute(u, new GetMethod(u), headers);
		FileUtils.writeObject(response, u);
		return response;
	}

	private Response post(final String u, final String contentType, final String content,
			final Map<java.lang.String, java.lang.String> headers) throws IOException {

		final PostMethod post = new PostMethod(u);
		try {
			post.setRequestEntity(new StringRequestEntity(content, contentType, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IOException("Failed to encode " + u, e);
		}
		return execute(u, post, headers);
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

	private Response post(final String u, final Map<java.lang.String, java.lang.String[]> params,
			final Map<java.lang.String, java.lang.String> headers) throws IOException {

		final PostMethod post = new PostMethod(u);
		for (Map.Entry<String, String> h : headers.entrySet()) {
			post.setRequestHeader(h.getKey(), h.getValue());
		}
		for (Map.Entry<String, String[]> e : params.entrySet()) {
			for (String v : e.getValue()) {
				if (e.getKey() != null && v != null) {
					post.addParameter(e.getKey().trim(), v.trim());
				}
			}
		}
		final int sc = httpClient.executeMethod(post);
		String type = post.getResponseHeader("Content-Type").getValue();
		return new Response(sc, FileUtils.read(post.getResponseBodyAsStream()), type, toHeaders(post));

	}

	private Map<String, String[]> toHeaders(final HttpMethodBase post) throws HttpException {
		Map<String, String[]> headers = new HashMap<String, String[]>();
		for (Header h : post.getRequestHeaders()) {
			@SuppressWarnings("deprecation")
			HeaderElement[] e = h.getValues();
			String[] values = new String[e.length];
			for (int i = 0; i < e.length; i++) {
				values[i] = e[i].getValue();
			}
			headers.put(h.getName(), values);
		}
		return headers;
	}

	public Map<String, String> toHeaders(final HttpServletRequest req) {
		Map<String, String> headers = new HashMap<String, String>();
		@SuppressWarnings("unchecked")
		Enumeration<String> names = req.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = req.getHeader(name);
			headers.put(name, value);
		}
		return headers;
	}

	private Response execute(final String url, final HttpMethodBase method, Map<String, String> headers)
			throws IOException {
		try {
			for (Map.Entry<String, String> h : headers.entrySet()) {
				method.setRequestHeader(h.getKey(), h.getValue());
			}
			final int sc = httpClient.executeMethod(method);
			if (sc < 200 || sc > 299) {
				throw new IOException("Unexpected status code: " + sc + ": " + method.getStatusText() + " -- " + url);
			}
			final InputStream in = method.getResponseBodyAsStream();
			String type = method.getResponseHeader("Content-Type").getValue();

			return new Response(sc, FileUtils.read(in), type, toHeaders(method));
		} finally {
			try {
				method.releaseConnection();
			} catch (Exception e) {
			}
		}
	}
}
