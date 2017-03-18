package com.plexobject.mock.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import com.plexobject.mock.util.YAMLUtils;

public class Server extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String YAML = ".yml";
	private static final String RECORD = "record";
	private static final String MOCK_MODE = "mockMode";
	private static final String XMOCK_MODE = "XMockMode";
	private static final Logger logger = Logger.getLogger(Server.class);
	private final HttpClient httpClient;

	enum MethodType {
		GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH
	}

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
		logger.info("TARGET URL PREFIX " + urlPrefix + " record mode ? " + recordMode);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doService(req, res, MethodType.HEAD);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doService(req, res, MethodType.GET);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doService(req, res, MethodType.POST);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doService(req, res, MethodType.PUT);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doService(req, res, MethodType.DELETE);
	}

	//////////////////// PRIVATE METHODS //////////////////

	private boolean isRecordMode(HttpServletRequest req) {
		String reqMode = req.getParameter(MOCK_MODE);
		if (reqMode == null) {
			reqMode = req.getHeader(XMOCK_MODE);
		}
		return reqMode != null ? RECORD.equalsIgnoreCase(reqMode) : recordMode;
	}

	private void doService(HttpServletRequest req, HttpServletResponse res, MethodType methodType)
			throws ServletException, IOException {
		RequestInfo requestInfo = new RequestInfo(urlPrefix, req);
		RecordedResponse response = null;

		if (isRecordMode(req)) {
			logger.info(methodType + " RECORDING " + requestInfo);

			response = invokeRemoteAPI(methodType, requestInfo);
			save(response, requestInfo.requestId, methodType);
		} else {
			response = read(requestInfo.requestId, methodType);
			logger.info(methodType + " PLAYING " + requestInfo);
		}

		//
		if (response == null) {
			res.sendError(500, "Failed to find response for " + requestInfo.url);
			return;
		}
		res.setContentType(response.contentType);
		OutputStream out = res.getOutputStream();
		if (response.payload instanceof byte[]) {
			out.write(((byte[]) response.payload));
		} else if (response.payload instanceof String) {
			out.write(((String) response.payload).getBytes());
		} else if (response.payload != null) {
			out.write(response.payload.toString().getBytes());
		}
		logger.debug(methodType + " SENDING " + requestInfo.requestId + ", response " + response.payload);

		out.flush();
	}

	private RecordedResponse invokeRemoteAPI(final MethodType methodType, final RequestInfo requestInfo)
			throws IOException, UnsupportedEncodingException {

		HttpMethodBase method = null;
		switch (methodType) {
		case GET:
			method = new GetMethod(requestInfo.url);
			break;
		case POST:
			method = new PostMethod(requestInfo.url);
			if (requestInfo.content != null) {
				((EntityEnclosingMethod) method).setRequestEntity(
						new StringRequestEntity(requestInfo.content, requestInfo.contentType, "UTF-8"));
			}
			break;
		case PUT:
			method = new PutMethod(requestInfo.url);
			if (requestInfo.content != null) {
				((EntityEnclosingMethod) method).setRequestEntity(
						new StringRequestEntity(requestInfo.content, requestInfo.contentType, "UTF-8"));
			}
			break;
		case DELETE:
			method = new DeleteMethod(requestInfo.url);
			break;
		case HEAD:
			method = new HeadMethod(requestInfo.url);
			break;
		default:
			return null;
		}
		return execute(requestInfo.url, method, requestInfo.headers, requestInfo.params);
	}

	private RecordedResponse execute(final String url, final HttpMethodBase method, Map<String, String> headers,
			final Map<java.lang.String, java.lang.String[]> params) throws IOException {
		try {
			for (Map.Entry<String, String> h : headers.entrySet()) {
				method.setRequestHeader(h.getKey(), h.getValue());
			}
			if (params != null && method instanceof PostMethod) {
				for (Map.Entry<String, String[]> e : params.entrySet()) {
					for (String v : e.getValue()) {
						if (e.getKey() != null && v != null) {
							((PostMethod) method).addParameter(e.getKey().trim(), v.trim());
						}
					}
				}
			}
			final int sc = httpClient.executeMethod(method);
			if (!isValidResponse(sc)) {
				logger.error("Unexpected status code: " + sc + ": " + method.getStatusText() + " -- " + url);
			}
			String contents = method.getResponseBodyAsString();
			String type = method.getResponseHeader("Content-Type").getValue();

			return new RecordedResponse(sc, type, toResponseHeaders(method), contents);
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

	private String normalizeName(String url, String operation) {
		int q = url.indexOf("?");
		if (q != -1) {
			url = url.substring(0, q);
		}
		return url.replaceAll("[\\&\\/\\?:;,\\s]", "_") + "_" + operation.toLowerCase();
	}

	private File toFile(String url, MethodType methodType, boolean readOnly) {
		Random random = new Random();
		final String name = normalizeName(url, methodType.name());
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

	private void save(RecordedResponse response, String url, MethodType methodType) throws IOException {
		File path = toFile(url, methodType, false);
		YAMLUtils.write(path, response);
	}

	private RecordedResponse read(String url, MethodType methodType) throws IOException {
		File path = toFile(url, methodType, true);
		if (path == null) {
			return null;
		}
		return (RecordedResponse) YAMLUtils.read(path, RecordedResponse.class);
	}

	private boolean isValidResponse(int responseCode) {
		return responseCode >= 200 && responseCode <= 299;
	}
}
