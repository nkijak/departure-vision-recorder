package com.kinnack.dvr.kafka.models;

public class Envelope<T> {
    private static class Headers  {
        private String id;
        private String eventTimestamp;
        private String action;
        private String type;
    }

    private Headers headers;
    private T body;

    public Envelope(Headers headers, T body) {
        this.headers = headers;
        this.body = body;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

}
