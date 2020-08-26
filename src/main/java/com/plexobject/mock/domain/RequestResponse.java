package com.plexobject.mock.domain;

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
