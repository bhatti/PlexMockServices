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
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.RecordedResponse;
import com.plexobject.mock.domain.RequestInfo;
import com.plexobject.mock.util.HTTPUtils;
import com.plexobject.mock.util.VelocityUtils;
import com.plexobject.mock.util.YAMLUtils;

public class MockService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MockService.class);
    private static final String RECORD = "record";
    private static final String MOCK_MODE = "mockMode";
    private static final String XMOCK_MODE = "XMockMode";

    private Configuration config;
    private HTTPUtils httpUtils;
    private VelocityUtils velocityUtils;
    private YAMLUtils yamlUtils = new YAMLUtils();

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        config = new Configuration(servletConfig);
        httpUtils = new HTTPUtils(config);
        velocityUtils = new VelocityUtils(config);

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

    private boolean isRecordMode(HttpServletRequest req) {
        String reqMode = req.getParameter(MOCK_MODE);
        if (reqMode == null) {
            reqMode = req.getHeader(XMOCK_MODE);
        }
        return reqMode != null ? RECORD.equalsIgnoreCase(reqMode) : config.isRecordMode();
    }

    private void doService(HttpServletRequest req, HttpServletResponse res, MethodType methodType)
            throws ServletException, IOException {
        RequestInfo requestInfo = new RequestInfo(config.getUrlPrefix(), req);
        RecordedResponse response = null;

        if (isRecordMode(req)) {
            logger.info(methodType + " RECORDING " + requestInfo);

            response = httpUtils.invokeRemoteAPI(methodType, requestInfo);
            save(response, methodType, requestInfo);
        } else {
            response = read(methodType, requestInfo);
            logger.info(methodType + " PLAYING " + requestInfo);
        }

        //
        if (response == null) {
            res.sendError(500, "Failed to find response for " + requestInfo.getUrl());
            return;
        }
        res.setContentType(response.getContentType());
        res.setStatus(response.getResponseCode());
        OutputStream out = res.getOutputStream();
        if (response.getContents() instanceof byte[]) {
            out.write(((byte[]) response.getContents()));
        } else if (response.getContents() instanceof String) {
            out.write(((String) response.getContents()).getBytes());
        } else if (response.getContents() != null) {
            out.write(response.getContents().toString().getBytes());
        }
        logger.info(methodType + " SENDING " + requestInfo.getRequestId() + ", status " + response.getResponseCode()
                + ", response " + response.getContentType());

        out.flush();
    }

    private void save(RecordedResponse response, MethodType methodType, RequestInfo requestInfo) throws IOException {
        response.unmarshalJsonContents();
        File path = config.toFile(requestInfo.getRequestId(), methodType, false);
        yamlUtils.write(path, response);
    }

    private RecordedResponse read(MethodType methodType, RequestInfo requestInfo) throws IOException {
        File path = config.toFile(requestInfo.getRequestId(), methodType, true);
        if (path == null) {
            return null;
        }
        try {
            RecordedResponse resp = null;
            if (path.getName().endsWith(Configuration.YAML)) {
                resp = (RecordedResponse) yamlUtils.read(path, RecordedResponse.class);
            } else if (path.getName().endsWith(Configuration.VELOCITY)) {
                String contents = velocityUtils.transform(path, requestInfo);
                resp = (RecordedResponse) yamlUtils.read(contents, RecordedResponse.class);
            }
            resp.marshalJsonContents();
            return resp;
        } catch (Exception e) {
            logger.warn("resp.read failed", e);
            throw new IOException(e);
        }
    }
}