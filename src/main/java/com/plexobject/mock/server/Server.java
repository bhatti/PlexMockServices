package com.plexobject.mock.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import com.plexobject.mock.util.YAMLUtils;

public class Server extends HttpServlet {
	private static final String YAML = ".yml";
	private static final String POST = "POST";
	private static final String MOCK_ID = "mockId";
	private static final long serialVersionUID = 1L;
	private static final String GET = "GET";
	private static final String RECORD = "record";
	private static final String MOCK_MODE = "mockMode";
	private static final String XMOCK_MODE = "XMockMode";
	private static final Logger LOGGER = Logger.getLogger(Server.class);
	private final HttpClient httpClient;

	private Map<String, Integer> counters = new HashMap<>();
	private int maxSamples;
	private int connectionTimeoutMillis;
	private boolean recordMode;
	private String urlPrefix;
	private File dataDir;

	public Server() {
		MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		httpClient = new HttpClient(connectionManager);
		httpClient.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
	}

	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		connectionTimeoutMillis = Integer.parseInt(servletConfig.getInitParameter("connectionTimeoutMillis"));
		maxSamples = Integer.parseInt(servletConfig.getInitParameter("maxSamples"));
		recordMode = "true".equals(servletConfig.getInitParameter("recordMode"));
		urlPrefix = servletConfig.getInitParameter("urlPrefix");
		dataDir = new File(servletConfig.getInitParameter("dataDir"));
		dataDir.mkdirs();

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
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		final String path = req.getRequestURI() + "?" + (req.getQueryString() == null ? "" : req.getQueryString());
		final String url = urlPrefix + path;
		Map<java.lang.String, java.lang.String[]> params = req.getParameterMap();
		String id = getRequestId(req, url, params, null);

		RecordedResponse response = null;

		boolean record = isRecordMode(req);

		if (record) {
			LOGGER.info("GET RECORDING " + url + "\n");
			response = invokeGetOnRemoteServer(url, toHeaders(req));
			save(response, id, GET);
		} else {
			response = read(id, GET);
			LOGGER.info("GET PLAYING " + url + " found " + (response != null) + "\n");

		}
		if (response == null) {
			res.sendError(500, "Failed to find response for " + path);
			return;
		}
		res.setContentType(response.contentType);
		OutputStream out = res.getOutputStream();
		out.write(response.payload.getBytes());
		out.flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		final String path = req.getRequestURI() + "?" + (req.getQueryString() == null ? "" : req.getQueryString());
		final String url = urlPrefix + path;
		RecordedResponse response = null;
		Map<java.lang.String, java.lang.String[]> params = req.getParameterMap();
		String body = new String(FileUtils.read(req.getInputStream()));

		String id = getRequestId(req, url, params, body);
		boolean record = isRecordMode(req);

		if (record) {
			LOGGER.info("POST RECORDING " + id + ", body " + body + "\n");

			if (isApplicationRequest(req)) {
				response = invokePostApplicationOnRemoteServer(url,
						new String(req.getHeader("Content-Type").getBytes()), body, toHeaders(req));
			} else {
				response = invokePostParamsOnRemoteServer(url, params, toHeaders(req));
			}
			save(response, id, POST);
		} else {
			response = read(id, POST);
			LOGGER.info("POST PLAYING " + id + ", body " + body + ", found " + (response != null) + "\n");
		}

		//
		if (response == null) {
			res.sendError(500, "Failed to find response for " + path);
			return;
		}
		res.setContentType(response.contentType);
		OutputStream out = res.getOutputStream();
		out.write(response.payload.getBytes());
		LOGGER.debug("POST SENDING " + id + ", response " + new String(response.payload));

		out.flush();
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	//////////////////// PRIVATE METHODS //////////////////

	private boolean isRecordMode(HttpServletRequest req) {
		String reqMode = req.getParameter(MOCK_MODE);
		if (reqMode == null) {
			reqMode = req.getHeader(XMOCK_MODE);
		}
		return reqMode != null ? RECORD.equalsIgnoreCase(reqMode) : recordMode;
	}

	private RecordedResponse invokeGetOnRemoteServer(final String url,
			final Map<java.lang.String, java.lang.String> headers) throws IOException {
		return execute(url, new GetMethod(url), headers);
	}

	private RecordedResponse invokePostApplicationOnRemoteServer(final String u, final String contentType,
			final String content, final Map<java.lang.String, java.lang.String> headers) throws IOException {

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

	private RecordedResponse invokePostParamsOnRemoteServer(final String u,
			final Map<java.lang.String, java.lang.String[]> params,
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
		String contents = post.getResponseBodyAsString();
		return new RecordedResponse(sc, type, toHeaders(post), contents);

	}

	private Map<String, String> toHeaders(final HttpMethodBase post) throws HttpException {
		Map<String, String> headers = new HashMap<>();
		for (Header h : post.getRequestHeaders()) {
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

	public Map<String, String> toHeaders(final HttpServletRequest req) {
		Map<String, String> headers = new HashMap<String, String>();
		Enumeration<String> names = req.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = req.getHeader(name);
			headers.put(name, value);
		}
		return headers;
	}

	private RecordedResponse execute(final String url, final HttpMethodBase method, Map<String, String> headers)
			throws IOException {
		try {
			for (Map.Entry<String, String> h : headers.entrySet()) {
				method.setRequestHeader(h.getKey(), h.getValue());
			}
			final int sc = httpClient.executeMethod(method);
			if (sc < 200 || sc > 299) {
				throw new IOException("Unexpected status code: " + sc + ": " + method.getStatusText() + " -- " + url);
			}
			String contents = method.getResponseBodyAsString();
			String type = method.getResponseHeader("Content-Type").getValue();

			return new RecordedResponse(sc, type, toHeaders(method), contents);
		} finally {
			try {
				method.releaseConnection();
			} catch (Exception e) {
			}
		}
	}

	private String normalizeName(String url, String operation) {
		int q = url.indexOf("?");
		if (q != -1) {
			url = url.substring(0, q);
		}
		return url.replaceAll("[\\&\\/\\?:;,\\s]", "_") + "_" + operation.toLowerCase();
	}

	private File toFile(String url, String operation, boolean readOnly) {
		Random random = new Random();
		final String name = normalizeName(url, operation);
		if (readOnly) {
			File[] files = dataDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith(name);
				}
			});
			return files != null && files.length > 0 ? files[random.nextInt(files.length)] : null;
		} else {
			Integer counter = counters.get(name);
			if (counter == null) {
				counter = 1;
			} else {
				counter = counter + 1;
			}
			if (counter > maxSamples) {
				counter = 1;
			}
			counters.put(name, counter);
			return new File(dataDir, name + "_" + counter + YAML);
		}
	}

	private void save(RecordedResponse response, String url, String operation) throws IOException {
		if (isValidResponse(response)) {
			File path = toFile(url, operation, false);
			YAMLUtils.write(path, response);
		}
	}

	private RecordedResponse read(String url, String operation) throws IOException {
		File path = toFile(url, operation, true);
		if (path == null) {
			return null;
		}
		return (RecordedResponse) YAMLUtils.read(path, RecordedResponse.class);
	}

	private boolean isValidResponse(RecordedResponse response) {
		return response.responseCode >= 200 && response.responseCode <= 299;
	}

	private boolean isApplicationRequest(HttpServletRequest req) {
		String contentType = req.getHeader("Content-Type");
		return contentType != null && contentType.startsWith("application/");
	}

	private String getRequestId(HttpServletRequest req, final String url,
			Map<java.lang.String, java.lang.String[]> params, String body) {
		String id;
		if (req.getParameter(MOCK_ID) != null) {
			id = req.getParameter(MOCK_ID);
		} else {
			if (body != null) {
				id = url + FileUtils.hash(body);
			} else {
				id = url + toKey(params);
			}
		}
		return id;
	}
}
