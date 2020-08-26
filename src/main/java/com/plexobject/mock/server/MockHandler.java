package com.plexobject.mock.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.ExportFormat;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.MockRequest;
import com.plexobject.mock.domain.RequestResponse;
import com.plexobject.mock.domain.MockResponse;
import com.plexobject.mock.util.HTTPUtils;

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
        doHandle(request, response);
    }

    //////////////////// PRIVATE METHODS //////////////////

    private void doHandle(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        MethodType methodType = MethodType.valueOf(req.getMethod());
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
        } catch (IOException e) {
            logger.error("IO Error, while invoking " + requestInfo + ": "
                    + debugInfo, e);
            res.sendError(500, "IO Error, while invoking " + requestInfo);
        }
    }

    private MockResponse handleMode(HttpServletRequest req,
            HttpServletResponse res, MethodType methodType,
            MockRequest requestInfo, StringBuilder debugInfo)
            throws IOException, UnsupportedEncodingException {
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
            response = MockResponse.from(req);
            if (save(methodType, requestInfo, response)) {
                debugInfo.append(methodType + " RECORDING " + requestInfo);
            } else {
                debugInfo.append(methodType + " REDIRECTING " + requestInfo);
            }
            break;
        }
        if (response == null) {
            throw new IOException(
                    "Failed to find response for " + requestInfo.getUrl());
        }
        return response;
    }

    private OutputStream addOutput(HttpServletResponse res,
            MockResponse response) throws IOException {
        OutputStream out = res.getOutputStream();
        if (response.isValidResponseCode()) {
            if (response.getContents() instanceof byte[]) {
                out.write(((byte[]) response.getContents()));
            } else if (response.getContents() instanceof String) {
                out.write(((String) response.getContents()).getBytes());
            } else if (response.getContents() != null) {
                out.write(response.getContents().toString().getBytes());
            }
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
            MockResponse responseInfo) throws IOException {
        if (config.isSaveAPIResponsesOnly()
                && !responseInfo.isAPIContentType()) {
            return false;
        }
        File path = config.toFile(requestInfo.getRequestId(), methodType,
                false);
        requestInfo.getExportFormat().write(path, responseInfo);
        logger.debug("Writing " + requestInfo.getUrl() + " to " + path);

        if (config.isSaveRawRequestResponses()) {
            ExportFormat.JSON.write(
                    config.getNextIOCounterFile(requestInfo.getRequestId(),
                            methodType),
                    new RequestResponse(requestInfo, responseInfo));
            logger.debug(
                    "Writing Raw file " + requestInfo.getUrl() + " to " + path);
        }
        return true;
    }

    private MockResponse read(MethodType methodType, MockRequest requestInfo)
            throws IOException {
        File path = config.toFile(requestInfo.getRequestId(), methodType, true);
        if (path == null) {
            throw new IOException("Failed to find path for " + methodType + " "
                    + requestInfo);
        }

        MockResponse resp = requestInfo.getExportFormat().read(path,
                MockResponse.class, requestInfo, config);
        logger.info("Reading " + requestInfo.getUrl() + " from " + path
                + " === " + resp);
        return resp;
    }

}
