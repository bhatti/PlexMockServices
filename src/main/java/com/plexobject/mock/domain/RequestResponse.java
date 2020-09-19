package com.plexobject.mock.domain;

/**
 * This class abstracts request and response objects
 * 
 * @author shahzad bhatti
 *
 */
public class RequestResponse {
    private final MockRequest request;
    private final MockResponse response;

    public RequestResponse(MockRequest request, MockResponse response) {
        this.request = request;
        this.response = response;
    }

    public MockRequest getRequest() {
        return request;
    }

    public MockResponse getResponse() {
        return response;
    }

}
