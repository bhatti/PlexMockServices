package com.plexobject.mock.domain;

public class RequestResponse {
    private final RequestInfo request;
    private final ResponseInfo response;

    public RequestResponse(RequestInfo request, ResponseInfo response) {
        this.request = request;
        this.response = response;
    }

    public RequestInfo getRequest() {
        return request;
    }

    public ResponseInfo getResponse() {
        return response;
    }

}
