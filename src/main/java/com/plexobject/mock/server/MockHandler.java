package com.plexobject.mock.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.thymeleaf.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.Constants;
import com.plexobject.mock.domain.ExportFormat;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.MockRequest;
import com.plexobject.mock.domain.MockResponse;
import com.plexobject.mock.domain.RequestResponse;
import com.plexobject.mock.util.HTTPUtils;

/**
 * This class handles mock requests
 * 
 * @author shahzad bhatti
 *
 */
public class MockHandler extends AbstractHandler {
    private static final Logger logger = Logger.getLogger(MockHandler.class);

    private Configuration config;
    private HTTPUtils httpUtils;

    public MockHandler() throws IOException {
        config = new Configuration();
        httpUtils = new HTTPUtils(config);
        logger.info("INITIALIZED " + config);
    }

    @Override
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        MethodType methodType = MockService.getMethod(request);

        doHandle(request, response, methodType);
    }

    public void doHandle(HttpServletRequest req, HttpServletResponse res,
            MethodType methodType) throws ServletException, IOException {
        MockRequest requestInfo = new MockRequest(req, config);

        StringBuilder debugInfo = new StringBuilder();

        try {
            MockResponse response = handleMode(req, res, methodType,
                    requestInfo, debugInfo);
            checkDelay(requestInfo, response, debugInfo);

            addStatus(res, requestInfo, response);

            res.setContentType(response.getContentType());
            addHeaders(res, response);
            OutputStream out = addOutput(res, response);
            debugInfo.append("\tStatus: " + response.getResponseCode()
                    + ", Response Content-Type: " + response.getContentType());
            logger.info(debugInfo);
            out.flush();
        } catch (Exception e) {
            logger.error("IO Error, while invoking " + requestInfo + ": "
                    + debugInfo, e);
            res.sendError(500, "IO Error, while invoking " + requestInfo);
        }
    }

    //////////////////// PRIVATE METHODS //////////////////

    private MockResponse handleMode(HttpServletRequest req,
            HttpServletResponse res, MethodType methodType,
            MockRequest requestInfo, StringBuilder debugInfo)
            throws IOException, UnsupportedEncodingException,
            URISyntaxException {
        MockResponse response = null;
        switch (requestInfo.getMockMode()) {
        case PASS:
            response = httpUtils.invokeRemoteAPI(methodType, requestInfo);
            debugInfo.append(methodType + " PASSING " + requestInfo);
            break;
        case RECORD:
            response = httpUtils.invokeRemoteAPI(methodType, requestInfo);
            if (save(methodType, requestInfo, response)) {
                debugInfo.append(methodType + " RECORDING " + requestInfo);
            } else {
                debugInfo.append(methodType + " REDIRECTING " + requestInfo);
            }
            break;
        case PLAY:
            response = read(methodType, requestInfo);
            debugInfo.append(methodType + " PLAYING " + requestInfo);
            break;
        case STORE:
            File path = config.toWriteFile(requestInfo);
            requestInfo.getExportFormat().write(path, requestInfo.getContent(),
                    config);
            logger.debug("Writing " + requestInfo.getURL() + " to " + path);
            return new MockResponse(200, Constants.JSON_CONTENT,
                    ImmutableMap.of("content-type", Constants.JSON_CONTENT),
                    requestInfo.getContent());
        }

        if (response == null) {
            throw new IOException(
                    "Failed to find response for " + requestInfo.getURL());
        }
        return response;
    }

    private OutputStream addOutput(HttpServletResponse res,
            MockResponse response) throws IOException {
        OutputStream out = res.getOutputStream();
        if (response.isValidResponseCode()) {
            out.write(((String) response.getContents()).getBytes());
        } else {
            out.write("API failed".getBytes());
        }
        return out;
    }

    private void addHeaders(HttpServletResponse res, MockResponse response) {
        for (Map.Entry<String, String> e : response.getHeaders().entrySet()) {
            res.addHeader(e.getKey(), e.getValue());
        }
    }

    private void addStatus(HttpServletResponse res, MockRequest requestInfo,
            MockResponse response) {
        if (requestInfo.getMockResponseCode() > 0) {
            res.setStatus(requestInfo.getMockResponseCode());
        } else {
            res.setStatus(response.getResponseCode());
        }
    }

    private void checkDelay(MockRequest requestInfo, MockResponse response,
            StringBuilder debugInfo) {
        if (requestInfo.getMockWaitTimeMillis() > 0) {
            debugInfo.append("\tAdding delay from request "
                    + requestInfo.getMockWaitTimeMillis() + "ms\n");
            delay(requestInfo.getMockWaitTimeMillis());
        } else if (config.getInjectFailuresAndWaitTimesPerc() > 0) {
            int delay = config.getRandomFailuresAndWaitTimesPerc();
            if (delay > 0) {
                if (delay % 2 == 0) {
                    debugInfo.append("\tAdding random delay " + delay + "ms\n");
                    delay(delay); // inject delay
                } else {
                    debugInfo.append("\tAdding random failure\n");
                    response.setResponseCode(500);
                }
            }
        }
    }

    private void delay(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private boolean save(MethodType methodType, MockRequest requestInfo,
            MockResponse responseInfo) throws IOException, URISyntaxException {
        if (config.isSaveAPIResponsesOnly()
                && !responseInfo.isAPIContentType()) {
            return false;
        }
        File path = config.toWriteFile(requestInfo);
        requestInfo.getExportFormat().write(path, responseInfo, config);
        logger.debug("Writing " + requestInfo.getURL() + " to " + path);

        if (config.isSaveRawRequestResponses()) {
            ExportFormat.JSON.write(config.getNextIOCounterFile(requestInfo),
                    new RequestResponse(requestInfo, responseInfo), config);
            logger.debug(
                    "Writing Raw file " + requestInfo.getURL() + " to " + path);
        }
        return true;
    }

    private MockResponse read(MethodType methodType, MockRequest requestInfo)
            throws IOException, URISyntaxException {
        File path = config.toReadFile(requestInfo);
        if (path == null) {
            throw new IOException("Failed to find path for " + methodType + " "
                    + requestInfo);
        }

        MockResponse resp = requestInfo.getExportFormat().read(path,
                MockResponse.class, requestInfo, config);
        resp.getHeaders().put(Constants.XMOCK_FILE_PATH, path.getAbsolutePath());
        resp.getHeaders().put(Constants.XMOCK_HASH, requestInfo.getHash());
        if (!StringUtils.isEmpty(requestInfo.getRequestId())) {
            resp.getHeaders().put(Constants.XMOCK_REQUEST_ID, requestInfo.getRequestId());
        }
        return resp;
    }

}
