package com.plexobject.mock.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import com.plexobject.mock.domain.Configuration;
import com.plexobject.mock.domain.ExportFormat;
import com.plexobject.mock.domain.MethodType;
import com.plexobject.mock.domain.MockRequest;
import com.plexobject.mock.domain.RequestResponse;
import com.plexobject.mock.domain.MockResponse;
import com.plexobject.mock.util.HTTPUtils;

// socat -v tcp-listen:8080,reuseaddr,fork tcp:192.168.180.40:80
@WebServlet(name = "mock_server", urlPatterns = { "/*" }, loadOnStartup = 1)
public class MockService extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MockService.class);
    private MockHandler mockHandler;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        try {
            mockHandler = new MockHandler();
        } catch (IOException e) {
            throw new ServletException(e);
        }
        logger.info("INITIALIZED " + servletConfig);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res, MethodType.HEAD);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res, MethodType.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res, MethodType.POST);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res, MethodType.PUT);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res, MethodType.DELETE);
    }

    //////////////////// PRIVATE METHODS //////////////////

    private void doService(HttpServletRequest req, HttpServletResponse res,
            MethodType methodType) throws ServletException, IOException {
        mockHandler.handle(null, null, req, res);
    }
}
