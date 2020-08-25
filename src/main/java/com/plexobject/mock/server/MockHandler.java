package com.plexobject.mock.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import com.plexobject.mock.domain.RequestInfo;
import com.plexobject.mock.domain.RequestResponse;
import com.plexobject.mock.domain.ResponseInfo;
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
        RequestInfo requestInfo = new RequestInfo(req, config);
        ResponseInfo response = null;

        StringBuilder debugInfo = new StringBuilder();

        if (requestInfo.isRecordMode()) {
            try {
                response = httpUtils.invokeRemoteAPI(methodType, requestInfo);
                if (save(methodType, requestInfo, response)) {
                    debugInfo.append(methodType + " RECORDING " + requestInfo);
                } else {
                    debugInfo
                            .append(methodType + " REDIRECTING " + requestInfo);
                }
            } catch (IOException e) {
                res.sendError(500, "IO Error, while invoking " + requestInfo);
                return;
            }
        } else {
            response = read(methodType, requestInfo);
            debugInfo.append(methodType + " PLAYING " + requestInfo);
        }

        //
        if (response == null) {
            res.sendError(500,
                    "Failed to find response for " + requestInfo.getUrl());
            return;
        }

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

        if (requestInfo.getMockResponseCode() > 0) {
            res.setStatus(requestInfo.getMockResponseCode());
        } else {
            res.setStatus(response.getResponseCode());
        }

        res.setContentType(response.getContentType());
        for (Map.Entry<String, String> e : response.getHeaders().entrySet()) {
            res.addHeader(e.getKey(), e.getValue());
        }
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
        debugInfo.append("\tStatus: " + response.getResponseCode()
                + ", Response Content-Type: " + response.getContentType());
        logger.info(debugInfo);
        out.flush();
    }

    private void delay(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private boolean save(MethodType methodType, RequestInfo requestInfo,
            ResponseInfo responseInfo) throws IOException {
        if (config.isSaveAPIResponsesOnly()
                && !responseInfo.isAPIContentType()) {
            return false;
        }
        File path = config.toFile(requestInfo.getRequestId(), methodType,
                false);
        config.getDefaultExportFormat().write(path, responseInfo);
        if (config.isSaveRawRequestResponses()) {
            ExportFormat.JSON.write(
                    config.getNextIOCounterFile(requestInfo.getRequestId(),
                            methodType),
                    new RequestResponse(requestInfo, responseInfo));
        }
        return true;
    }

    private ResponseInfo read(MethodType methodType, RequestInfo requestInfo)
            throws IOException {
        File path = config.toFile(requestInfo.getRequestId(), methodType, true);
        if (path == null) {
            return null;
        }
        ResponseInfo resp = config.getDefaultExportFormat().read(path,
                ResponseInfo.class, requestInfo, config);
        return resp;
    }

}
