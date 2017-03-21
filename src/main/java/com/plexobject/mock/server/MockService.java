package com.plexobject.mock.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.ExportFormat;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.RequestInfo;
import com.plexobject.mock.domain.RequestResponse;
import com.plexobject.mock.domain.ResponseInfo;
import com.plexobject.mock.util.HTTPUtils;

public class MockService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MockService.class);

    private Configuration config;
    private HTTPUtils httpUtils;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        config = new Configuration(servletConfig);
        httpUtils = new HTTPUtils(config);
        logger.info("INITIALIZED " + config);
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

    private void doService(HttpServletRequest req, HttpServletResponse res, MethodType methodType)
            throws ServletException, IOException {
        RequestInfo requestInfo = new RequestInfo(req, config);
        ResponseInfo response = null;

        StringBuilder debugInfo = new StringBuilder();

        if (requestInfo.isRecordMode()) {
            response = httpUtils.invokeRemoteAPI(methodType, requestInfo);
            if (save(methodType, requestInfo, response)) {
                debugInfo.append(methodType + " RECORDING " + requestInfo);
            } else {
                debugInfo.append(methodType + " REDIRECTING " + requestInfo);
            }
        } else {
            response = read(methodType, requestInfo);
            debugInfo.append(methodType + " PLAYING " + requestInfo);
        }

        //
        if (response == null) {
            res.sendError(500, "Failed to find response for " + requestInfo.getUrl());
            return;
        }

        if (requestInfo.getMockWaitTimeMillis() > 0) {
            debugInfo.append("\tAdding delay from request " + requestInfo.getMockWaitTimeMillis() + "ms\n");
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
        debugInfo.append("\tStatus " + response.getResponseCode());
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

    private boolean save(MethodType methodType, RequestInfo requestInfo, ResponseInfo responseInfo) throws IOException {
        if (config.isSaveAPIResponsesOnly() && !requestInfo.isAPIContentType()) {
            return false;
        }
        File path = config.toFile(requestInfo.getRequestId(), methodType, false);
        config.getDefaultExportFormat().write(path, responseInfo);
        if (config.isSaveRawRequestResponses()) {
            ExportFormat.JSON.write(config.getNextIOCounterFile(requestInfo.getRequestId(), methodType),
                    new RequestResponse(requestInfo, responseInfo));
        }
        return true;
    }

    private ResponseInfo read(MethodType methodType, RequestInfo requestInfo) throws IOException {
        File path = config.toFile(requestInfo.getRequestId(), methodType, true);
        if (path == null) {
            return null;
        }
        ResponseInfo resp = config.getDefaultExportFormat().read(path, ResponseInfo.class, requestInfo, config);
        return resp;
    }
}
