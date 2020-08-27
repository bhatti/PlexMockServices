package com.plexobject.mock.server;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.thymeleaf.util.StringUtils;

import com.plexobject.mock.domain.Constants;
import com.plexobject.mock.domain.MethodType;

/**
 * This servlet handles all incoming requests and forwards to mock handler
 * 
 * @author shahzad bhatti
 *
 *         Note: manually you can use `socat -v tcp-listen:8080,reuseaddr,fork
 *         tcp:192.168.180.40:80`
 * 
 */
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
        doService(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doService(req, res);
    }

    public static MethodType getMethod(HttpServletRequest req) {
        String method = req.getParameter(Constants.MOCK_METHOD);
        if (StringUtils.isEmpty(method)) {
            method = req.getHeader(Constants.XMOCK_METHOD);
        }
        if (StringUtils.isEmpty(method)) {
            method = req.getMethod();
        }
        return MethodType.valueOf(method.toUpperCase());
    }

    //////////////////// PRIVATE METHODS //////////////////

    private void doService(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        MethodType method = getMethod(req);
        mockHandler.doHandle(req, res, method);
    }
}
