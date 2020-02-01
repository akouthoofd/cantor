package com.salesforce.cantor.http.functions;

import java.util.HashMap;
import java.util.Map;

public class Entity {
    private int status;
    private Map<String, String> headersMap = new HashMap<>();
    private byte[] body = new byte[0];

    public int getStatus() {
        return this.status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public Map<String, String> getHeadersMap() {
        return this.headersMap;
    }

    public void setHeader(final String name, final String value) {
        this.headersMap.put(name, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(final byte[] body) {
        this.body = body;
    }

    public void setBody(final String body) {
        this.body = body.getBytes();
    }
}
