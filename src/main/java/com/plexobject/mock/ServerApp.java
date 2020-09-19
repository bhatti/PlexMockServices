package com.plexobject.mock;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import com.plexobject.mock.server.MockHandler;

/**
 * This class starts embedded http server
 * 
 * @author shahzad bhatti
 *
 */
public class ServerApp {
    private static final Logger logger = Logger.getLogger(ServerApp.class);

    public static void main(String[] args) throws Exception {
        int port = Integer
                .parseInt(System.getProperty("jetty.http.port", "8000"));
        Server server = new Server(port);
        server.setHandler(new MockHandler());
        server.start();
        logger.info("Listening on " + port);
        server.join();
    }
}
